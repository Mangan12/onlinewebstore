package com.order.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;

import com.order.dto.*;
import com.order.entity.Order;
import com.order.entity.OrderLineItems;
import com.order.events.OrderPlacedEvent;
import com.order.repository.OrderRepo;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import io.github.resilience4j.timelimiter.annotation.TimeLimiter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@Transactional
public class OrderServiceImpl implements OrderService {

	@Autowired
	private ModelMapper modelMapper;

	@Autowired
	private OrderRepo orderRepo;

	@Autowired
	private WebClient.Builder webClientBuilder;

	@Autowired
	private KafkaTemplate<String, OrderPlacedEvent> template;

	@Override
	@CircuitBreaker(name = "inventory", fallbackMethod = "fallBackMethod")
	@TimeLimiter(name = "inventory", fallbackMethod = "fallbackMethodForTimeOut")
	@Retry(name = "inventory", fallbackMethod = "fallBackMethodRetry")
	public CompletableFuture<String> placeOrder(OrderRequest orderRequest) {
		log.info("Received order request: {}", orderRequest);
		try {
			// Create order with UUID and map line items
			Order order = createOrderFromRequest(orderRequest);
			log.debug("Created order with number: {}", order.getOrderNumber());

			// Get unique SKU codes and check inventory
			List<String> skuCodes = extractSkuCodes(order.getOrderLineItems());
			List<InventoryResponse> inventoryResponses = checkInventory(skuCodes);

			// Validate inventory
			InventoryValidationResult validationResult = validateOrderAgainstInventory(order.getOrderLineItems(),
					inventoryResponses);

			// Process order based on validation result
			return processOrderBasedOnValidation(order, validationResult);
		} catch (Exception e) {
			log.error("Error processing order request", e);
			throw e;
		}
	}

	private Order createOrderFromRequest(OrderRequest orderRequest) {
		try {
			Order order = new Order();
			order.setOrderNumber(UUID.randomUUID().toString());
			List<OrderLineItems> orderLineItems = orderRequest.getOrderLineItemsDto().stream()
					.map(orderLine -> modelMapper.map(orderLine, OrderLineItems.class)).collect(Collectors.toList());
			order.setOrderLineItems(orderLineItems);
			return order;
		} catch (Exception e) {
			log.error("Error creating order from request", e);
			throw new IllegalStateException("Failed to create order from request", e);
		}
	}

	private List<String> extractSkuCodes(List<OrderLineItems> orderLineItems) {
		return orderLineItems.stream().map(OrderLineItems::getSkuCode).distinct().collect(Collectors.toList());
	}

	private List<InventoryResponse> checkInventory(List<String> skuCodes) {
		log.debug("Checking inventory for SKU codes: {}", skuCodes);
		try {
			List<InventoryResponse> responses = webClientBuilder.build().get()
					.uri("http://inventory-service/api/inventory/checkstock",
							uriBuilder -> uriBuilder.queryParam("skuCode", skuCodes).build())
					.retrieve().bodyToMono(new ParameterizedTypeReference<List<InventoryResponse>>() {
					}).block();

			if (responses == null || responses.isEmpty()) {
				throw new IllegalStateException("No inventory data available");
			}

			log.debug("Received inventory responses: {}", responses);
			return responses;
		} catch (Exception e) {
			log.error("Error checking inventory", e);
			throw new IllegalStateException("Failed to check inventory", e);
		}
	}

	private static class InventoryValidationResult {
		final List<String> outOfStockSkus = new ArrayList<>();
		final List<OrderLineItems> inStockSkus = new ArrayList<>();
	}

	private InventoryValidationResult validateOrderAgainstInventory(List<OrderLineItems> orderLineItems,
			List<InventoryResponse> inventoryResponses) {
		log.debug("Validating order against inventory");

		InventoryValidationResult result = new InventoryValidationResult();
		Map<String, Integer> inventoryMap = inventoryResponses.stream()
				.collect(Collectors.toMap(InventoryResponse::getSkuCode, InventoryResponse::getQuantity));

		for (OrderLineItems item : orderLineItems) {
			String skuCode = item.getSkuCode();
			int requestedQuantity = item.getQuantity();

			Integer availableQuantity = inventoryMap.get(skuCode);
			if (availableQuantity == null) {
				log.error("SKU Code not found in inventory: {}", skuCode);
				throw new IllegalArgumentException("SKU Code not found in inventory: " + skuCode);
			}

			if (availableQuantity == 0) {
				log.info("SKU Code {} is out of stock", skuCode);
				result.outOfStockSkus.add(skuCode);
			} else if (requestedQuantity > availableQuantity) {
				log.error("Insufficient inventory for SKU Code: {}", skuCode);
				throw new IllegalStateException(String.format(
						"Inventory quantity for SKU Code %s is %d. Please request quantity less than or equal to inventory.",
						skuCode, availableQuantity));
			} else {
				result.inStockSkus.add(item);
			}
		}
		return result;
	}

