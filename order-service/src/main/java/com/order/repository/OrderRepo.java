package com.order.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.order.entity.Order;

public interface OrderRepo extends JpaRepository<Order, Long> {

}
