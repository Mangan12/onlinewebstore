package com.user.controller;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.user.dto.JwtResponseDTO;
import com.user.dto.RefreshTokenRequestDTO;
import com.user.dto.UserDTO;
import com.user.dto.UserLoginDTO;
import com.user.dto.UserRegisterDTO;
import com.user.dto.UserResponseDTO;
import com.user.exception.UserNotFoundException;
import com.user.service.UserService;
import com.user.token.service.JWTService;
import com.user.token.service.RefreshTokenService;

import io.jsonwebtoken.Claims;
import jakarta.validation.Valid;

@RestController
@RequestMapping("api/users")
public class UserController {

	private final UserService userService;
	private final JWTService jwtService;
	
	@Autowired
	private RefreshTokenService refreshTokenService;

	public UserController(UserService userService, JWTService jwtService) {
		this.userService = userService;
		this.jwtService = jwtService;
	}

	@PostMapping("/login")
	public ResponseEntity<?> userLogin(@RequestBody @Valid UserLoginDTO userLoginDTO) {
		try {
			// Attempt to authenticate the user
			JwtResponseDTO keys = userService.authenticateUser(userLoginDTO);

			// If successful, return the key
			return ResponseEntity.ok().body(Map.of("message", "Login successful!", "keys", keys));
		} catch (AuthenticationException e) {
			// Handle authentication failures
			return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
					.body(Map.of("message", "Invalid email or password!", "error", e.getMessage()));
		} catch (Exception e) {
			// Handle any unexpected errors
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
					.body(Map.of("message", "An unexpected error occurred during login.", "error", e.getMessage()));
		}
	}

	@PostMapping("/register")
	@ResponseStatus(HttpStatus.OK)
	public ResponseEntity<String> userRegister(@RequestBody @Valid UserRegisterDTO userRegisterDTO) {
		userService.registerUser(userRegisterDTO);
		return ResponseEntity.status(HttpStatus.CREATED).body("Registration Successfull!");
	}

	// Fetch a user by ID
	@GetMapping("/by-mail")
	public ResponseEntity<Optional<UserResponseDTO>> getUser(@RequestParam String email) {
		try {
			Optional<UserResponseDTO> userResponseDTO = userService.getUserByEmail(email);
			return ResponseEntity.ok(userResponseDTO); // Return 200 OK with the user data
		} catch (UserNotFoundException ex) {
			// Return 404 if the user does not exist
			return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null);
		} catch (Exception ex) {
			// Return 500 Internal Server Error for unexpected issues
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
		}
	}

	// Fetch a user by ID
	@GetMapping("/id")
	public ResponseEntity<Optional<UserResponseDTO>> getUserByParam(@RequestParam long userId) {
		try {
			Optional<UserResponseDTO> userResponseDTO = userService.getUserById(userId);
			return ResponseEntity.ok(userResponseDTO); // Return 200 OK with the user data
		} catch (UserNotFoundException ex) {
			// Return 404 if the user does not exist
			return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null);
		} catch (Exception ex) {
			// Return 500 Internal Server Error for unexpected issues
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
		}
	}

	// Fetch all users
	@GetMapping("/")
	public ResponseEntity<List<UserResponseDTO>> getAllUsers() {
		try {
			List<UserResponseDTO> users = userService.getAllUsers();
			return ResponseEntity.ok(users); // Return 200 OK with the list of users
		} catch (Exception ex) {
			// Return 500 Internal Server Error for unexpected issues
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
		}
	}

	@PutMapping("/{userId}")
	public ResponseEntity<String> updateUser(@PathVariable long userId, @RequestBody UserDTO userDTO) {
		Optional<UserResponseDTO> existingUser = userService.getUserById(userId);

		// Check if the user exists
		if (existingUser.isEmpty()) {
			throw new UserNotFoundException("Invalid User ID: " + userId);
		}

		// Check if the user is active
		if (!"active".equalsIgnoreCase(existingUser.get().getStatus())) {
			throw new IllegalStateException("User is not active. Only active users can be updated.");
		}

		// Proceed with updating the user
		userService.updateUser(userId, userDTO);
		return ResponseEntity.ok("Updated Successfully!");
	}

	@DeleteMapping("/{userId}")
	public ResponseEntity<String> deactivateUser(@PathVariable long userId) {
		try {
			userService.deactivateUser(userId);
			return ResponseEntity.status(HttpStatus.CREATED).body("User deactivated Successfully!");
		} catch (IllegalArgumentException e) {
			throw new IllegalArgumentException("Invalid User ID: " + userId);
		} catch (Exception e) {
			throw new RuntimeException("An unexpected error occurred while deactivating the user.");
		}
	}

	@GetMapping("/validate-token")
	public ResponseEntity<Map<String, Object>> validateToken(
			@RequestHeader(HttpHeaders.AUTHORIZATION) String bearerToken) {

		try {
			Map<String, Object> response = new HashMap<>();
			// Validate header format
			if (bearerToken == null || !bearerToken.startsWith("Bearer ")) {
				response.put("valid", false);
				response.put("error", "Authorization header is missing or improperly formatted.");
				return ResponseEntity.ok(response);
			}

			// Remove "Bearer " prefix
			String token = bearerToken.substring(7);
			System.out.println("Extracted Token: " + token);
			Claims claims = jwtService.validateToken(token);
			response.put("valid", true);
			response.put("username", claims.getSubject());
			response.put("roles", claims.get("roles", List.class));
			response.put("claims", claims);
			return ResponseEntity.ok(response);
		} catch (Exception e) {
			Map<String, Object> response = new HashMap<>();
			response.put("valid", false);
			response.put("error", e.getMessage());
			return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
		}
	}

	@PostMapping("/refresh")
	public ResponseEntity<JwtResponseDTO> getNewTokens(@RequestBody RefreshTokenRequestDTO refreshToken) {
		try {
			System.out.println("hi" + refreshToken);
			JwtResponseDTO tokenResponse = refreshTokenService.getNewTokens(refreshToken);
			return ResponseEntity.ok(tokenResponse); // Return 200 OK with the user data
		} catch (UserNotFoundException ex) {
			// Return 404 if the user does not exist
			return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null);
		} catch (Exception ex) {
			// Return 500 Internal Server Error for unexpected issues
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
		}
	}
	
	@PatchMapping
	public void update(@RequestBody RefreshTokenRequestDTO refreshToken) {
		refreshTokenService.updateById(refreshToken);
	}
}