	private CompletableFuture<String> processOrderBasedOnValidation(Order order,
			InventoryValidationResult validationResult) {
		log.info("Processing order based on validation results");

		// Use synchronized block to prevent concurrent modifications
		synchronized (this) {
			try {
				if (!validationResult.outOfStockSkus.isEmpty()) {
					if (validationResult.outOfStockSkus.size() == order.getOrderLineItems().size()) {
						log.info("All items out of stock for order {}", order.getOrderNumber());
						return CompletableFuture.completedFuture(
								"All items out of stock for SKU Codes: " + validationResult.outOfStockSkus);
					}

					order.setOrderLineItems(validationResult.inStockSkus);
					Order savedOrder = orderRepo.save(order);
					sendNotification(savedOrder);
					log.info("Partial order placed for order number: {}", savedOrder.getOrderNumber());
					return CompletableFuture.completedFuture(
							"Partial order placed. Out of stock items: " + validationResult.outOfStockSkus);
				}

				if (validationResult.inStockSkus.size() == order.getOrderLineItems().size()) {
					order.setOrderLineItems(validationResult.inStockSkus);
					Order savedOrder = orderRepo.save(order);
					sendNotification(savedOrder);
					log.info("Order successfully placed with order number: {}", savedOrder.getOrderNumber());
					return CompletableFuture.completedFuture("Hurray Order placed!");
				}

				log.error("Unexpected validation result for order {}", order.getOrderNumber());
				return CompletableFuture.completedFuture("OOps something went wrong...!");
			} catch (Exception e) {
				log.error("Error processing order {}", order.getOrderNumber(), e);
				throw e;
			}
		}
	}

	private void sendNotification(Order order) {
		try {
			OrderPlacedEvent orderPlacedEvent = createOrderPlacedEvent(order);
			CompletableFuture<SendResult<String, OrderPlacedEvent>> message = template.send("order-topic",
					orderPlacedEvent);

			message.whenComplete((result, ex) -> {
				if (ex == null) {
					log.info("Sent notification for order: {}. Offset: {}", order.getOrderNumber(),
							result.getRecordMetadata().offset());
				} else {
					log.error("Failed to send notification for order: {}", order.getOrderNumber(), ex);
				}
			});
		} catch (Exception e) {
			log.error("Error sending notification for order: {}");
			throw new IllegalStateException("Failed to send order notification", e);
		}
	}

	private OrderPlacedEvent createOrderPlacedEvent(Order order) {
		OrderPlacedEvent event = new OrderPlacedEvent();
		event.setEventId(UUID.randomUUID().toString());
		event.setEventTimestamp(order.getCreatedAt());
		event.setItems(order.getOrderLineItems());
		event.setOrderDate(order.getCreatedAt());
		event.setOrderId(order.getId());
		event.setOrderNumber(order.getOrderNumber());
		return event;
	}

	// Fallback methods remain unchanged
	public String fallBackMethod(OrderRequest orderRequest, Exception t) {
		log.error("Circuit breaker fallback triggered", t);
		return "OOPS Something went wrong....Please try later!!";
	}

	public CompletableFuture<String> fallbackMethodForTimeOut(OrderRequest orderRequest, Exception t) {
		log.error("Timeout fallback triggered", t);
		return CompletableFuture.supplyAsync(() -> "Fallback response due to timeout");
	}

	public String fallBackMethodRetry(OrderRequest orderRequest, Exception t) {
		log.error("Retry fallback triggered", t);
		return "OOPS Something went wrong....Please try later!!";
	}

	@Override
	public List<OrderResponse> getAllOrders() {
		log.debug("Retrieving all orders");
		try {
			List<Order> orders = orderRepo.findAll();
			return orders.stream().map(this::convertToDTO).collect(Collectors.toList());
		} catch (Exception e) {
			log.error("Error retrieving orders", e);
			throw new IllegalStateException("Failed to retrieve orders", e);
		}
	}

	private OrderResponse convertToDTO(Order order) {
		try {
			OrderResponse orderResponse = modelMapper.map(order, OrderResponse.class);
			List<OrderLineItemsResponse> orderLineitemsDto = order.getOrderLineItems().stream()
					.map(orderLineItem -> modelMapper.map(orderLineItem, OrderLineItemsResponse.class))
					.collect(Collectors.toList());
			orderResponse.setOrderLineItemsResponse(orderLineitemsDto);
			return orderResponse;
		} catch (Exception e) {
			log.error("Error converting order to DTO: {}", order.getOrderNumber(), e);
			throw new IllegalStateException("Failed to convert order to DTO", e);
		}
	}
}