package com.user.service;

import java.util.Optional;

import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Component;

import com.user.dto.UserResponseDTO;
import com.user.entity.User;
import com.user.entity.UserPrincipal;
import com.user.repository.UserRepository;

@Component
public class MyUserDetailsService implements UserDetailsService {

	@Autowired
	private UserRepository userRepo;

	@Autowired
	private ModelMapper modelMapper;

	@Override
	public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
		// TODO Auto-generated method stub
		Optional<User> user = userRepo.findByEmail(username);
		UserResponseDTO userResponse = modelMapper.map(user.get(), UserResponseDTO.class);
		return new UserPrincipal(userResponse);
	}

}
