package com.user.controller;

import java.util.List;
import java.util.Optional;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.user.dto.UserDTO;
import com.user.dto.UserLoginDTO;
import com.user.dto.UserRegisterDTO;
import com.user.dto.UserResponseDTO;
import com.user.exception.UserNotFoundException;
import com.user.service.UserService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;

@RestController
@RequestMapping("api/users")
public class UserController {

	private final UserService userService;

	public UserController(UserService userService) {
		this.userService = userService;
	}

	@PostMapping("/login")
	public ResponseEntity<String> userLogin(@RequestBody @Valid UserLoginDTO userLoginDTO) {
		boolean isAuthenticated = userService.authenticateUser(userLoginDTO);
		if (isAuthenticated) {
			return ResponseEntity.ok("Login Successful!");
		} else {
			return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid email or password!");
		}
	}

	@PostMapping("/register")
	@ResponseStatus(HttpStatus.OK)
	public ResponseEntity<String> userRegister(@RequestBody @Valid UserRegisterDTO userRegisterDTO) {
		userService.registerUser(userRegisterDTO);
		return ResponseEntity.status(HttpStatus.CREATED).body("Registration Successfull!");
	}

	// Fetch a user by ID
	@GetMapping("/{userId}")
	public ResponseEntity<Optional<UserResponseDTO>> getUser(@PathVariable long userId) {
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

}
