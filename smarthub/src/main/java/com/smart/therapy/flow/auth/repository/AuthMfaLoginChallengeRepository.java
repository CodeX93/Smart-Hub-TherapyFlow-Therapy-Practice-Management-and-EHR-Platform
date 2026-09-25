package com.smart.therapy.flow.auth.repository;

import com.smart.therapy.flow.auth.entity.AuthMfaLoginChallenge;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

import java.util.Optional;

public interface AuthMfaLoginChallengeRepository extends JpaRepository<AuthMfaLoginChallenge, Long> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<AuthMfaLoginChallenge> findByJtiHash(String jtiHash);
}
