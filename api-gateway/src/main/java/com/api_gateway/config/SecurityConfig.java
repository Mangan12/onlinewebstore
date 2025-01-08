package com.api_gateway.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.SecurityWebFiltersOrder;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.web.reactive.function.client.WebClient;

import com.api_gateway.filter.JwtWebFilter;

@Configuration
@EnableWebFluxSecurity
public class SecurityConfig {

	@Autowired
	private JwtWebFilter jwtWebFilter;

	@Bean
	@LoadBalanced
	public WebClient.Builder webClientBuilder() {
		return WebClient.builder();
	}

	@Bean
	public SecurityWebFilterChain springSecurityFilterChain(ServerHttpSecurity http) {
		return http.csrf(csrf -> csrf.disable())
				.authorizeExchange(auth -> auth.pathMatchers("api/users/login", "api/users/register").permitAll()
						.anyExchange().authenticated())
				.addFilterAt(jwtWebFilter, SecurityWebFiltersOrder.AUTHENTICATION).httpBasic(Customizer.withDefaults())
				.formLogin(Customizer.withDefaults()).build();
	}
}
