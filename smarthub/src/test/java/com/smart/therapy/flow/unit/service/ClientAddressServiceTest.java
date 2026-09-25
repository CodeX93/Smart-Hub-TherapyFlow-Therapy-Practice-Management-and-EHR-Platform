package com.smart.therapy.flow.unit.service;

import com.smart.therapy.flow.client.entity.Client;
import com.smart.therapy.flow.client.entity.ClientAddress;
import com.smart.therapy.flow.client.enums.AddressType;
import com.smart.therapy.flow.client.repository.ClientAddressRepository;
import com.smart.therapy.flow.client.repository.ClientRepository;
import com.smart.therapy.flow.client.service.ClientAddressService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("ClientAddressService Unit Tests")
class ClientAddressServiceTest {

    @Mock
    private ClientAddressRepository addressRepository;

    @Mock
    private ClientRepository clientRepository;

    @InjectMocks
    private ClientAddressService addressService;

    private Client client;
    private ClientAddress primaryAddress;

    @BeforeEach
    void setUp() {
        client = Client.builder().fullName("Test Client").build();
        client.setId(10L);
        primaryAddress = ClientAddress.builder()
                .client(client)
                .addressType(AddressType.HOME)
                .streetAddress1("123 Main St")
                .streetAddress2("Apt 1")
                .city("Toronto")
                .stateProvince("ON")
                .postalCode("M5H 2N2")
                .country("Canada")
                .isPrimary(true)
                .isCurrent(true)
                .build();
        primaryAddress.setId(99L);
    }

    @Test
    @DisplayName("patchAddress should update only provided fields and keep isPrimary true")
    void patchAddressShouldPreserveOtherFieldsAndPrimaryFlag() {
        when(addressRepository.findById(99L)).thenReturn(Optional.of(primaryAddress));
        when(addressRepository.findByClientId(10L)).thenReturn(List.of(primaryAddress));
        when(addressRepository.save(any(ClientAddress.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ClientAddress updated = addressService.patchAddress(99L, address ->
                address.setStreetAddress1("House no 631 street 39"));

        assertThat(updated.getStreetAddress1()).isEqualTo("House no 631 street 39");
        assertThat(updated.getStreetAddress2()).isEqualTo("Apt 1");
        assertThat(updated.getCity()).isEqualTo("Toronto");
        assertThat(updated.getStateProvince()).isEqualTo("ON");
        assertThat(updated.getPostalCode()).isEqualTo("M5H 2N2");
        assertThat(updated.getCountry()).isEqualTo("Canada");
        assertThat(updated.getIsPrimary()).isTrue();

        ArgumentCaptor<ClientAddress> captor = ArgumentCaptor.forClass(ClientAddress.class);
        verify(addressRepository).save(captor.capture());
        assertThat(captor.getValue().getIsPrimary()).isTrue();
    }

    @Test
    @DisplayName("updateAddress should not clear isPrimary on the same address row")
    void updateAddressShouldKeepPrimaryFlagOnSameRow() {
        when(addressRepository.findById(99L)).thenReturn(Optional.of(primaryAddress));
        when(addressRepository.findByClientId(10L)).thenReturn(List.of(primaryAddress));
        when(addressRepository.save(any(ClientAddress.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ClientAddress payload = ClientAddress.builder()
                .addressType(AddressType.HOME)
                .streetAddress1("Updated line 1")
                .streetAddress2("Apt 1")
                .city("Toronto")
                .stateProvince("ON")
                .postalCode("M5H 2N2")
                .country("Canada")
                .isPrimary(true)
                .isCurrent(true)
                .build();

        ClientAddress updated = addressService.updateAddress(99L, payload);

        assertThat(updated.getStreetAddress1()).isEqualTo("Updated line 1");
        assertThat(updated.getIsPrimary()).isTrue();
        verify(addressRepository).findByClientId(eq(10L));
    }
}
