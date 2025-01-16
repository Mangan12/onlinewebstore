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

import com.order.dto.InventoryResponse;
import com.order.dto.OrderLineItemsResponse;
import com.order.dto.OrderRequest;
import com.order.dto.OrderResponse;
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
		// Create order with UUID and map line items
		Order order = new Order();
		order.setOrderNumber(UUID.randomUUID().toString());
		List<OrderLineItems> orderLineItems = orderRequest.getOrderLineItemsDto().stream()
				.map(orderLine -> modelMapper.map(orderLine, OrderLineItems.class)).collect(Collectors.toList());
		order.setOrderLineItems(orderLineItems);

		// Get unique SKU codes and check inventory
		List<String> skuCodes = orderLineItems.stream().map(OrderLineItems::getSkuCode).distinct()
				.collect(Collectors.toList());

		List<InventoryResponse> inventoryResponses = checkInventory(skuCodes);
		logInventoryResponses(inventoryResponses);
		validateInventoryData(inventoryResponses);

		// Process inventory validation
		InventoryValidationResult validationResult = validateOrderAgainstInventory(orderLineItems, inventoryResponses);

		// Process order based on validation result
		return processOrderBasedOnValidation(order, validationResult);
	}

	private List<InventoryResponse> checkInventory(List<String> skuCodes) {
		return webClientBuilder.build().get()
				.uri("http://inventory-service/api/inventory/checkstock",
						uriBuilder -> uriBuilder.queryParam("skuCode", skuCodes).build())
				.retrieve().bodyToMono(new ParameterizedTypeReference<List<InventoryResponse>>() {
				}).block();
	}

	private void logInventoryResponses(List<InventoryResponse> responses) {
		responses.forEach(response -> log.info("{} {}", response.getSkuCode(), response.getQuantity()));
	}

	private void validateInventoryData(List<InventoryResponse> inventoryResponses) {
		if (inventoryResponses == null || inventoryResponses.isEmpty()) {
			throw new IllegalStateException("No inventory data available.");
		}
	}

	private static class InventoryValidationResult {
		final List<String> outOfStockSkus = new ArrayList<>();
		final List<OrderLineItems> inStockSkus = new ArrayList<>();
	}

	private InventoryValidationResult validateOrderAgainstInventory(List<OrderLineItems> orderLineItems,
			List<InventoryResponse> inventoryResponses) {

		InventoryValidationResult result = new InventoryValidationResult();
		Map<String, Integer> inventoryMap = inventoryResponses.stream()
				.collect(Collectors.toMap(InventoryResponse::getSkuCode, InventoryResponse::getQuantity));

		for (OrderLineItems item : orderLineItems) {
			String skuCode = item.getSkuCode();
			int requestedQuantity = item.getQuantity();

			Integer availableQuantity = inventoryMap.get(skuCode);
			if (availableQuantity == null) {
				throw new IllegalArgumentException("SKU Code not found in inventory: " + skuCode);
			}

			if (availableQuantity == 0) {
				result.outOfStockSkus.add(skuCode);
			} else if (requestedQuantity > availableQuantity) {
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

		Order placedOrder = null;
		try {
			if (!validationResult.outOfStockSkus.isEmpty()) {
				if (validationResult.outOfStockSkus.size() == order.getOrderLineItems().size()) {
					return CompletableFuture.completedFuture(
							"All items out of stock for SKU Codes: " + validationResult.outOfStockSkus);
				}

				order.setOrderLineItems(validationResult.inStockSkus);
				placedOrder = orderRepo.save(order);
				return CompletableFuture.completedFuture(
						"Partial order placed. Out of stock items: " + validationResult.outOfStockSkus);
			}

			if (validationResult.inStockSkus.size() == order.getOrderLineItems().size()) {
				order.setOrderLineItems(validationResult.inStockSkus);
				placedOrder = orderRepo.save(order);
				return CompletableFuture.completedFuture("Hurray Order placed!");
			}

			return CompletableFuture.completedFuture("OOps something went wrong...!");
		} finally {
			sendNotification(placedOrder);
		}
	}

	private void sendNotification(Order order) {
		OrderPlacedEvent orderPlacedEvent = new OrderPlacedEvent();
		orderPlacedEvent.setEventId(UUID.randomUUID().toString());
		orderPlacedEvent.setEventTimestamp(order.getCreatedAt());
		orderPlacedEvent.setItems(order.getOrderLineItems());
		orderPlacedEvent.setOrderDate(order.getCreatedAt());
		orderPlacedEvent.setOrderId(order.getId());
		orderPlacedEvent.setOrderNumber(order.getOrderNumber());
		CompletableFuture<SendResult<String, OrderPlacedEvent>> message = template.send("order-topic", orderPlacedEvent);
		message.whenComplete((result, ex) -> {
			if (ex == null) {
				System.out.println("Sent Message = [" + orderPlacedEvent + "] with offset = ["
						+ result.getRecordMetadata().offset() + "]");
			} else {
				System.out.println("unable to send message = [" + orderPlacedEvent + "] due to : " + ex.getMessage());
			}
		});
	}

	public String fallbackMethod(OrderRequest orderRequest, Exception t) {
		return "OOPS Something went wrong....Please try later!!";
	}

	public CompletableFuture<String> fallbackMethodForTimeOut(OrderRequest orderRequest, Exception t) {
		return CompletableFuture.supplyAsync(() -> "Fallback response due to timeout");
	}

	public String fallBackMethodRetry(OrderRequest orderRequest, Exception t) {
		return "OOPS Something went wrong....Please try later!!";
	}

	@Override
	public List<OrderResponse> getAllOrders() {
		// TODO Auto-generated method stub
		List<Order> orders = orderRepo.findAll();
		return orders.stream().map(this::convertToDTO).toList();
	}

	private OrderResponse convertToDTO(Order order) {
		OrderResponse orderResponse = modelMapper.map(order, OrderResponse.class);
		List<OrderLineItemsResponse> orderLineitemsDto = order.getOrderLineItems().stream()
				.map(orderLineItem -> modelMapper.map(orderLineItem, OrderLineItemsResponse.class)).toList();
		orderResponse.setOrderLineItemsResponse(orderLineitemsDto);
		return orderResponse;
	}
}
