package com.user.service;

import java.util.List;
import java.util.Optional;

import com.user.dto.JwtResponseDTO;
import com.user.dto.RefreshTokenRequestDTO;
import com.user.dto.UserDTO;
import com.user.dto.UserLoginDTO;
import com.user.dto.UserRegisterDTO;
import com.user.dto.UserResponseDTO;
import com.user.token.RefreshToken;

import jakarta.validation.Valid;

public interface UserService {

	void registerUser(@Valid UserRegisterDTO userRegisterDTO);

	Optional<UserResponseDTO> getUserById(long userId);

	List<UserResponseDTO> getAllUsers();

	void updateUser(long userId, UserDTO userDTO);

	void deactivateUser(long userId);

	JwtResponseDTO authenticateUser(@Valid UserLoginDTO userLoginDTO);

	Optional<UserResponseDTO> getUserByEmail(String email);
}
