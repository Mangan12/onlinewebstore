package com.inventory.config;

import org.modelmapper.ModelMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class InventoryConfig {

	@Bean
	ModelMapper modelMapper() {
		return new ModelMapper();
	}
	@Bean
	WebClient webClient() {
		return WebClient.builder().build();
	}
	
}
