package com.smart.therapy.flow.unit.service;

import com.smart.therapy.flow.auth.entity.User;
import com.smart.therapy.flow.auth.repository.AuditLogRepository;
import com.smart.therapy.flow.auth.repository.UserRepository;
import com.smart.therapy.flow.common.TestDataFactory;
import com.smart.therapy.flow.common.exception.ForbiddenException;
import com.smart.therapy.flow.common.exception.ResourceNotFoundException;
import com.smart.therapy.flow.common.security.AuthPrincipal;
import com.smart.therapy.flow.common.security.CurrentUserService;
import com.smart.therapy.flow.document.dto.*;
import com.smart.therapy.flow.document.entity.LibraryCategory;
import com.smart.therapy.flow.document.entity.LibraryEntry;
import com.smart.therapy.flow.document.entity.LibraryEntryConnection;
import com.smart.therapy.flow.document.enums.ConnectionType;
import com.smart.therapy.flow.document.repository.LibraryCategoryRepository;
import com.smart.therapy.flow.document.repository.LibraryEntryConnectionRepository;
import com.smart.therapy.flow.document.repository.LibraryEntryRepository;
import com.smart.therapy.flow.common.security.PermissionChecker;
import com.smart.therapy.flow.document.service.LibraryService;
import com.smart.therapy.flow.document.service.LibraryTagService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("LibraryService Unit Tests")
class LibraryServiceTest {

    @Mock
    private LibraryEntryRepository libraryEntryRepository;

    @Mock
    private LibraryCategoryRepository libraryCategoryRepository;

    @Mock
    private LibraryEntryConnectionRepository connectionRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private AuditLogRepository auditLogRepository;

    @Mock
    private CurrentUserService currentUserService;

    @Mock
    private LibraryTagService libraryTagService;

    @Mock
    private PermissionChecker permissionChecker;

    @InjectMocks
    private LibraryService libraryService;

    private AuthPrincipal adminPrincipal;
    private AuthPrincipal therapistPrincipal;
    private User admin;
    private User therapist;
    private LibraryCategory category;
    private LibraryEntry entry;

    @BeforeEach
    void setUp() {
        admin = TestDataFactory.createTestAdmin();
        admin.setId(1L);
        adminPrincipal = TestDataFactory.createAuthPrincipal(admin);

        therapist = TestDataFactory.createTestTherapist();
        therapist.setId(2L);
        therapistPrincipal = TestDataFactory.createAuthPrincipal(therapist);
        when(currentUserService.requireCurrentUser(any(AuthPrincipal.class))).thenAnswer(inv -> {
            AuthPrincipal ap = inv.getArgument(0);
            if (ap.getAuthId().equals(admin.getAuthIdentity().getId())) return admin;
            return therapist;
        });
        when(permissionChecker.hasRole(any(AuthPrincipal.class), eq("ADMIN"))).thenAnswer(inv -> {
            AuthPrincipal ap = inv.getArgument(0);
            return ap.getAuthId().equals(admin.getAuthIdentity().getId());
        });
        when(permissionChecker.hasRole(any(AuthPrincipal.class), eq("SUPERVISOR"))).thenReturn(false);
        when(userRepository.findById(admin.getId())).thenReturn(Optional.of(admin));

        category = LibraryCategory.builder()
                .name("Treatment Plans")
                .description("Treatment planning resources")
                .isActive(true)
                .build();

        category.setId(1L);

        entry = LibraryEntry.builder()
                .title("CBT Techniques")
                .content("Cognitive Behavioral Therapy techniques")
                .category(category)
                .isActive(true)
                .build();
        entry.setId(1L);
    }

    @Test
    @DisplayName("Should get all library entries")
    void shouldGetAllLibraryEntries() {
        // Arrange
        when(libraryEntryRepository.findByCategoryId(1L)).thenReturn(List.of(entry));

        // Act
        List<LibraryEntryResponse> entries = libraryService.getEntries(1L);

        // Assert
        assertThat(entries).isNotNull();
        assertThat(entries).hasSize(1);
        assertThat(entries.get(0).getTitle()).isEqualTo("CBT Techniques");
        verify(libraryEntryRepository).findByCategoryId(1L);
    }

    @Test
    @DisplayName("Should get library entry by ID successfully")
    void shouldGetLibraryEntryByIdSuccessfully() {
        // Arrange
        Long entryId = 1L;
        when(libraryEntryRepository.findById(entryId)).thenReturn(Optional.of(entry));

        // Act
        LibraryEntryResponse response = libraryService.getEntry(entryId);

        // Assert
        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo(entryId);
        assertThat(response.getTitle()).isEqualTo("CBT Techniques");
        verify(libraryEntryRepository).findById(entryId);
    }

