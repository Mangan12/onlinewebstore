package com.order.dto;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class OrderResponse {
	private Long id;
	private String orderNumber;
	private List<OrderLineItemsResponse> orderLineItemsResponse;
}
