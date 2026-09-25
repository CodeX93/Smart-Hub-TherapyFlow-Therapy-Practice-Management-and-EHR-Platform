package com.smart.therapy.flow.unit.service;

import com.smart.therapy.flow.client.entity.ClientInsurance;
import com.smart.therapy.flow.client.repository.ClientInsuranceRepository;
import com.smart.therapy.flow.client.repository.ClientRepository;
import com.smart.therapy.flow.client.service.ClientInsuranceService;
import com.smart.therapy.flow.common.exception.BadRequestException;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ClientInsuranceServiceTest {

    @Mock
    private ClientInsuranceRepository insuranceRepository;

    @Mock
    private ClientRepository clientRepository;

    private ClientInsuranceService insuranceService;

    @BeforeEach
    void setUp() {
        Validator validator = Validation.buildDefaultValidatorFactory().getValidator();
        insuranceService = new ClientInsuranceService(insuranceRepository, clientRepository, validator);
    }

    @Test
    @DisplayName("Should reject invalid insurance phone before save")
    void shouldRejectInvalidInsurancePhone() {
        Long clientId = 10L;
        ClientInsurance existing = ClientInsurance.builder()
                .insuranceProvider("Blue Cross")
                .policyNumber("POL123")
                .insurancePhone("+1-555-123-4567")
                .build();

        when(insuranceRepository.findByClientId(clientId)).thenReturn(Optional.of(existing));

        ClientInsurance update = ClientInsurance.builder()
                .insurancePhone("dwdwdwdwd")
                .build();

        assertThatThrownBy(() -> insuranceService.updateInsurance(clientId, update))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Invalid insurance phone format");

        verify(insuranceRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should patch insurance fields without clearing existing values")
    void shouldPatchInsuranceFieldsWithoutClearingExistingValues() {
        Long clientId = 10L;
        ClientInsurance existing = ClientInsurance.builder()
                .insuranceProvider("Blue Cross")
                .policyNumber("POL123")
                .groupNumber("GRP1")
                .insurancePhone("+1-555-123-4567")
                .build();

        when(insuranceRepository.findByClientId(clientId)).thenReturn(Optional.of(existing));
        when(insuranceRepository.save(any(ClientInsurance.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ClientInsurance update = ClientInsurance.builder()
                .insurancePhone("+1-555-999-0000")
                .build();

        ClientInsurance saved = insuranceService.updateInsurance(clientId, update);

        assertThat(saved.getInsuranceProvider()).isEqualTo("Blue Cross");
        assertThat(saved.getPolicyNumber()).isEqualTo("POL123");
        assertThat(saved.getGroupNumber()).isEqualTo("GRP1");
        assertThat(saved.getInsurancePhone()).isEqualTo("+1-555-999-0000");
    }
}
