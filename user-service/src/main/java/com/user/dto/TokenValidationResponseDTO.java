package com.user.dto;

import java.util.Map;

public class TokenValidationResponseDTO {
    private Map<String, Object> claims;

    // Getter and Setter
    public Map<String, Object> getClaims() {
        return claims;
    }

    public void setClaims(Map<String, Object> claims) {
        this.claims = claims;
    }

    // Override toString() for logging or debugging
    @Override
    public String toString() {
        return "TokenValidationResponseDTO{" +
                "claims=" + claims +
                '}';
    }
}

