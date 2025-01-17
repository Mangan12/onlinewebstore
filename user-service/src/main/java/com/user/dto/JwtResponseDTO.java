package com.user.dto;

import lombok.Builder;

@Builder
public class JwtResponseDTO {
	
    private String accessToken;
    
    private String token;
    
	public String getAccessToken() {
		return accessToken;
	}
	public void setAccessToken(String accessToken) {
		this.accessToken = accessToken;
	}
	public String getToken() {
		return token;
	}
	public void setToken(String token) {
		this.token = token;
	}
    
    
}

