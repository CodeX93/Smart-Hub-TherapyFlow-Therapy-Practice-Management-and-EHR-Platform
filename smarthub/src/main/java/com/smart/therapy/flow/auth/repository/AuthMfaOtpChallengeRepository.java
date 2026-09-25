package com.smart.therapy.flow.auth.repository;

import com.smart.therapy.flow.auth.entity.AuthMfaOtpChallenge;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface AuthMfaOtpChallengeRepository extends JpaRepository<AuthMfaOtpChallenge, Long> {

    Optional<AuthMfaOtpChallenge> findByJtiHashAndPurpose(String jtiHash, AuthMfaOtpChallenge.Purpose purpose);
}
