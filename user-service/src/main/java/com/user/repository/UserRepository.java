package com.user.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.user.entity.User;

public interface UserRepository extends JpaRepository<User, Long> {

	Optional<User> findByEmailAndPassword(String email, String password);

	@Modifying
	@Query("UPDATE User u SET " +
	       "u.email = :#{#user.email}, " +
	       "u.password = :#{#user.password}, " +
	       "u.firstName = :#{#user.firstName}, " +
	       "u.middleName = :#{#user.middleName}, " +
	       "u.lastName = :#{#user.lastName}, " +
	       "u.phoneNumber = :#{#user.phoneNumber}, " +
	       "u.address = :#{#user.address}, " +
	       "u.city = :#{#user.city}, " +
	       "u.state = :#{#user.state}, " +
	       "u.pincode = :#{#user.pincode}, " +
	       "u.updatedAt = CURRENT_TIMESTAMP " +
	       "WHERE u.userId = :userId")
	void updateUser(@Param("userId") long userId, @Param("user") User user);

	@Modifying
	@Query("UPDATE User u SET u.status = 'Inactive' WHERE u.userId = :userId")
	void deactivateStatus(@Param("userId") long userId);


}
