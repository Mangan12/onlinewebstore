package com.api_gateway.service;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import io.jsonwebtoken.JwtException;
import reactor.core.publisher.Mono;

@Component
public class JwtService {

	private final WebClient.Builder webClientBuilder;

	public JwtService(WebClient.Builder webClientBuilder) {
		this.webClientBuilder = webClientBuilder;
	}

	public Mono<Map<String, Object>> validateToken(String token) {
		Logger logger = LoggerFactory.getLogger(JwtService.class);
		try {
			logger.info("Starting token validation...");
			return webClientBuilder.build().get().uri("http://user-service/api/users/validate-token")
					.header(HttpHeaders.AUTHORIZATION, "Bearer " + token).retrieve()
					.bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {
					}).onErrorResume(WebClientResponseException.class, ex -> {
						logger.error("WebClientResponseException caught. Status code: {}", ex.getStatusCode());
						if (ex.getStatusCode() == HttpStatus.UNAUTHORIZED) {
							logger.error("Invalid token error due to UNAUTHORIZED status.");
							return Mono.error(new JwtException("Invalid token"));
						}
						logger.error("Error: {}", ex.getMessage());
						return Mono.error(ex);
					});
		} catch (Exception e) {
			logger.error("Unexpected error during token validation: {}", e.getMessage());
			return Mono.error(e);
		}
	}

}
