package com.smart.therapy.flow.booking.service;

import com.smart.therapy.flow.booking.dto.BookingRequestDto;
import com.smart.therapy.flow.booking.dto.CreateBookingRequestDto;
import com.smart.therapy.flow.booking.entity.BookingRequest;
import com.smart.therapy.flow.booking.entity.BookingRequestStatus;
import com.smart.therapy.flow.booking.repository.BookingRequestRepository;
import com.smart.therapy.flow.common.exception.ResourceNotFoundException;
import com.smart.therapy.flow.common.service.EmailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class BookingRequestService {

    private final BookingRequestRepository repository;
    private final EmailService emailService;

    @Transactional
    public BookingRequestDto createRequest(CreateBookingRequestDto dto) {
        if (repository.existsByEmailAndStatusIn(dto.getEmail(), java.util.List.of(
                BookingRequestStatus.PENDING, 
                BookingRequestStatus.REVIEWED, 
                BookingRequestStatus.CONVERTED))) {
            throw new com.smart.therapy.flow.common.exception.ConflictException("A booking request with this email is already pending or has been accepted.");
        }

        BookingRequest request = BookingRequest.builder()
                .firstName(dto.getFirstName())
                .lastName(dto.getLastName())
                .email(dto.getEmail())
                .phone(dto.getPhone())
                .practiceName(dto.getPracticeName())
                .servicesOffered(dto.getServicesOffered())
                .practiceSize(dto.getPracticeSize())
                .country(dto.getCountry())
                .address(dto.getAddress())
                .agreeToTerms(dto.getAgreeToTerms())
                .status(BookingRequestStatus.PENDING)
                .build();

        BookingRequest saved = repository.save(request);

        // Send email to the lead
        try {
            emailService.sendBookingRequestConfirmationEmail(saved.getEmail(), saved.getFirstName());
        } catch (Exception e) {
            log.error("Failed to send booking request confirmation email: bookingRequestId={}", saved.getId(), e);
        }

        return mapToDto(saved);
    }

    @Transactional(readOnly = true)
    public Page<BookingRequestDto> getAllRequests(Pageable pageable) {
        return repository.findAll(pageable).map(this::mapToDto);
    }
    
    @Transactional(readOnly = true)
    public BookingRequestDto getRequestById(Long id) {
        return repository.findById(id)
                .map(this::mapToDto)
                .orElseThrow(() -> new ResourceNotFoundException("Booking request not found"));
    }

    @Transactional
    public BookingRequestDto updateStatus(Long id, BookingRequestStatus status, String statusMessage) {
        BookingRequest request = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Booking request not found"));
        request.setStatus(status);
        if (statusMessage != null && !statusMessage.isBlank()) {
            request.setStatusMessage(statusMessage);
        }
        
        BookingRequest saved = repository.save(request);
        
        try {
            if (status == BookingRequestStatus.CONVERTED) {
                emailService.sendBookingRequestAcceptedEmail(saved.getEmail(), saved.getFirstName(), saved.getStatusMessage());
            } else if (status == BookingRequestStatus.REJECTED) {
                emailService.sendBookingRequestRejectedEmail(saved.getEmail(), saved.getFirstName(), saved.getStatusMessage());
            }
        } catch (Exception e) {
            log.error("Failed to send status update email: bookingRequestId={}", saved.getId(), e);
        }

        return mapToDto(saved);
    }

    @Transactional
    public void deleteRequest(Long id) {
        if (!repository.existsById(id)) {
            throw new ResourceNotFoundException("Booking request not found");
        }
        repository.deleteById(id);
    }

    private BookingRequestDto mapToDto(BookingRequest entity) {
        return BookingRequestDto.builder()
                .id(entity.getId())
                .firstName(entity.getFirstName())
                .lastName(entity.getLastName())
                .email(entity.getEmail())
                .phone(entity.getPhone())
                .practiceName(entity.getPracticeName())
                .servicesOffered(entity.getServicesOffered())
                .practiceSize(entity.getPracticeSize())
                .country(entity.getCountry())
                .address(entity.getAddress())
                .status(entity.getStatus())
                .statusMessage(entity.getStatusMessage())
                .createdAt(entity.getCreatedAt())
                .build();
    }
}
