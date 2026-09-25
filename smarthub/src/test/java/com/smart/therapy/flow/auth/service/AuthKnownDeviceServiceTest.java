package com.smart.therapy.flow.auth.service;

import com.smart.therapy.flow.auth.dto.AuthDeviceDtos;
import com.smart.therapy.flow.auth.entity.AuthIdentity;
import com.smart.therapy.flow.auth.entity.AuthKnownDevice;
import com.smart.therapy.flow.auth.repository.AuthIdentityRepository;
import com.smart.therapy.flow.auth.repository.AuthKnownDeviceRepository;
import com.smart.therapy.flow.common.service.EmailService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthKnownDeviceServiceTest {

    @Mock AuthKnownDeviceRepository knownDeviceRepository;
    @Mock AuthIdentityRepository identityRepository;
    @Mock EmailService emailService;

    private AuthKnownDeviceService service;
    private AuthIdentity identity;

    @BeforeEach
    void setUp() {
        service = new AuthKnownDeviceService(knownDeviceRepository, identityRepository, emailService);
        ReflectionTestUtils.setField(service, "trustedDeviceEnabled", true);
        ReflectionTestUtils.setField(service, "trustDays", 30);
        ReflectionTestUtils.setField(service, "staySignedInRefreshDays", 30);
        identity = AuthIdentity.builder().id(7L).loginIdentifier("admin@example.com").isActive(true).build();
    }

    @Test
    void newDeviceSendsSecurityEmailAndPersistsFingerprint() {
        when(identityRepository.findById(7L)).thenReturn(Optional.of(identity));
        when(knownDeviceRepository.findByAuthIdentityIdAndFingerprintHash(eq(7L), anyString()))
                .thenReturn(Optional.empty());
        when(knownDeviceRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        String ua = "Mozilla/5.0 (Macintosh; Intel Mac OS X) Chrome/126.0.0.0 Safari/537.36";
        service.recordSuccessfulLogin(7L, "1.2.3.4", ua);

        ArgumentCaptor<AuthKnownDevice> captor = ArgumentCaptor.forClass(AuthKnownDevice.class);
        verify(knownDeviceRepository).save(captor.capture());
        assertThat(captor.getValue().getDeviceLabel()).contains("Chrome");
        verify(emailService).sendNewDeviceLoginAlert(
                eq("admin@example.com"),
                eq("admin@example.com"),
                anyString(),
                eq("1.2.3.4"),
                eq(ua),
                any(Instant.class));
    }

    @Test
    void knownDeviceUpdatesLastSeenWithoutEmail() {
        when(identityRepository.findById(7L)).thenReturn(Optional.of(identity));
        AuthKnownDevice existing = AuthKnownDevice.builder()
                .authIdentity(identity)
                .fingerprintHash("abc")
                .deviceLabel("Chrome on macOS")
                .firstSeenAt(Instant.now().minusSeconds(3600))
                .lastSeenAt(Instant.now().minusSeconds(3600))
                .trusted(false)
                .build();
        when(knownDeviceRepository.findByAuthIdentityIdAndFingerprintHash(eq(7L), anyString()))
                .thenReturn(Optional.of(existing));

        service.recordSuccessfulLogin(7L, "9.9.9.9", "Mozilla/5.0 Chrome/126 Safari/537.36");

        verify(emailService, never()).sendNewDeviceLoginAlert(
                anyString(), anyString(), anyString(), anyString(), anyString(), any());
        verify(knownDeviceRepository).save(existing);
        assertThat(existing.getLastIp()).isEqualTo("9.9.9.9");
    }

    @Test
    void trustCurrentDeviceIssuesOpaqueTokenAndMarksTrusted() {
        when(identityRepository.findById(7L)).thenReturn(Optional.of(identity));
        when(knownDeviceRepository.findByAuthIdentityIdAndFingerprintHash(eq(7L), anyString()))
                .thenReturn(Optional.empty());
        when(knownDeviceRepository.save(any())).thenAnswer(i -> {
            AuthKnownDevice d = i.getArgument(0);
            d.setId(99L);
            return d;
        });

        String token = service.trustCurrentDevice(7L, "1.2.3.4", "Mozilla/5.0 Chrome/126 Safari/537.36");

        assertThat(token).isNotBlank();
        ArgumentCaptor<AuthKnownDevice> captor = ArgumentCaptor.forClass(AuthKnownDevice.class);
        verify(knownDeviceRepository, atLeastOnce()).save(captor.capture());
        AuthKnownDevice saved = captor.getAllValues().get(captor.getAllValues().size() - 1);
        assertThat(saved.getTrusted()).isTrue();
        assertThat(saved.getTrustTokenHash()).isEqualTo(AuthKnownDeviceService.hashToken(token));
        assertThat(saved.getTrustExpiresAt()).isAfter(Instant.now());
    }

    @Test
    void isTrustedDeviceAcceptsValidToken() {
        String token = "abc123";
        AuthKnownDevice trusted = AuthKnownDevice.builder()
                .authIdentity(identity)
                .fingerprintHash("fp")
                .deviceLabel("Chrome on macOS")
                .firstSeenAt(Instant.now())
                .lastSeenAt(Instant.now())
                .trusted(true)
                .trustTokenHash(AuthKnownDeviceService.hashToken(token))
                .trustExpiresAt(Instant.now().plusSeconds(3600))
                .build();
        when(knownDeviceRepository.findByAuthIdentityIdAndTrustTokenHash(
                eq(7L), eq(AuthKnownDeviceService.hashToken(token))))
                .thenReturn(Optional.of(trusted));

        assertThat(service.isTrustedDevice(7L, token)).isTrue();
    }

    @Test
    void isTrustedDeviceRejectsExpiredToken() {
        String token = "abc123";
        AuthKnownDevice trusted = AuthKnownDevice.builder()
                .authIdentity(identity)
                .fingerprintHash("fp")
                .deviceLabel("Chrome on macOS")
                .firstSeenAt(Instant.now())
                .lastSeenAt(Instant.now())
                .trusted(true)
                .trustTokenHash(AuthKnownDeviceService.hashToken(token))
                .trustExpiresAt(Instant.now().minusSeconds(10))
                .build();
        when(knownDeviceRepository.findByAuthIdentityIdAndTrustTokenHash(eq(7L), anyString()))
                .thenReturn(Optional.of(trusted));

        assertThat(service.isTrustedDevice(7L, token)).isFalse();
    }

    @Test
    void listDevicesMarksTrustedAndCurrent() {
        String token = "tok";
        AuthKnownDevice trusted = AuthKnownDevice.builder()
                .id(1L)
                .authIdentity(identity)
                .fingerprintHash("fp")
                .deviceLabel("Chrome on macOS")
                .firstSeenAt(Instant.now())
                .lastSeenAt(Instant.now())
                .trusted(true)
                .trustTokenHash(AuthKnownDeviceService.hashToken(token))
                .trustExpiresAt(Instant.now().plusSeconds(3600))
                .build();
        when(knownDeviceRepository.findByAuthIdentityIdOrderByLastSeenAtDesc(7L))
                .thenReturn(List.of(trusted));

        AuthDeviceDtos.DeviceListResponse response = service.listDevices(7L, token, "ua");
        assertThat(response.trustedCount()).isEqualTo(1);
        assertThat(response.devices().get(0).trusted()).isTrue();
        assertThat(response.devices().get(0).current()).isTrue();
    }
}
