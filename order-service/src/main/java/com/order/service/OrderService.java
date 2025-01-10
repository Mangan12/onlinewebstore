package com.order.service;

import java.util.List;
import java.util.concurrent.CompletableFuture;

import com.order.dto.OrderRequest;
import com.order.dto.OrderResponse;

public interface OrderService {
	public CompletableFuture<String> placeOrder(OrderRequest orderRequest);

	public List<OrderResponse> getAllOrders();
}
