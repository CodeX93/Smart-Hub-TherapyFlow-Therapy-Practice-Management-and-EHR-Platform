package com.smart.therapy.flow.client.service;

import com.smart.therapy.flow.client.entity.Client;
import com.smart.therapy.flow.client.entity.ClientEmployment;
import com.smart.therapy.flow.client.repository.ClientEmploymentRepository;
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

@Service
@Slf4j
@RequiredArgsConstructor
public class ClientEmploymentService {

    private final ClientEmploymentRepository employmentRepository;
    private final ClientRepository clientRepository;

    @Transactional(readOnly = true)
    public Optional<ClientEmployment> getEmployment(Long clientId) {
        return employmentRepository.findByClientId(clientId);
    }

    @Transactional
    public ClientEmployment createEmployment(Long clientId, ClientEmployment employment) {
        Objects.requireNonNull(clientId, "Client ID is required");
        Objects.requireNonNull(employment, "Employment is required");

        Client client = clientRepository.findById(clientId)
                .orElseThrow(() -> new ResourceNotFoundException("Client not found"));

        if (employmentRepository.findByClientId(clientId).isPresent()) {
            throw new BadRequestException("Employment information already exists for this client");
        }

        employment.setClient(client);
        return employmentRepository.save(employment);
    }

    @Transactional
    public ClientEmployment updateEmployment(Long clientId, ClientEmployment updatedEmployment) {
        ClientEmployment employment = employmentRepository.findByClientId(clientId)
                .orElseThrow(() -> new ResourceNotFoundException("Employment information not found for client"));

        // Update all fields
        employment.setEmploymentStatus(updatedEmployment.getEmploymentStatus());
        employment.setEmployerName(updatedEmployment.getEmployerName());
        employment.setJobTitle(updatedEmployment.getJobTitle());
        employment.setEmploymentStartDate(updatedEmployment.getEmploymentStartDate());
        employment.setEmploymentEndDate(updatedEmployment.getEmploymentEndDate());
        employment.setIsCurrentlyEmployed(updatedEmployment.getIsCurrentlyEmployed());
        employment.setEducationLevel(updatedEmployment.getEducationLevel());
        employment.setFieldOfStudy(updatedEmployment.getFieldOfStudy());
        employment.setSchoolName(updatedEmployment.getSchoolName());
        employment.setGraduationYear(updatedEmployment.getGraduationYear());
        employment.setIsStudent(updatedEmployment.getIsStudent());
        employment.setOccupationCategory(updatedEmployment.getOccupationCategory());
        employment.setWorkHoursPerWeek(updatedEmployment.getWorkHoursPerWeek());
        employment.setShiftWork(updatedEmployment.getShiftWork());
        employment.setRemoteWork(updatedEmployment.getRemoteWork());
        employment.setAnnualIncome(updatedEmployment.getAnnualIncome());
        employment.setHouseholdIncome(updatedEmployment.getHouseholdIncome());
        employment.setDependents(updatedEmployment.getDependents());
        employment.setHouseholdSize(updatedEmployment.getHouseholdSize());
        employment.setFinancialHardship(updatedEmployment.getFinancialHardship());
        employment.setEligibleForSlidingScale(updatedEmployment.getEligibleForSlidingScale());
        employment.setSlidingScalePercentage(updatedEmployment.getSlidingScalePercentage());
        employment.setDisabilityStatus(updatedEmployment.getDisabilityStatus());
        employment.setVeteranStatus(updatedEmployment.getVeteranStatus());
        employment.setMilitaryBranch(updatedEmployment.getMilitaryBranch());
        employment.setMilitaryServiceYears(updatedEmployment.getMilitaryServiceYears());
        employment.setNotes(updatedEmployment.getNotes());

        return employmentRepository.save(employment);
    }

    @Transactional
    public void deleteEmployment(Long clientId) {
        ClientEmployment employment = employmentRepository.findByClientId(clientId)
                .orElseThrow(() -> new ResourceNotFoundException("Employment information not found"));
        employmentRepository.delete(employment);
    }

    @Transactional(readOnly = true)
    public List<ClientEmployment> getClientsWithFinancialHardship() {
        return employmentRepository.findClientsWithFinancialHardship();
    }

    @Transactional(readOnly = true)
    public List<ClientEmployment> getEligibleForSlidingScale() {
        return employmentRepository.findEligibleForSlidingScale();
    }

    @Transactional(readOnly = true)
    public List<ClientEmployment> getVeterans() {
        return employmentRepository.findVeterans();
    }
}
