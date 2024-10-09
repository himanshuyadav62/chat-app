package com.example.backend.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import com.example.backend.entity.User;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {


    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);

    @Query("SELECT u.email AS email, u.fullName AS fullName FROM User u WHERE u.email <> :currentUserEmail")
    List<Map<String, String>> findAllEmailsAndFullNamesExcept(@Param("currentUserEmail") String currentUserEmail);

    @Transactional
    @Modifying
    @Query("UPDATE User u SET u.lastSeenAt = :localDateTime WHERE u.email = :email")
    void updateLastSeenAt(@Param("email") String email, @Param("localDateTime") LocalDateTime localDateTime);

    @Query("SELECT u.lastSeenAt FROM User u WHERE u.email = :email")
    LocalDateTime getLastSeenByEmail(String email);

}
