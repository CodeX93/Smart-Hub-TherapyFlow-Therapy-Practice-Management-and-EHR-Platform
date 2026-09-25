package com.smart.therapy.flow.unit.service;

import com.smart.therapy.flow.auth.entity.AuthIdentity;
import com.smart.therapy.flow.auth.entity.Role;
import com.smart.therapy.flow.auth.entity.User;
import com.smart.therapy.flow.auth.entity.UserActivityLog;
import com.smart.therapy.flow.auth.repository.AuthIdentityRepository;
import com.smart.therapy.flow.auth.repository.AuthIdentityRoleRepository;
import com.smart.therapy.flow.auth.repository.AuditLogRepository;
import com.smart.therapy.flow.auth.repository.RoleRepository;
import com.smart.therapy.flow.auth.repository.UserActivityLogRepository;
import com.smart.therapy.flow.auth.repository.UserRepository;
import com.smart.therapy.flow.auth.service.AuthIdentityService;
import com.smart.therapy.flow.common.TestDataFactory;
import com.smart.therapy.flow.common.dto.PaginatedResponse;
import com.smart.therapy.flow.common.exception.BadRequestException;
import com.smart.therapy.flow.common.exception.ForbiddenException;
import com.smart.therapy.flow.common.exception.ResourceNotFoundException;
import com.smart.therapy.flow.common.security.AuthIdentityDetailsService;
import com.smart.therapy.flow.common.security.AuthPrincipal;
import com.smart.therapy.flow.common.security.CurrentUserService;
import com.smart.therapy.flow.common.security.PermissionChecker;
import com.smart.therapy.flow.common.security.TokenBlacklistService;
import com.smart.therapy.flow.common.service.AuditService;
import com.smart.therapy.flow.common.tenant.TenantContext;
import com.smart.therapy.flow.common.util.RoleName;
import com.smart.therapy.flow.organisation.entity.Organisation;
import com.smart.therapy.flow.organisation.repository.OrganisationRepository;
import com.smart.therapy.flow.organisation.repository.UserOrganisationRepository;
import com.smart.therapy.flow.subscription.service.SubscriptionFeatureService;
import com.smart.therapy.flow.user.dto.*;
import com.smart.therapy.flow.user.entity.ShiftMode;
import com.smart.therapy.flow.user.entity.UserProfileWorkingHours;
import com.smart.therapy.flow.user.repository.UserIdempotencyKeyRepository;
import com.smart.therapy.flow.user.repository.UserIntegrationRepository;
import com.smart.therapy.flow.user.repository.SupervisorAssignmentRepository;
import com.smart.therapy.flow.user.repository.UserContactRepository;
import com.smart.therapy.flow.user.repository.UserProfileRepository;
import com.smart.therapy.flow.user.repository.UserProfileAgeGroupRepository;
import com.smart.therapy.flow.user.repository.UserProfileAwardRepository;
import com.smart.therapy.flow.user.repository.UserProfileCertificationRepository;
import com.smart.therapy.flow.user.repository.UserProfileContinuingEducationRepository;
import com.smart.therapy.flow.user.repository.UserProfileEducationRepository;
import com.smart.therapy.flow.user.repository.UserProfileLanguageRepository;
import com.smart.therapy.flow.user.repository.UserProfileMembershipRepository;
import com.smart.therapy.flow.user.repository.UserProfilePhysicalRoomRepository;
import com.smart.therapy.flow.user.repository.UserProfilePreviousPositionRepository;
import com.smart.therapy.flow.user.repository.UserProfilePublicationRepository;
import com.smart.therapy.flow.user.repository.UserProfileReferenceRepository;
import com.smart.therapy.flow.user.repository.UserProfileSpecializationRepository;
import com.smart.therapy.flow.user.repository.UserProfileTreatmentApproachRepository;
import com.smart.therapy.flow.user.repository.UserProfileWorkingHoursRepository;
import com.smart.therapy.flow.user.service.UserService;
import com.smart.therapy.flow.user.validation.UserProfileRequestValidator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalTime;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("UserService Unit Tests")
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private RoleRepository roleRepository;

    @Mock
    private UserIdempotencyKeyRepository idempotencyKeyRepository;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @Mock
    private AuditService auditService;

    @Mock
    private UserProfileRequestValidator profileRequestValidator;

    @Mock
    private UserProfileRepository userProfileRepository;

    @Mock
    private SupervisorAssignmentRepository supervisorAssignmentRepository;

    @Mock
    private AuditLogRepository auditLogRepository;

    @Mock
    private UserActivityLogRepository userActivityLogRepository;

    @Mock
    private UserIntegrationRepository userIntegrationRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private CurrentUserService currentUserService;

    @Mock
    private PermissionChecker permissionChecker;

    @Mock
    private AuthIdentityDetailsService authIdentityDetailsService;

    @Mock
    private AuthIdentityService authIdentityService;

    @Mock
    private AuthIdentityRoleRepository authIdentityRoleRepository;

    @Mock
    private AuthIdentityRepository authIdentityRepository;

    @Mock
    private SubscriptionFeatureService subscriptionFeatureService;

    @Mock
    private OrganisationRepository organisationRepository;

    @Mock
    private UserOrganisationRepository userOrganisationRepository;

    @Mock
    private UserProfileLanguageRepository languageRepository;

    @Mock
    private UserProfileCertificationRepository certificationRepository;

    @Mock
    private UserProfileSpecializationRepository specializationRepository;

    @Mock
    private UserProfileTreatmentApproachRepository treatmentApproachRepository;

    @Mock
    private UserProfileAgeGroupRepository ageGroupRepository;

    @Mock
    private UserProfileEducationRepository educationRepository;

    @Mock
    private UserProfileContinuingEducationRepository continuingEducationRepository;

    @Mock
    private UserProfileMembershipRepository membershipRepository;

    @Mock
    private UserProfileAwardRepository awardRepository;

    @Mock
    private UserProfilePublicationRepository publicationRepository;

    @Mock
    private UserProfileReferenceRepository referenceRepository;

    @Mock
    private UserProfilePreviousPositionRepository previousPositionRepository;

    @Mock
    private UserProfilePhysicalRoomRepository physicalRoomRepository;

    @Mock
    private UserProfileWorkingHoursRepository workingHoursRepository;

    @Mock
    private com.smart.therapy.flow.billing.repository.ServiceRepository serviceRepository;

    @Mock
    private UserContactRepository userContactRepository;

    @Mock
    private com.smart.therapy.flow.session.repository.RoomRepository roomRepository;

    @Mock
    private TokenBlacklistService tokenBlacklistService;

    @Mock
    private RestTemplate restTemplate;

    @InjectMocks
    private UserService userService;

    private AuthPrincipal adminPrincipal;
    private AuthPrincipal therapistPrincipal;
    private User admin;
    private User therapist;
    private Role therapistRole;

    @BeforeEach
    void setUp() {
        TenantContext.setOrganisationId(1L);
        TenantContext.setSchemaName("tenant_1");

        admin = TestDataFactory.createTestAdmin();
        admin.setId(1L);
        adminPrincipal = TestDataFactory.createAuthPrincipal(admin);

        therapist = TestDataFactory.createTestTherapist();
        therapist.setId(2L);
        therapistPrincipal = TestDataFactory.createAuthPrincipal(therapist);

        lenient().when(currentUserService.requireCurrentUser(any(AuthPrincipal.class))).thenAnswer(inv -> {
            AuthPrincipal ap = inv.getArgument(0);
            if (ap.getAuthId().equals(admin.getAuthIdentity().getId())) return admin;
            return therapist;
        });

        lenient().when(permissionChecker.hasRole(eq(adminPrincipal), eq(RoleName.ADMIN.name()))).thenReturn(true);
        lenient().when(permissionChecker.hasRole(eq(adminPrincipal), eq(RoleName.SUPERVISOR.name()))).thenReturn(false);
        lenient().when(permissionChecker.hasRole(eq(therapistPrincipal), eq(RoleName.ADMIN.name()))).thenReturn(false);
        lenient().when(permissionChecker.hasRole(eq(therapistPrincipal), eq(RoleName.SUPERVISOR.name()))).thenReturn(false);
        lenient().when(permissionChecker.hasPermission(eq(adminPrincipal), anyString())).thenReturn(true);
        lenient().when(permissionChecker.hasPermission(eq(therapistPrincipal), anyString())).thenReturn(false);
        lenient().when(auditService.serializeUserState(any(User.class))).thenReturn("{}");
        lenient().when(authIdentityRepository.existsByNormalisedLoginIdentifier(anyString())).thenReturn(false);
        lenient().when(authIdentityRepository.findByNormalisedLoginIdentifier(anyString())).thenReturn(Optional.empty());
        lenient().doNothing().when(authIdentityService).assertUsernameAvailable(any(), anyString(), any());
        lenient().doNothing().when(authIdentityService).assertEmailAvailable(any(), anyString(), any());
        Organisation org = Organisation.builder().id(1L).name("Test Org").schemaName("tenant_1").build();
        lenient().when(organisationRepository.findById(1L)).thenReturn(Optional.of(org));
        lenient().when(subscriptionFeatureService.getEffectiveLimit(eq(1L), anyString(), eq(null))).thenReturn(null);
        lenient().when(userRepository.countNonDeleted()).thenReturn(0L);

        therapistRole = TestDataFactory.createTestRole(com.smart.therapy.flow.common.util.RoleName.THERAPIST);
        therapistRole.setId(1L);
        lenient().when(roleRepository.findByNameForOrganisation(anyString(), eq(1L))).thenReturn(Optional.of(therapistRole));
        lenient().when(authIdentityRoleRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        lenient().when(authIdentityService.createStaffIdentity(anyString(), anyString(), anyString(), any())).thenAnswer(inv ->
                TestDataFactory.createTestAuthIdentity(inv.getArgument(0), "encoded-password"));
        lenient().when(authIdentityService.createIdentity(anyString(), anyString(), any(), any())).thenAnswer(inv ->
                TestDataFactory.createTestAuthIdentity(inv.getArgument(0), "encoded-password"));
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    @DisplayName("Should create user successfully when admin")
    void shouldCreateUserSuccessfullyWhenAdmin() {
        // Arrange
        CreateUserRequest request = new CreateUserRequest();
        request.setUsername("newuser@example.com");
        request.setEmail("newuser@example.com");
        request.setFullName("New User");
        request.setPassword("password123");
        request.setActive(true);
        request.setRoles(Set.of("THERAPIST"));

        com.smart.therapy.flow.auth.entity.AuthIdentity authNew = TestDataFactory.createTestAuthIdentity("newuser@example.com", "encoded-password");
        authNew.setId(300L);
        User newUser = User.builder()
                .email("newuser@example.com")
                .fullName("New User")
                .authIdentity(authNew)
                .isActive(true)
                .build();
        newUser.setId(3L);

        when(userRepository.existsByEmail("newuser@example.com")).thenReturn(false);
        when(roleRepository.findByNameForOrganisation(RoleName.THERAPIST.name(), 1L)).thenReturn(Optional.of(therapistRole));
        when(userRepository.save(any(User.class))).thenReturn(newUser);
        when(authIdentityService.createStaffIdentity(anyString(), anyString(), anyString(), any())).thenReturn(authNew);
        when(userOrganisationRepository.existsByAuth_IdAndOrganisation_Id(300L, 1L)).thenReturn(false);
        when(userOrganisationRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        // Act
        UserResponse response = userService.createUser(request, adminPrincipal, "127.0.0.1");

        // Assert
        assertThat(response).isNotNull();
        assertThat(response.getUsername()).isEqualTo("newuser@example.com");
        verify(userRepository).existsByEmail("newuser@example.com");
        verify(userRepository).save(any(User.class));
        verify(userOrganisationRepository).save(any());
    }

    @Test
    @DisplayName("Should throw BadRequestException when email already exists")
    void shouldThrowExceptionWhenEmailAlreadyExists() {
        // Arrange
        CreateUserRequest request = new CreateUserRequest();
        request.setUsername("existing@example.com");
        request.setEmail("existing@example.com");

        when(userRepository.existsByEmail("existing@example.com")).thenReturn(true);

        // Act & Assert
        assertThatThrownBy(() -> userService.createUser(request, adminPrincipal, "127.0.0.1"))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Email already in use");

        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    @DisplayName("Should throw BadRequestException when email already exists")
    void shouldThrowExceptionWhenEmailExists() {
        // Arrange
        CreateUserRequest request = new CreateUserRequest();
        request.setUsername("newuser@example.com");
        request.setEmail("existing@example.com");

        when(userRepository.existsByEmail("existing@example.com")).thenReturn(true);

        // Act & Assert
        assertThatThrownBy(() -> userService.createUser(request, adminPrincipal, "127.0.0.1"))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Email already in use");

        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    @DisplayName("Should throw ForbiddenException when non-admin creates user")
    void shouldThrowExceptionWhenNonAdminCreatesUser() {
        // Arrange
        CreateUserRequest request = new CreateUserRequest();
        request.setUsername("newuser@example.com");
        request.setEmail("newuser@example.com");

        // Act & Assert
        assertThatThrownBy(() -> userService.createUser(request, therapistPrincipal, "127.0.0.1"))
                .isInstanceOf(ForbiddenException.class)
                .hasMessageContaining("Only administrators can create users");

        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    @DisplayName("Should get user by ID successfully")
    void shouldGetUserByIdSuccessfully() {
        // Arrange
        Long userId = 2L;
        when(userRepository.findById(userId)).thenReturn(Optional.of(therapist));

        // Act
        UserResponse response = userService.getUser(userId, adminPrincipal);

        // Assert
        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo(userId);
        verify(userRepository).findById(userId);
    }

    @Test
    @DisplayName("Should throw ResourceNotFoundException when user not found")
    void shouldThrowExceptionWhenUserNotFound() {
        // Arrange
        Long userId = 999L;
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> userService.getUser(userId, adminPrincipal))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("User not found");

        verify(userRepository).findById(userId);
    }

    @Test
    @DisplayName("Should update user successfully")
    void shouldUpdateUserSuccessfully() {
        // Arrange
        Long userId = 2L;
        UpdateUserRequest request = new UpdateUserRequest();
        request.setFullName("Updated Name");
        request.setActive(true);

        when(userRepository.findById(userId)).thenReturn(Optional.of(therapist));
        when(userRepository.save(any(User.class))).thenReturn(therapist);

        // Act
        UserResponse response = userService.updateUser(userId, request, adminPrincipal, "127.0.0.1");

        // Assert
        assertThat(response).isNotNull();
        verify(userRepository).findById(userId);
        verify(userRepository).save(any(User.class));
    }

    @Test
    @DisplayName("Should persist auth identity when admin updates username")
    void shouldPersistAuthIdentityWhenUsernameChanges() {
        Long userId = 2L;
        UpdateUserRequest request = new UpdateUserRequest();
        request.setUsername("new.admin.username");

        com.smart.therapy.flow.auth.entity.AuthIdentity identity =
                TestDataFactory.createTestAuthIdentity("admin@example.com", "hash");
        identity.setId(2L);
        therapist.setAuthIdentity(identity);

        when(userRepository.findById(userId)).thenReturn(Optional.of(therapist));
        when(userRepository.save(any(User.class))).thenReturn(therapist);
        when(authIdentityService.updateUsername(eq(identity), eq("new.admin.username")))
                .thenAnswer(inv -> {
                    AuthIdentity auth = inv.getArgument(0);
                    auth.setUsername("new.admin.username");
                    auth.setNormalisedUsername("new.admin.username");
                    auth.setLoginIdentifier("new.admin.username");
                    auth.setNormalisedLoginIdentifier("new.admin.username");
                    return auth;
                });

        UserResponse response = userService.updateUser(userId, request, adminPrincipal, "127.0.0.1");

        assertThat(response).isNotNull();
        assertThat(response.getUsername()).isEqualTo("new.admin.username");
        verify(authIdentityService).updateUsername(identity, "new.admin.username");
        verify(userRepository).save(any(User.class));
    }

    @Test
    @DisplayName("Should throw BadRequestException when updating to existing username")
    void shouldThrowExceptionWhenUpdatingToExistingUsername() {
        // Arrange
        Long userId = 2L;
        UpdateUserRequest request = new UpdateUserRequest();
        request.setUsername("existing@example.com");

        therapist.getAuthIdentity().setId(2L);

        when(userRepository.findById(userId)).thenReturn(Optional.of(therapist));
        when(authIdentityService.updateUsername(therapist.getAuthIdentity(), "existing@example.com"))
                .thenThrow(new BadRequestException("Username already in use"));

        // Act & Assert
        assertThatThrownBy(() -> userService.updateUser(userId, request, adminPrincipal, "127.0.0.1"))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Username already in use");

        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    @DisplayName("Should return paginated users list")
    void shouldReturnPaginatedUsersList() {
        // Arrange
        int page = 1;
        int pageSize = 10;
        Page<User> userPage = new PageImpl<>(List.of(therapist), PageRequest.of(0, 10), 1);

        when(userRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(userPage);

        // Act
        PaginatedResponse<UserResponse> response = userService.getUsers(
                page, pageSize, null, null, null, adminPrincipal
        );

        // Assert
        assertThat(response).isNotNull();
        assertThat(response.getItems()).hasSize(1);
        assertThat(response.getTotalCount()).isEqualTo(1);
        assertThat(response.getPage()).isEqualTo(page);
        assertThat(response.getPageSize()).isEqualTo(pageSize);
    }

    @Test
    @DisplayName("Should throw exception when request is null")
    void shouldThrowExceptionWhenRequestIsNull() {
        // Act & Assert
        assertThatThrownBy(() -> userService.createUser(null, adminPrincipal, "127.0.0.1"))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("Request payload is required");
    }

    @Test
    @DisplayName("Should throw exception when requester is null")
    void shouldThrowExceptionWhenRequesterIsNull() {
        // Arrange
        CreateUserRequest request = new CreateUserRequest();
        request.setUsername("newuser@example.com");
        request.setEmail("newuser@example.com");

        // Act & Assert
        assertThatThrownBy(() -> userService.createUser(request, null, "127.0.0.1"))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("Requester is required");
    }

    @Test
    @DisplayName("Should block self-update without admin or supervisor permission")
    void shouldBlockSelfUpdateWithoutPermission() {
        // Arrange
        Long userId = 2L;
        UpdateUserRequest request = new UpdateUserRequest();
        request.setFullName("My Updated Name");

        // Act & Assert
        assertThatThrownBy(() -> userService.updateUser(userId, request, therapistPrincipal, "127.0.0.1"))
                .isInstanceOf(ForbiddenException.class)
                .hasMessageContaining("Insufficient permissions to update users");
    }

    @Test
    @DisplayName("Should allow same-time shifts when modes are virtual and in-person")
    void shouldAllowSameTimeShiftsForDifferentModes() {
        List<UserProfileWorkingHours> shifts = List.of(
                workingHours("MONDAY", "09:00", "17:00", ShiftMode.VIRTUAL),
                workingHours("MONDAY", "09:00", "17:00", ShiftMode.IN_PERSON)
        );

        assertThatCode(() -> invokeValidateWorkingHoursNoOverlap(shifts))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("Should reject overlapping shifts with same mode")
    void shouldRejectOverlappingShiftsWithSameMode() {
        List<UserProfileWorkingHours> shifts = List.of(
                workingHours("MONDAY", "09:00", "17:00", ShiftMode.VIRTUAL),
                workingHours("MONDAY", "16:00", "18:00", ShiftMode.VIRTUAL)
        );

        assertThatThrownBy(() -> invokeValidateWorkingHoursNoOverlap(shifts))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("conflicts");
    }

    @Test
    @DisplayName("Should reject overlap when one shift mode is both")
    void shouldRejectOverlapWhenOneShiftModeIsBoth() {
        List<UserProfileWorkingHours> shifts = List.of(
                workingHours("MONDAY", "09:00", "17:00", ShiftMode.BOTH),
                workingHours("MONDAY", "10:00", "12:00", ShiftMode.IN_PERSON)
        );

        assertThatThrownBy(() -> invokeValidateWorkingHoursNoOverlap(shifts))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("conflicts");
    }

    private UserProfileWorkingHours workingHours(String day, String start, String end, ShiftMode mode) {
        return UserProfileWorkingHours.builder()
                .day(day)
                .startTime(LocalTime.parse(start))
                .endTime(LocalTime.parse(end))
                .sessionMode(mode)
                .build();
    }

    @SuppressWarnings("unchecked")
    private void invokeValidateWorkingHoursNoOverlap(List<UserProfileWorkingHours> shifts) {
        ReflectionTestUtils.invokeMethod(userService, "validateWorkingHoursNoOverlap", shifts);
    }
}
