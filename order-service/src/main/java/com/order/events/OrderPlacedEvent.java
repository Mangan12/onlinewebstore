package com.order.events;

import java.time.LocalDateTime;
import java.util.List;

import com.order.entity.OrderLineItems;

import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
public class OrderPlacedEvent {

	// Order details
	private long orderId;
	private LocalDateTime orderDate;
	private String orderNumber;
	// Product details
	private List<OrderLineItems> items;

	// Metadata
	private String eventId;

	private LocalDateTime eventTimestamp;

	public long getOrderId() {
		return orderId;
	}

	public void setOrderId(long orderId) {
		this.orderId = orderId;
	}

	public LocalDateTime getOrderDate() {
		return orderDate;
	}

	public void setOrderDate(LocalDateTime orderDate) {
		this.orderDate = orderDate;
	}

	public List<OrderLineItems> getItems() {
		return items;
	}

	public void setItems(List<OrderLineItems> items) {
		this.items = items;
	}

	public String getEventId() {
		return eventId;
	}

	public void setEventId(String eventId) {
		this.eventId = eventId;
	}

	public LocalDateTime getEventTimestamp() {
		return eventTimestamp;
	}

	public void setEventTimestamp(LocalDateTime eventTimestamp) {
		this.eventTimestamp = eventTimestamp;
	}

	public String getOrderNumber() {
		return orderNumber;
	}

	public void setOrderNumber(String orderNumber) {
		this.orderNumber = orderNumber;
	}
	

}
