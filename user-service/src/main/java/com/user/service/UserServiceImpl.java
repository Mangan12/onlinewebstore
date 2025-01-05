package com.user.service;

import java.util.List;
import java.util.Optional;

import org.modelmapper.ModelMapper;
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

	public UserServiceImpl(UserRepository userRepo, ModelMapper modelMapper) {
		this.userRepo = userRepo;
		this.modelMapper= modelMapper;
	}

	@Override
	public boolean authenticateUser(@Valid UserLoginDTO userLoginDTO) {
		return userRepo.findByEmailAndPassword(userLoginDTO.getEmail(),userLoginDTO.getPassword()).isPresent();
	}

	@Override
	public void registerUser(@Valid UserRegisterDTO userRegisterDTO) {
		User user = modelMapper.map(userRegisterDTO, User.class);
		userRepo.save(user);
	}

	@Override
	public Optional<UserResponseDTO> getUserById(long userId) {
		// TODO Auto-generated method stub
		Optional<User> OptionalUser = userRepo.findById(userId);
		return OptionalUser.map(user-> modelMapper.map(OptionalUser, UserResponseDTO.class));
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
		if(checkUser) {
			User user = modelMapper.map(userDTO, User.class);
			userRepo.updateUser(userId, user);
		}else {
			throw new IllegalArgumentException("User with ID " + userId + " not found.");
		}
	}

	@Override
	public void deactivateUser(long userId) {
		userRepo.deactivateStatus(userId);
	}
	

}
