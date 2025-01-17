package com.user.token.service;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.user.dto.JwtResponseDTO;
import com.user.dto.RefreshTokenRequestDTO;
import com.user.repository.RefreshTokenRepository;
import com.user.repository.UserRepository;
import com.user.token.RefreshToken;

import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class RefreshTokenServiceImpl implements RefreshTokenService {

	@Autowired
	RefreshTokenRepository refreshTokenRepository;

	@Autowired
	private JWTService jwtService;

	@Autowired
	UserRepository userRepository;

	public RefreshToken createRefreshToken(String username) {
		RefreshToken refreshToken = RefreshToken.builder().userInfo(userRepository.findByEmail(username).get())
				.token(UUID.randomUUID().toString()).expiryDate(Instant.now().plusMillis(600000)).status("a").build();
		return refreshTokenRepository.save(refreshToken);
	}

	public Optional<RefreshToken> findByToken(String token) {
		return refreshTokenRepository.findByToken(token);
	}

	public RefreshToken verifyExpiration(RefreshToken token) {
		if (token.getExpiryDate().compareTo(Instant.now()) < 0) {
			refreshTokenRepository.delete(token);
			throw new RuntimeException(token.getToken() + " Refresh token is expired. Please make a new login..!");
		}
		return token;
	}

	@Override
	public JwtResponseDTO getNewTokens(RefreshTokenRequestDTO refreshTokenRequest) {
		// Debug: Print the incoming refresh token request
		log.info("Received refresh token request: {}", refreshTokenRequest.getRefreshToken());

		// Fetch the refresh token from the database
		Optional<RefreshToken> existingtokenInfo = refreshTokenRepository.findByToken(refreshTokenRequest.getRefreshToken());

		// Validate if the token exists
		if (existingtokenInfo.isEmpty()) {
			log.error("Invalid refresh token: Token not found in database.");
			throw new RuntimeException("Invalid refresh token: Token not found.");
		}

		// Validate if the refresh token is expired
		if (existingtokenInfo.get().getExpiryDate().isBefore(Instant.now()) || existingtokenInfo.get().getStatus().equalsIgnoreCase("i")) {
			log.warn("Invalid refresh token: Token has expired. Token: {}", refreshTokenRequest.getRefreshToken());
			throw new RuntimeException("Invalid refresh token: Token has expired.");
		}

		// Retrieve the user's email from the refresh token
		String email = existingtokenInfo.get().getUserInfo().getEmail();
		log.info("Refresh token is valid. Associated email: {}", email);
		System.out.println("Extracted email from refresh token: " + email);

		// Rotate the refresh token (invalidate old one and create a new one)
		log.info("Fetching new Token info using: {}, {}", existingtokenInfo.get().getUserInfo().getUserId(),
				refreshTokenRequest.getRefreshToken());

		Optional<RefreshToken> newToken = Optional.of(createRefreshToken(email));

		if (newToken.isPresent()) {
			updateById(refreshTokenRequest);
		} else {
			throw new RuntimeException("Cannot Create Token!");
		}

		log.info("Updated to new refresh token {}", newToken.get().getId());

		// Generate a new access token
		String newAccessToken = jwtService.generateToken(email);

		log.info("Successfully processed refresh token request for email: {}", email);

		// Return the new access token and refresh token in the response
		return JwtResponseDTO.builder().accessToken(newAccessToken).token(newToken.get().getToken()).build();
	}

	public void updateById(RefreshTokenRequestDTO refreshTokenRequestDTO) {
		refreshTokenRepository.updateById(refreshTokenRequestDTO.getRefreshToken());
	}

}
