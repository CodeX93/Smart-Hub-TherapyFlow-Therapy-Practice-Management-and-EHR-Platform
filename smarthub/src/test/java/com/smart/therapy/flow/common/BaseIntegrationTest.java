package com.smart.therapy.flow.common;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.smart.therapy.flow.auth.dto.JwtAuthenticationResponse;
import com.smart.therapy.flow.auth.dto.LoginRequest;
import com.smart.therapy.flow.auth.entity.AuthIdentity;
import com.smart.therapy.flow.auth.entity.User;
import com.smart.therapy.flow.auth.repository.AuthIdentityRepository;
import com.smart.therapy.flow.auth.repository.UserRepository;
import com.smart.therapy.flow.client.repository.ClientRepository;
import com.smart.therapy.flow.common.TestDataFactory;
import com.smart.therapy.flow.organisation.entity.Organisation;
import com.smart.therapy.flow.organisation.repository.OrganisationRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Base class for integration tests.
 * Provides common setup and utilities for all integration tests.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@ActiveProfiles("test")
public abstract class BaseIntegrationTest {

    @Autowired
    protected MockMvc mockMvc;

    @Autowired
    protected ObjectMapper objectMapper;

    @Autowired
    protected ClientRepository clientRepository;

    @Autowired
    protected UserRepository userRepository;

    @Autowired
    protected AuthIdentityRepository authIdentityRepository;

    @Autowired
    protected OrganisationRepository organisationRepository;

    @Autowired
    protected PasswordEncoder passwordEncoder;

    /**
     * Get authentication token for default test therapist
     */
    protected String getAuthToken() {
        return getAuthToken("therapist@example.com", "password123");
    }

    /**
     * Get authentication token for specified user by calling the actual login endpoint.
     * This ensures tests use real JWT tokens and verify the authentication flow.
     */
    protected String getAuthToken(String email, String password) {
        try {
            // Ensure the user exists with the expected credentials before logging in
            User user = userRepository.findByEmail(email).orElse(null);
            if (user == null) {
                // Create user with AuthIdentity and Organisation (unified auth)
                Organisation org = organisationRepository.findAll().isEmpty()
                        ? organisationRepository.save(TestDataFactory.createTestOrganisation())
                        : organisationRepository.findAll().get(0);
                AuthIdentity auth = AuthIdentity.builder()
                        .loginIdentifier(email)
                        .normalisedLoginIdentifier(email.toLowerCase().trim())
                        .passwordHash(passwordEncoder.encode(password))
                        .identityType(com.smart.therapy.flow.auth.entity.IdentityType.STAFF)
                        .isActive(true)
                        .build();
                auth = authIdentityRepository.save(auth);
                user = email.toLowerCase().startsWith("admin") ? TestDataFactory.createTestAdmin() : TestDataFactory.createTestTherapist();
                user.setEmail(email);
                user.setFullName(user.getFullName());
                user.setAuthIdentity(auth);
                user = userRepository.save(user);
            } else if (user.getAuthIdentity() != null && !passwordEncoder.matches(password, user.getAuthIdentity().getPasswordHash())) {
                AuthIdentity auth = user.getAuthIdentity();
                auth.setPasswordHash(passwordEncoder.encode(password));
                authIdentityRepository.save(auth);
            }

            LoginRequest loginRequest = new LoginRequest();
            loginRequest.setUsername(email);
            loginRequest.setPassword(password);
            
            String response = mockMvc.perform(post("/api/v1/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(loginRequest)))
                    .andExpect(status().isOk())
                    .andReturn()
                    .getResponse()
                    .getContentAsString();
            
            JwtAuthenticationResponse authResponse = objectMapper.readValue(
                    response, JwtAuthenticationResponse.class);
            return authResponse.getAccessToken();
        } catch (Exception e) {
            throw new RuntimeException("Failed to get auth token for user: " + email, e);
        }
    }

    /**
     * Create HTTP headers with authentication token
     */
    protected HttpHeaders createHeaders(String token) {
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + token);
        headers.setContentType(MediaType.APPLICATION_JSON);
        return headers;
    }

    /**
     * Create HTTP headers without authentication (for public endpoints)
     */
    protected HttpHeaders createHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        return headers;
    }
}

