package com.mangan.notification.consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Service;

import com.mangan.notification.dto.OrderPlacedEvent;

@Service
public class OrderConsumer {

    private static final Logger logger = LoggerFactory.getLogger(OrderConsumer.class);

    @KafkaListener(topics = "order-topic", groupId = "order_group", containerFactory = "containerFactory")
    public void handleNotification(@Payload OrderPlacedEvent orderPlacedEvent) {
        try {
            // Log the received event
            logger.info("Received OrderPlacedEvent: {}", orderPlacedEvent);

            // Validate the event data
            if (orderPlacedEvent == null || orderPlacedEvent.getOrderNumber() == null) {
                logger.error("Invalid OrderPlacedEvent received: {}", orderPlacedEvent);
                return; // Optionally decide whether to acknowledge invalid events
            }

            // Simulate sending notification to the user
            String message = String.format("Order with ID: %s has been placed successfully!", orderPlacedEvent.getOrderId());
            //sendNotification(orderPlacedEvent.get(), message);
            System.out.println(message);

            logger.info("OrderPlacedEvent processed and acknowledged successfully.");

        } catch (Exception e) {
            // Log the error and handle retries or dead letter queue
            logger.error("Error processing OrderPlacedEvent: {}", orderPlacedEvent, e);
        }
    }
}