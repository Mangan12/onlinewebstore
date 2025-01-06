package com.user.service;

import java.util.List;
import java.util.Optional;

import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import com.user.dto.UserDTO;
import com.user.dto.UserLoginDTO;
import com.user.dto.UserRegisterDTO;
import com.user.dto.UserResponseDTO;
import com.user.entity.User;
import com.user.repository.UserRepository;

import jakarta.transaction.Transactional;
import jakarta.validation.Valid;

@Service
@Transactional
public class UserServiceImpl implements UserService {

	private final UserRepository userRepo;
	private final ModelMapper modelMapper;

	@Autowired
	@Lazy
	private AuthenticationManager authenticationManager;

	@Autowired
	private BCryptPasswordEncoder passwordEncoder;

	public UserServiceImpl(UserRepository userRepo, ModelMapper modelMapper) {
		this.userRepo = userRepo;
		this.modelMapper = modelMapper;
	}

	@Override
	public boolean authenticateUser(@Valid UserLoginDTO userLoginDTO) {
	    try {
	    	System.out.println(userLoginDTO.toString());
	        // Attempt to authenticate the user
	    	 // Attempt to authenticate the user
	        Authentication auth = authenticationManager.authenticate(
	                new UsernamePasswordAuthenticationToken(userLoginDTO.getEmail(), userLoginDTO.getPassword()));

	        // Check if authentication was successful
	        if (auth.isAuthenticated()) {
	            System.out.println("Authentication successful for user: " + userLoginDTO.getEmail());
	            return true;
	        } else {
	            System.err.println("Authentication failed for user: " + userLoginDTO.getEmail());
	            return false;
	        }
	    } catch (BadCredentialsException e) {
	        // Handle invalid credentials
	        System.err.println("Authentication failed: Invalid username or password.");
	        e.printStackTrace();
	        throw new RuntimeException("Invalid username or password.", e);
	    } catch (DisabledException e) {
	        // Handle disabled user account
	        System.err.println("Authentication failed: User account is disabled.");
	        e.printStackTrace();
	        throw new RuntimeException("User account is disabled.", e);
	    } catch (LockedException e) {
	        // Handle locked user account
	        System.err.println("Authentication failed: User account is locked.");
	        e.printStackTrace();
	        throw new RuntimeException("User account is locked.", e);
	    } catch (AuthenticationServiceException e) {
	        // Handle internal errors during authentication
	        System.err.println("Authentication failed: Internal authentication service error.");
	        e.printStackTrace();
	        throw new RuntimeException("Internal authentication service error.", e);
	    } catch (Exception e) {
	        // Handle any other unexpected errors
	        System.err.println("Authentication failed: An unexpected error occurred.");
	        e.printStackTrace();
	        throw new RuntimeException("An unexpected error occurred during authentication.", e);
	    }
	}

	
	

	@Override
	public void registerUser(@Valid UserRegisterDTO userRegisterDTO) {
		User user = modelMapper.map(userRegisterDTO, User.class);
		user.setPassword(passwordEncoder.encode(userRegisterDTO.getPassword()));
		userRepo.save(user);
	}

	@Override
	public Optional<UserResponseDTO> getUserById(long userId) {
		// TODO Auto-generated method stub
		Optional<User> OptionalUser = userRepo.findById(userId);
		return OptionalUser.map(user -> modelMapper.map(OptionalUser, UserResponseDTO.class));
	}

	@Override
	public List<UserResponseDTO> getAllUsers() {
		// TODO Auto-generated method stub
		List<User> users = userRepo.findAll();
		return users.stream().map(user -> modelMapper.map(user, UserResponseDTO.class)).toList();
	}

	@Override
	public void updateUser(long userId, UserDTO userDTO) {
		boolean checkUser = userRepo.findById(userId).isPresent();
		if (checkUser) {
			User user = modelMapper.map(userDTO, User.class);
			userRepo.updateUser(userId, user);
		} else {
			throw new IllegalArgumentException("User with ID " + userId + " not found.");
		}
	}

	@Override
	public void deactivateUser(long userId) {
		userRepo.deactivateStatus(userId);
	}

}
