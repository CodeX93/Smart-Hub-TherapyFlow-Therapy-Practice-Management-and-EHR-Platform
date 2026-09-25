package com.smart.therapy.flow.unit.controller;

import com.smart.therapy.flow.auth.entity.AuthIdentity;
import com.smart.therapy.flow.auth.entity.IdentityType;
import com.smart.therapy.flow.auth.repository.AuthIdentityRepository;
import com.smart.therapy.flow.auth.service.AuthIdentityService;
import com.smart.therapy.flow.auth.service.AuthenticationService;
import com.smart.therapy.flow.common.security.AuthPrincipal;
import com.smart.therapy.flow.common.tenant.TenantContext;
import com.smart.therapy.flow.superadmin.controller.SuperAdminProfileController;
import com.smart.therapy.flow.user.dto.UpdateUserRequest;
import com.smart.therapy.flow.user.dto.UserProfileRequest;
import com.smart.therapy.flow.user.dto.UserProfileResponse;
import com.smart.therapy.flow.user.dto.UserResponse;
import com.smart.therapy.flow.user.service.UserService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SuperAdminProfileControllerTest {

    @Mock
    private UserService userService;
    @Mock
    private AuthIdentityService authIdentityService;
    @Mock
    private AuthIdentityRepository authIdentityRepository;
    @Mock
    private AuthenticationService authenticationService;

    @InjectMocks
    private SuperAdminProfileController controller;

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    void updateMeShouldPersistAndReturnFullNameAndPhoneForPublicPlatformPrincipal() {
        TenantContext.setSchemaName("public");
        AuthPrincipal principal = principal(99L, "old-admin@therapyflow.pro");
        UpdateUserRequest request = new UpdateUserRequest();
        request.setEmail("new-admin@therapyflow.pro");
        request.setFullName("  Jane Platform  ");
        request.setPhone("  +1-555-123-4567  ");

        AuthIdentity identity = AuthIdentity.builder()
                .id(99L)
                .identityType(IdentityType.STAFF)
                .loginIdentifier("old-admin@therapyflow.pro")
                .normalisedLoginIdentifier("old-admin@therapyflow.pro")
                .isActive(true)
                .build();

        when(authIdentityService.getById(99L)).thenReturn(identity);
        stubEmailUpdate(identity, "new-admin@therapyflow.pro");
        when(authIdentityRepository.save(any(AuthIdentity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UserResponse response = controller.updateMe(request, principal, new MockHttpServletRequest()).getBody();

        assertNotNull(response);
        assertEquals("old-admin@therapyflow.pro", response.getUsername());
        verify(authIdentityService).updateEmail(identity, "new-admin@therapyflow.pro");
        assertEquals("new-admin@therapyflow.pro", response.getEmail());
        assertEquals("Jane Platform", response.getFullName());
        assertEquals("+1-555-123-4567", response.getPhone());
        verify(authIdentityRepository).save(identity);
    }

    @Test
    void updateProfileShouldPersistFullNameForPublicPlatformPrincipal() {
        TenantContext.setSchemaName("public");
        AuthPrincipal principal = principal(42L, "platform@therapyflow.pro");
        UserProfileRequest request = new UserProfileRequest();
        request.setEmail("owner@therapyflow.pro");
        request.setFullName("  Platform Owner ");

        AuthIdentity identity = AuthIdentity.builder()
                .id(42L)
                .identityType(IdentityType.STAFF)
                .loginIdentifier("platform@therapyflow.pro")
                .normalisedLoginIdentifier("platform@therapyflow.pro")
                .isActive(true)
                .build();

        when(authIdentityService.getById(42L)).thenReturn(identity);
        stubEmailUpdate(identity, "owner@therapyflow.pro");
        when(authIdentityRepository.save(any(AuthIdentity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UserProfileResponse response = controller.updateProfile(request, principal, new MockHttpServletRequest()).getBody();

        assertNotNull(response);
        assertEquals("owner@therapyflow.pro", response.getEmail());
        assertEquals("Platform Owner", response.getFullName());
        verify(authIdentityRepository).save(identity);
    }

    @Test
    void updateMeShouldNotSaveWhenNoSupportedFieldIsProvided() {
        TenantContext.setSchemaName("public");
        AuthPrincipal principal = principal(7L, "readonly@therapyflow.pro");
        UpdateUserRequest request = new UpdateUserRequest();

        AuthIdentity identity = AuthIdentity.builder()
                .id(7L)
                .identityType(IdentityType.STAFF)
                .loginIdentifier("readonly@therapyflow.pro")
                .normalisedLoginIdentifier("readonly@therapyflow.pro")
                .fullName("Readonly Admin")
                .phone("+1-555-000-0000")
                .isActive(true)
                .build();

        when(authIdentityService.getById(7L)).thenReturn(identity);

        UserResponse response = controller.updateMe(request, principal, new MockHttpServletRequest()).getBody();

        assertNotNull(response);
        assertEquals("Readonly Admin", response.getFullName());
        assertEquals("+1-555-000-0000", response.getPhone());
        verify(authIdentityRepository, never()).save(any(AuthIdentity.class));
    }

    @Test
    void usernameChangePreservesIndependentEmail() {
        TenantContext.setSchemaName("public");
        AuthIdentity identity = AuthIdentity.builder().id(99L).identityType(IdentityType.STAFF)
                .username("old-admin").loginIdentifier("old-admin")
                .email("contact@example.com").isActive(true).build();
        UpdateUserRequest request = new UpdateUserRequest();
        request.setUsername("new-admin");
        when(authIdentityService.getById(99L)).thenReturn(identity);
        when(authIdentityService.updateUsername(identity, "new-admin")).thenAnswer(invocation -> {
            identity.setUsername("new-admin");
            identity.setLoginIdentifier("new-admin");
            return identity;
        });
        when(authIdentityRepository.save(identity)).thenReturn(identity);

        UserResponse response = controller.updateMe(request, principal(99L, "old-admin"),
                new MockHttpServletRequest()).getBody();

        assertNotNull(response);
        assertEquals("new-admin", response.getUsername());
        assertEquals("contact@example.com", response.getEmail());
        verify(authIdentityService, never()).updateEmail(any(), any());
    }

    private void stubEmailUpdate(AuthIdentity identity, String email) {
        when(authIdentityService.updateEmail(identity, email)).thenAnswer(invocation -> {
            identity.setEmail(email);
            identity.setNormalisedEmail(email);
            return identity;
        });
    }

    private static AuthPrincipal principal(Long authId, String loginIdentifier) {
        return new AuthPrincipal(
                authId,
                loginIdentifier,
                "hash",
                List.of(new SimpleGrantedAuthority("ROLE_PLATFORM_SUPER_ADMIN")),
                true,
                IdentityType.STAFF,
                null
        );
    }
}
