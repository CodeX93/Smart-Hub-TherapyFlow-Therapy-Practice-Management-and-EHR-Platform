package com.smart.therapy.flow.common.security;

import com.smart.therapy.flow.auth.entity.AuthIdentity;
import com.smart.therapy.flow.auth.entity.IdentityType;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.Objects;

/**
 * Minimal principal returned by UserDetailsService.
 * {@link #getUsername()} returns authId (immutable identity id) for Spring Security only.
 * For audit actor labels use {@link #getLoginIdentifier()} (staff username / portal email).
 */
@Getter
@AllArgsConstructor
public final class AuthPrincipal implements UserDetails {

    private final Long authId;
    private final String loginIdentifier;
    private final String passwordHash;
    private final Collection<? extends GrantedAuthority> authorities;
    private final boolean active;
    private final IdentityType identityType;
    /** Platform super-admin auth id when this session is an impersonation token; null otherwise. */
    private final Long impersonatorAuthId;

    public static AuthPrincipal create(AuthIdentity auth, Collection<? extends GrantedAuthority> authorities) {
        return create(auth, authorities, null);
    }

    public static AuthPrincipal create(
            AuthIdentity auth,
            Collection<? extends GrantedAuthority> authorities,
            Long impersonatorAuthId) {
        return new AuthPrincipal(
            auth.getId(),
            auth.getLoginIdentifier(),
            auth.getPasswordHash(),
            authorities,
            Boolean.TRUE.equals(auth.getIsActive()),
            auth.getIdentityType(),
            impersonatorAuthId
        );
    }

    public AuthPrincipal withImpersonator(Long impersonatorAuthId) {
        if (Objects.equals(this.impersonatorAuthId, impersonatorAuthId)) {
            return this;
        }
        return new AuthPrincipal(
                authId,
                loginIdentifier,
                passwordHash,
                authorities,
                active,
                identityType,
                impersonatorAuthId);
    }

    @Override
    public String getUsername() {
        return String.valueOf(authId);
    }

    @Override
    public String getPassword() {
        return passwordHash;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return active;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return active;
    }
}
