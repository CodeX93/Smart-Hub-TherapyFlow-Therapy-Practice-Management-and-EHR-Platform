package com.smart.therapy.flow.auth.service;

import com.smart.therapy.flow.auth.repository.AuthRefreshTokenRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

/**
 * Commits the theft response for a replayed refresh token in its own transaction. The refresh request
 * still fails with 401, and that exception rolls back the caller's transaction; the reuse flag and the
 * family revocation must survive it. A separate bean so the REQUIRES_NEW proxy applies.
 */
@Service
@RequiredArgsConstructor
public class AuthRefreshTokenReuseRecorder {

    private final AuthRefreshTokenRepository refreshTokenRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordReuse(Long tokenId, String familyId, Instant now) {
        refreshTokenRepository.markReuseDetected(tokenId);
        refreshTokenRepository.revokeFamily(familyId, now);
    }
}
