package com.user.token.service;

import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;

import org.springframework.stereotype.Service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;

@Service
public class JWTService {

	// secret key for token generation and token validation
	private static String secretKey;

	public JWTService() throws NoSuchAlgorithmException {
		KeyGenerator kgen = KeyGenerator.getInstance("HmacSHA256");
		SecretKey sk = kgen.generateKey();
		secretKey = Base64.getEncoder().encodeToString(sk.getEncoded());
	}
	
	// token generation process
	public String generateToken(String username) {

		// create map<string and object> of claims

		Map<String, Object> claims = new HashMap<>();

		return Jwts.builder().claims().add(claims).subject(username).issuedAt(new Date(System.currentTimeMillis()))
				.expiration(new Date(System.currentTimeMillis() + 60 * 60 * 90)).and().signWith(getKey()).compact();

	}

	private SecretKey getKey() {
		// TODO Auto-generated method stub
		byte[] keyBytes = Decoders.BASE64.decode(secretKey);
		return Keys.hmacShaKeyFor(keyBytes);
	}

    // token validation process
	// check expiry of the token 
	private Boolean isTokenExpired(String token) {
        return extractExpiration(token).before(new Date());
    }
	
	public Claims validateToken(String token) {
		try {
			final String username = extractUsername(token);

			Claims claims = Jwts.parser().verifyWith(getKey()).build().parseSignedClaims(token).getPayload();

			return claims;

		} catch (ExpiredJwtException e) {
			throw new JwtException("Token has expired");
		} catch (Exception e) {
			throw new JwtException("Invalid token");
		}
	}

	private String extractUsername(String token) {
		// TODO Auto-generated method stub
		return extractClaim(token, Claims::getSubject);
	}
	
	private Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

	private <T> T extractClaim(String token, Function<Claims, T> claimResolver) {
		final Claims claims = extractAllClaims(token);
		return claimResolver.apply(claims);
	}

	private Claims extractAllClaims(String token) {
		// TODO Auto-generated method stub
		return Jwts.parser().verifyWith(getKey()).build().parseSignedClaims(token).getPayload();
	}

}
