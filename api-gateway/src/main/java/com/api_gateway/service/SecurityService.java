package com.api_gateway.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.ReactiveUserDetailsService;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import com.api_gateway.dto.UserResponseDTO;

import reactor.core.publisher.Mono;

@Service
public class SecurityService implements ReactiveUserDetailsService {
	
	@Autowired
	private WebClient.Builder webClientBuilder;

	@Override
	public Mono<UserDetails> findByUsername(String username) {
		long userId;
		try {
	        userId = Long.parseLong(username);
	        System.out.println("Parsed userId: " + userId);
	    } catch (NumberFormatException e) {
	        System.err.println("Failed to parse userId: " + e.getMessage());
	        return Mono.error(new UsernameNotFoundException("Invalid userId format: " + username, e));
	    }
		Mono<UserDetails> m = webClientBuilder.build()
		        .get()
		        .uri("http://user-service/api/users/id", uriBuilder -> {
		            try {
		                return uriBuilder.queryParam("userId", userId).build();
		            } catch (Exception e) {
		                System.err.println("Error building URI: " + e.getMessage());
		                throw e;
		            }
		        })
		        .retrieve()
		        .onStatus(
		            status -> !status.is2xxSuccessful(),
		            response -> {
		                System.err.println("Error response from server: " + response.statusCode());
		                return Mono.error(new RuntimeException("Unexpected response status: " + response.statusCode()));
		            }
		        )
		        .bodyToMono(UserResponseDTO.class)
		        .doOnError(e -> {
		            System.err.println("Error during WebClient call: " + e.getMessage());
		            e.printStackTrace();
		        })
		        .onErrorResume(WebClientResponseException.NotFound.class, error -> {
		            System.err.println("User not found: " + error.getMessage());
		            return Mono.error(new UsernameNotFoundException("User not found with id: " + userId, error));
		        })
		        .map(userDto -> {
		            try {
		                System.out.println("Fetched user: " + userDto);
		                return (UserDetails) User.builder()
		                    .username(String.valueOf(userDto.getUserId()))
		                    .password("{bcrypt}" + userDto.getPassword())
		                    .build();
		            } catch (Exception e) {
		                System.err.println("Error mapping UserResponseDTO to UserDetails: " + e.getMessage());
		                throw new RuntimeException("Mapping error", e);
		            }
		        })
		        .switchIfEmpty(Mono.error(new UsernameNotFoundException("User not found")));
	    return m;
	}

}