    @Test
    @DisplayName("Should throw ResourceNotFoundException when entry not found")
    void shouldThrowExceptionWhenEntryNotFound() {
        // Arrange
        Long entryId = 999L;
        when(libraryEntryRepository.findById(entryId)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> libraryService.getEntry(entryId))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Library entry not found");

        verify(libraryEntryRepository).findById(entryId);
    }

    @Test
    @DisplayName("Should create library entry successfully")
    void shouldCreateLibraryEntrySuccessfully() {
        // Arrange
        CreateLibraryEntryRequest request = new CreateLibraryEntryRequest();
        request.setTitle("New Entry");
        request.setContent("Entry content");
        request.setCategoryId(1L);

        LibraryEntry newEntry = LibraryEntry.builder()
                .title("New Entry")
                .content("Entry content")
                .category(category)
                .isActive(true)
                .build();

        when(libraryCategoryRepository.findById(1L)).thenReturn(Optional.of(category));
        when(libraryEntryRepository.findAll()).thenReturn(List.of());
        when(libraryEntryRepository.save(any(LibraryEntry.class))).thenReturn(newEntry);
        when(auditLogRepository.save(any())).thenReturn(null);

        // Act
        LibraryEntryResponse response = libraryService.createEntry(request, adminPrincipal);

        // Assert
        assertThat(response).isNotNull();
        assertThat(response.getTitle()).isEqualTo("New Entry");
        verify(libraryCategoryRepository).findById(1L);
        verify(libraryEntryRepository).save(any(LibraryEntry.class));
    }

