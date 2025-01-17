package com.user.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import com.user.token.RefreshToken;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {
	Optional<RefreshToken> findByToken(String token);

//	@Modifying
//    @Transactional
	@Query(value = "SELECT a FROM RefreshToken a WHERE a.userInfo.userId = :userId AND a.token = :token")
	RefreshToken updateRecord(@Param("userId") long userId, @Param("token") String token);

	@Modifying
	@Transactional
	@Query(value = "UPDATE RefreshToken SET status = 'i' WHERE token = :token")
	void updateById(@Param("token") String token);

}
