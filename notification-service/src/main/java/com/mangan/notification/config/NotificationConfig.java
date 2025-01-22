package com.mangan.notification.config;

import java.util.HashMap;
import java.util.Map;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.config.KafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.listener.ConcurrentMessageListenerContainer;
import org.springframework.kafka.listener.ContainerProperties;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import org.springframework.util.backoff.ExponentialBackOff;

import com.mangan.notification.dto.OrderPlacedEvent;

@Configuration
public class NotificationConfig {

	@Value("${spring.kafka.bootstrap-servers}")
	private String bootstrapServers;

	private static final Logger logger = LoggerFactory.getLogger(NotificationConfig.class);

	@Bean
	ConsumerFactory<String, OrderPlacedEvent> consumerFactory() {
		Map<String, Object> props = new HashMap<>();
		props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
		props.put(ConsumerConfig.GROUP_ID_CONFIG, "order_group");
		props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
		props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, JsonDeserializer.class);
		props.put(JsonDeserializer.TRUSTED_PACKAGES, "com.mangan.notification.dto");
		props.put(JsonDeserializer.TYPE_MAPPINGS, "event:com.mangan.notification.dto.OrderPlacedEvent");

		return new DefaultKafkaConsumerFactory<>(props, new StringDeserializer(),
				new JsonDeserializer<>(OrderPlacedEvent.class));
	}

	@Bean
	KafkaListenerContainerFactory<ConcurrentMessageListenerContainer<String, OrderPlacedEvent>> containerFactory() {
		ConcurrentKafkaListenerContainerFactory<String, OrderPlacedEvent> factory = new ConcurrentKafkaListenerContainerFactory<>();
		factory.setConsumerFactory(consumerFactory());
		// Enable manual acknowledgment
		factory.getContainerProperties().setAckMode(ContainerProperties.AckMode.MANUAL);

		// Set error handler to move failed messages to a dead-letter topic
		factory.setCommonErrorHandler(commonErrorHandler());
		return factory;
	}

	@Bean
	DefaultErrorHandler commonErrorHandler() {
		// Exponential backoff for retries
		ExponentialBackOff backOff = new ExponentialBackOff();
		backOff.setInitialInterval(1000L); // 1 second
		backOff.setMaxInterval(10_000L); // 10 seconds
		backOff.setMultiplier(2.0); // Exponential factor

		DefaultErrorHandler errorHandler = new DefaultErrorHandler((consumerRecord, exception) -> {
			logger.error("Error processing message: {}. Sending to DLQ.", consumerRecord, exception);
			// You can send the failed message to a dead-letter topic here
			// Example: kafkaTemplate.send("dead-letter-topic", consumerRecord.key(),
			// consumerRecord.value());
		}, backOff);

		// Optionally add record-level filtering for specific exceptions
		errorHandler.addNotRetryableExceptions(IllegalArgumentException.class);

		return errorHandler;
	}

}
