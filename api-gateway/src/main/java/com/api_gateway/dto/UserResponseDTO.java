package com.api_gateway.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Builder
public class UserResponseDTO {

	private long userId;

	@NotBlank(message = "First name cannot be blank")
	@Size(max = 50, message = "First name cannot exceed 50 characters")
	private String firstName;

	private String middleName;

	@Size(max = 50, message = "Last name cannot exceed 50 characters")
	private String lastName;

	@Email(message = "Invalid email format")
	@NotBlank(message = "Email cannot be blank")
	private String email;

	@NotBlank(message = "Password cannot be blank")
	@Size(min = 8, message = "Password must be at least 8 characters long")
	private String password;

	@Pattern(regexp = "\\d{10}", message = "Phone number must be 10 digits")
	@NotBlank(message = "Phone number cannot be blank")
	private String phoneNumber;

	private String address;

	private String city;

	private String state;

	private int pincode;

	@NotBlank(message = "Role cannot be blank")
	private String role;

	private String status;

	private boolean accountLocked;

	private int failedAttempts;

	private boolean emailVerified;

}
