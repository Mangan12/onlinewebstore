package com.api_gateway.filter;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;

import com.api_gateway.service.JwtService;

import io.jsonwebtoken.JwtException;
import reactor.core.publisher.Mono;

@Component
public class JwtWebFilter implements WebFilter {
	Logger logger = LoggerFactory.getLogger(JwtWebFilter.class);

	private final JwtService jwtService;

	public JwtWebFilter(JwtService jwtService) {
		this.jwtService = jwtService;
	}

	@Override
	public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
		// Extract the token from the request
		String token = extractTokenFromRequest(exchange.getRequest());
		System.out.println("Extracted Token: " + token);

		if (token != null) {
			System.out.println("Validating token...");
			return jwtService.validateToken(token).flatMap(validateResponse -> {
				if (Boolean.TRUE.equals(validateResponse.get("valid"))) {
					List<SimpleGrantedAuthority> authorities;
					try {
						authorities = extractAuthorities(validateResponse);
					} catch (ClassCastException e) {
						// Handle case where roles are not in expected format
						authorities = Collections.emptyList();
					}
					UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
							validateResponse.get("userId"), null, authorities);

					return chain.filter(exchange).contextWrite(ReactiveSecurityContextHolder.withAuthentication(auth));
				} else {
					exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
					return exchange.getResponse().setComplete();
				}
			}).onErrorResume(JwtException.class, error -> {
				// Log the error details
				System.err.println("JWT Validation Error: " + error.getMessage());
				return Mono.error(new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid JWT token"));
			});
		}
		// Log when no token is present in the request
		System.out.println("No token found. Proceeding without authentication.");
		return chain.filter(exchange);
	}

	private List<SimpleGrantedAuthority> extractAuthorities(Map<String, Object> validateResponse) {
		try {
			@SuppressWarnings("unchecked")
			List<String> roles = (List<String>) validateResponse.getOrDefault("roles", Collections.emptyList());
			if (roles == null) {
				return Collections.emptyList();
			}

			return roles.stream().filter(Objects::nonNull) // Filter out any null roles
					.map(role -> new SimpleGrantedAuthority("ROLE_" + role)).collect(Collectors.toList());

		} catch (ClassCastException e) {
			logger.error("Error extracting authorities: {}", e.getMessage());
			return Collections.emptyList();
		}
	}

	private String extractTokenFromRequest(ServerHttpRequest request) {
		String bearerToken = request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
		if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
			return bearerToken.substring(7);
		}
		return null;
	}

}
