package com.smart.therapy.flow.client.service;

import com.smart.therapy.flow.client.entity.Client;
import com.smart.therapy.flow.client.entity.ClientAddress;
import com.smart.therapy.flow.client.enums.AddressType;
import com.smart.therapy.flow.client.repository.ClientAddressRepository;
import com.smart.therapy.flow.client.repository.ClientRepository;
import com.smart.therapy.flow.common.exception.BadRequestException;
import com.smart.therapy.flow.common.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;

@Service
@Slf4j
@RequiredArgsConstructor
public class ClientAddressService {

    private final ClientAddressRepository addressRepository;
    private final ClientRepository clientRepository;

    @Transactional(readOnly = true)
    public List<ClientAddress> getAddresses(Long clientId) {
        return addressRepository.findByClientId(clientId);
    }

    @Transactional(readOnly = true)
    public List<ClientAddress> getAddressesByType(Long clientId, AddressType addressType) {
        return addressRepository.findByClientIdAndAddressType(clientId, addressType);
    }

    @Transactional(readOnly = true)
    public Optional<ClientAddress> getPrimaryAddress(Long clientId) {
        return addressRepository.findPrimaryByClientId(clientId);
    }

    @Transactional(readOnly = true)
    public List<ClientAddress> getCurrentAddresses(Long clientId) {
        return addressRepository.findCurrentByClientId(clientId);
    }

    @Transactional
    public ClientAddress createAddress(Long clientId, ClientAddress address) {
        Objects.requireNonNull(clientId, "Client ID is required");
        Objects.requireNonNull(address, "Address is required");

        Client client = clientRepository.findById(clientId)
                .orElseThrow(() -> new ResourceNotFoundException("Client not found"));

        if (address.getAddressType() == null) {
            throw new BadRequestException("Address type is required");
        }

        address.setClient(client);

        if (Boolean.TRUE.equals(address.getIsPrimary())) {
            unsetPrimaryAddresses(clientId, null);
        }

        return addressRepository.save(address);
    }

    /**
     * Full replace of address fields. Caller must supply a complete address snapshot.
     */
    @Transactional
    public ClientAddress updateAddress(Long addressId, ClientAddress updatedAddress) {
        ClientAddress address = addressRepository.findById(addressId)
                .orElseThrow(() -> new ResourceNotFoundException("Address not found"));

        address.setAddressType(updatedAddress.getAddressType());
        address.setStreetAddress1(updatedAddress.getStreetAddress1());
        address.setStreetAddress2(updatedAddress.getStreetAddress2());
        address.setCity(updatedAddress.getCity());
        address.setStateProvince(updatedAddress.getStateProvince());
        address.setPostalCode(updatedAddress.getPostalCode());
        address.setCountry(updatedAddress.getCountry());
        address.setIsPrimary(updatedAddress.getIsPrimary());
        address.setIsCurrent(updatedAddress.getIsCurrent());
        address.setIsVerified(updatedAddress.getIsVerified());
        address.setValidFrom(updatedAddress.getValidFrom());
        address.setValidUntil(updatedAddress.getValidUntil());
        address.setNotes(updatedAddress.getNotes());
        address.setDisplayOrder(updatedAddress.getDisplayOrder());
        address.setAddressLegacy(updatedAddress.getAddressLegacy());
        address.setStateLegacy(updatedAddress.getStateLegacy());
        address.setZipCodeLegacy(updatedAddress.getZipCodeLegacy());

        return saveWithPrimaryHandling(address);
    }

    /**
     * Applies only the fields set by the patch consumer; omitted fields remain unchanged.
     */
    @Transactional
    public ClientAddress patchAddress(Long addressId, Consumer<ClientAddress> patch) {
        Objects.requireNonNull(patch, "Patch is required");
        ClientAddress address = addressRepository.findById(addressId)
                .orElseThrow(() -> new ResourceNotFoundException("Address not found"));
        patch.accept(address);
        return saveWithPrimaryHandling(address);
    }

    @Transactional
    public void deleteAddress(Long addressId) {
        ClientAddress address = addressRepository.findById(addressId)
                .orElseThrow(() -> new ResourceNotFoundException("Address not found"));
        addressRepository.delete(address);
    }

    @Transactional
    public ClientAddress setAsPrimary(Long addressId) {
        ClientAddress address = addressRepository.findById(addressId)
                .orElseThrow(() -> new ResourceNotFoundException("Address not found"));

        unsetPrimaryAddresses(address.getClient().getId(), addressId);
        address.setIsPrimary(true);
        return addressRepository.save(address);
    }

    private ClientAddress saveWithPrimaryHandling(ClientAddress address) {
        if (Boolean.TRUE.equals(address.getIsPrimary())) {
            unsetPrimaryAddresses(address.getClient().getId(), address.getId());
        }
        return addressRepository.save(address);
    }

    /**
     * Clears primary flag on other addresses for the client.
     *
     * @param exceptAddressId address to keep untouched (the one being saved as primary)
     */
    private void unsetPrimaryAddresses(Long clientId, Long exceptAddressId) {
        addressRepository.findByClientId(clientId).stream()
                .filter(address -> Boolean.TRUE.equals(address.getIsPrimary()))
                .filter(address -> exceptAddressId == null || !Objects.equals(address.getId(), exceptAddressId))
                .forEach(address -> {
                    address.setIsPrimary(false);
                    addressRepository.save(address);
                });
    }

    @Transactional(readOnly = true)
    public Optional<ClientAddress> getAddress(Long addressId) {
        return addressRepository.findById(addressId);
    }
}
