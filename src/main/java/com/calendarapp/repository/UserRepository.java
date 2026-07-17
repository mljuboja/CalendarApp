package com.calendarapp.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.calendarapp.entity.User;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);
}
