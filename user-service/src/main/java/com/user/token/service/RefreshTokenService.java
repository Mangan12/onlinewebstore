package com.user.token.service;

import java.util.Optional;

import com.user.dto.JwtResponseDTO;
import com.user.dto.RefreshTokenRequestDTO;
import com.user.token.RefreshToken;

public interface RefreshTokenService {

	RefreshToken createRefreshToken(String username);
	
	Optional<RefreshToken> findByToken(String token);
	
	RefreshToken verifyExpiration(RefreshToken token);

	JwtResponseDTO getNewTokens(RefreshTokenRequestDTO refreshToken);

	void updateById(RefreshTokenRequestDTO refreshToken);
}
