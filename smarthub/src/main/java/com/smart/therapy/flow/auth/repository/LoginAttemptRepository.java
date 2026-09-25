package com.smart.therapy.flow.auth.repository;

import com.smart.therapy.flow.auth.entity.LoginAttempt;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface LoginAttemptRepository extends JpaRepository<LoginAttempt, Long> {
    List<LoginAttempt> findByUsername(String username);
}

