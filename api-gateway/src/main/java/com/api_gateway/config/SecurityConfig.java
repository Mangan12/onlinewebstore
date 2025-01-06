package com.api_gateway.config;

import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.web.reactive.function.client.WebClient;


@Configuration
@EnableWebFluxSecurity
public class SecurityConfig {

//	@Autowired
//	private ReactiveUserDetailsService reactiveUserDetailsService;

	@Bean
	@LoadBalanced
	public WebClient.Builder webClientBuilder() {
	    return WebClient.builder();
	}

	@Bean
	public SecurityWebFilterChain springSecurityFilterChain(ServerHttpSecurity http) {
	    return http
	        .csrf(csrf -> csrf.disable())
	        .authorizeExchange(auth -> auth
	            .pathMatchers("api/users/login","api/users/register").permitAll()
	            .anyExchange().authenticated()
	        )
	        .httpBasic(Customizer.withDefaults())
	        .formLogin(Customizer.withDefaults())
	        .build();
	}

//	@Bean
//	public AuthenticationProvider ap() {
//		DaoAuthenticationProvider dap = new DaoAuthenticationProvider();
//		dap.setPasswordEncoder(NoOpPasswordEncoder.getInstance());
//		dap.setUserDetailsService(userDetailsService);
//		return dap;
//	}
	
//	@Bean
//    public ReactiveAuthenticationManager authenticationManager() {
//        DaoAuthenticationProvider authenticationProvider = new DaoAuthenticationProvider();
//        authenticationProvider.setPasswordEncoder(NoOpPasswordEncoder.getInstance()); // Use a proper encoder like BCrypt
//        authenticationProvider.setUserDetailsService(userDetailsService);
//        return new DefaultReactiveAuthenticationManager(authenticationProvider);
//    }
}
