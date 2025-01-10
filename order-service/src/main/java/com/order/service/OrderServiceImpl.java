package com.order.service;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;

import com.order.dto.InventoryResponse;
import com.order.dto.OrderLineItemsResponse;
import com.order.dto.OrderRequest;
import com.order.dto.OrderResponse;
import com.order.entity.Order;
import com.order.entity.OrderLineItems;
import com.order.repository.OrderRepo;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import io.github.resilience4j.timelimiter.annotation.TimeLimiter;

@Service
@Transactional
public class OrderServiceImpl implements OrderService {

	@Autowired
	private ModelMapper modelMapper;

	@Autowired
	private OrderRepo orderRepo;

	@Autowired
	private WebClient.Builder webClientBuilder;

	@Override
	@CircuitBreaker(name = "inventory", fallbackMethod = "fallBackMethod")
//	@TimeLimiter(name="inventory", fallbackMethod = "fallbackMethodForTimeOut")
//	@Retry(name="inventory", fallbackMethod = "fallBackMethodRetry")
	public CompletableFuture<String> placeOrder(OrderRequest orderRequest) {
		Order order = new Order();
		order.setOrderNumber(UUID.randomUUID().toString());
		List<OrderLineItems> orderLineItems = orderRequest.getOrderLineItemsDto().stream()
				.map(orderLine -> modelMapper.map(orderLine, OrderLineItems.class)).toList();
		order.setOrderLineItems(orderLineItems);

		List<String> skuCode = order.getOrderLineItems().stream().map(OrderLineItems::getSkuCode).distinct().toList();
		InventoryResponse[] inventoryResponses = webClientBuilder.build().get()
				.uri("http://inventory-service/api/inventory/checkstock",
						uriBuilder -> uriBuilder.queryParam("skuCode", skuCode).build())
				.retrieve().bodyToMono(InventoryResponse[].class).block();

		boolean res = Arrays.stream(inventoryResponses)
				.allMatch(inventoryResponse -> (inventoryResponse.getQuantity() > 0));

		if (res) {
			orderRepo.save(order);
			return CompletableFuture.supplyAsync(() -> "Order Placed Successfully");
		}
		else
			throw new IllegalArgumentException("Product is not in stock!!");
	}

	public String fallbackMethod(OrderRequest orderRequest, Exception  t) {
		return "OOPS Something went wrong....Please try later!!";
	}
	public CompletableFuture<String> fallbackMethodForTimeOut(OrderRequest orderRequest, Exception  t) {
		 return CompletableFuture.supplyAsync(() -> "Fallback response due to timeout");
	}
	public String fallBackMethodRetry(OrderRequest orderRequest, Exception  t) {
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