    @Test
    @DisplayName("Should throw ResourceNotFoundException when category not found")
    void shouldThrowExceptionWhenCategoryNotFound() {
        // Arrange
        CreateLibraryEntryRequest request = new CreateLibraryEntryRequest();
        request.setTitle("New Entry");
        request.setCategoryId(999L);

        when(libraryCategoryRepository.findById(999L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> libraryService.createEntry(request, adminPrincipal))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Library category not found");

        verify(libraryEntryRepository, never()).save(any(LibraryEntry.class));
    }

    @Test
    @DisplayName("Should create connection between entries successfully")
    void shouldCreateConnectionBetweenEntriesSuccessfully() {
        // Arrange
        LibraryEntry entry2 = LibraryEntry.builder()
                .title("Related Entry")
                .isActive(true)
                .build();

        entry2.setId(2L);

        LibraryConnectionRequest request = new LibraryConnectionRequest();
        request.setToEntryId(2L);
        request.setConnectionType(ConnectionType.RELATED);
        request.setStrength(5);

        LibraryEntryConnection connection = LibraryEntryConnection.builder()
                .fromEntry(entry)
                .toEntry(entry2)
                .connectionType(ConnectionType.RELATED)
                .strength(5)
                .isActive(true)
                .build();

        when(libraryEntryRepository.findById(1L)).thenReturn(Optional.of(entry));
        when(libraryEntryRepository.findById(2L)).thenReturn(Optional.of(entry2));
        when(connectionRepository.save(any(LibraryEntryConnection.class))).thenReturn(connection);
        when(auditLogRepository.save(any())).thenReturn(null);

        // Act
        request.setFromEntryId(1L);
        LibraryConnectionResponse response = libraryService.createConnection(request, adminPrincipal);

        // Assert
        assertThat(response).isNotNull();
        verify(libraryEntryRepository).findById(1L);
        verify(libraryEntryRepository).findById(2L);
        verify(connectionRepository).save(any(LibraryEntryConnection.class));
    }

    @Test
    @DisplayName("Should throw ResourceNotFoundException when from entry not found")
    void shouldThrowExceptionWhenFromEntryNotFound() {
        // Arrange
        LibraryConnectionRequest request = new LibraryConnectionRequest();
        request.setFromEntryId(999L);
        request.setToEntryId(2L);

        when(libraryEntryRepository.findById(999L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> libraryService.createConnection(request, adminPrincipal))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Library entry not found");

        verify(connectionRepository, never()).save(any(LibraryEntryConnection.class));
    }

    @Test
    @DisplayName("Should get all categories")
    void shouldGetAllCategories() {
        // Arrange
        when(libraryCategoryRepository.findAll()).thenReturn(List.of(category));

        // Act
        List<LibraryCategoryResponse> categories = libraryService.getCategories();

        // Assert
        assertThat(categories).isNotNull();
        assertThat(categories).hasSize(1);
        assertThat(categories.get(0).getName()).isEqualTo("Treatment Plans");
        verify(libraryCategoryRepository).findAll();
    }

    @Test
    @DisplayName("Should bulk import entries in category tab mode")
    void shouldBulkImportEntriesInCategoryTabMode() {
        category.setId(2L);
        LibraryEntryBulkRequest request = new LibraryEntryBulkRequest();
        request.setCategoryId(2L);
        LibraryEntryBulkRequest.LibraryEntryBulkItem item = new LibraryEntryBulkRequest.LibraryEntryBulkItem();
        item.setTitle("ANXS10");
        item.setContent("Client reports excessive worry");
        request.setEntries(List.of(item));

        when(libraryCategoryRepository.findById(2L)).thenReturn(Optional.of(category));
        when(libraryEntryRepository.findAll()).thenReturn(List.of());
        when(libraryCategoryRepository.findAll()).thenReturn(List.of(category));
        when(userRepository.findById(admin.getId())).thenReturn(Optional.of(admin));
        when(libraryEntryRepository.save(any(LibraryEntry.class))).thenAnswer(inv -> inv.getArgument(0));

        LibraryBulkImportResponse response = libraryService.bulkCreateEntries(request, adminPrincipal);

        assertThat(response.getSuccessful()).isEqualTo(1);
        assertThat(response.getSkipped()).isZero();
        assertThat(response.getFailed()).isZero();
        verify(libraryEntryRepository).save(any(LibraryEntry.class));
    }

    @Test
    @DisplayName("Should skip duplicate titles during bulk import")
    void shouldSkipDuplicateTitlesDuringBulkImport() {
        category.setId(2L);
        entry.setTitle("ANXS10");

        LibraryEntryBulkRequest request = new LibraryEntryBulkRequest();
        request.setCategoryId(2L);
        LibraryEntryBulkRequest.LibraryEntryBulkItem item = new LibraryEntryBulkRequest.LibraryEntryBulkItem();
        item.setTitle("anxs10");
        item.setContent("Duplicate title");
        request.setEntries(List.of(item));

        when(libraryCategoryRepository.findById(2L)).thenReturn(Optional.of(category));
        when(libraryEntryRepository.findAll()).thenReturn(List.of(entry));
        when(libraryCategoryRepository.findAll()).thenReturn(List.of(category));

        LibraryBulkImportResponse response = libraryService.bulkCreateEntries(request, adminPrincipal);

        assertThat(response.getSuccessful()).isZero();
        assertThat(response.getSkipped()).isEqualTo(1);
        verify(libraryEntryRepository, never()).save(any(LibraryEntry.class));
    }

    @Test
    @DisplayName("Should create categories and connections in domain bulk import")
    void shouldCreateCategoriesAndConnectionsInDomainBulkImport() {
        category.setId(1L);
        LibraryEntryBulkRequest request = new LibraryEntryBulkRequest();
        request.setCategoryId(1L);
        LibraryEntryBulkRequest.LibraryEntryBulkItem item = new LibraryEntryBulkRequest.LibraryEntryBulkItem();
        item.setDomain("Anxiety");
        item.setSubdomain("Cognitive");
        item.setTitle("Cognitive Restructuring");
        item.setContent("A technique to identify and challenge negative thoughts");
        request.setEntries(List.of(item));

        LibraryCategory domainCategory = LibraryCategory.builder().name("Anxiety").isActive(true).build();
        domainCategory.setId(10L);
        LibraryCategory subdomainCategory = LibraryCategory.builder()
                .name("Cognitive")
                .parentCategory(domainCategory)
                .isActive(true)
                .build();
        subdomainCategory.setId(11L);

        when(libraryCategoryRepository.findById(1L)).thenReturn(Optional.of(category));
        when(libraryEntryRepository.findAll()).thenReturn(List.of());
        when(libraryCategoryRepository.findAll()).thenReturn(List.of());
        when(libraryCategoryRepository.save(any(LibraryCategory.class)))
                .thenReturn(domainCategory)
                .thenReturn(subdomainCategory);
        when(libraryEntryRepository.findByCategoryId(11L)).thenReturn(List.of());
        when(libraryEntryRepository.save(any(LibraryEntry.class))).thenAnswer(inv -> {
            LibraryEntry entry = inv.getArgument(0);
            if (entry.getId() == null) {
                entry.setId(entry.getTitle().equals("Cognitive") ? 99L : 100L);
            }
            return entry;
        });
        when(connectionRepository.save(any(LibraryEntryConnection.class))).thenAnswer(inv -> inv.getArgument(0));

        LibraryBulkImportResponse response = libraryService.bulkCreateEntries(request, adminPrincipal);

        assertThat(response.getSuccessful()).isEqualTo(1);
        assertThat(response.getCategoriesCreated()).isEqualTo(2);
        assertThat(response.getConnectionsCreated()).isEqualTo(1);
        verify(connectionRepository).save(any(LibraryEntryConnection.class));
    }
}

