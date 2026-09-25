package com.smart.therapy.flow.unit.organisation;

import com.smart.therapy.flow.auth.entity.User;
import com.smart.therapy.flow.auth.repository.UserRepository;
import com.smart.therapy.flow.common.tenant.TenantTransactionExecutor;
import com.smart.therapy.flow.document.entity.LibraryCategory;
import com.smart.therapy.flow.document.entity.LibraryEntry;
import com.smart.therapy.flow.document.repository.LibraryCategoryRepository;
import com.smart.therapy.flow.document.repository.LibraryEntryConnectionRepository;
import com.smart.therapy.flow.document.repository.LibraryEntryRepository;
import com.smart.therapy.flow.document.service.LibrarySeedDefinitions;
import com.smart.therapy.flow.organisation.service.TenantLibrarySeedService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("TenantLibrarySeedService")
class TenantLibrarySeedServiceTest {

    @Mock
    private TenantTransactionExecutor tenantTransactionExecutor;

    @Mock
    private LibraryCategoryRepository categoryRepository;

    @Mock
    private LibraryEntryRepository entryRepository;

    @Mock
    private LibraryEntryConnectionRepository connectionRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private TenantLibrarySeedService seedService;

    @Test
    @DisplayName("seeds categories, entries, and connections when library is empty after V45 wipe")
    void seedsAllLibraryDataWhenEmpty() {
        when(tenantTransactionExecutor.executeWrite(eq(5L), eq("tenant_test"), any()))
                .thenAnswer(invocation -> {
                    Supplier<?> supplier = invocation.getArgument(2);
                    return supplier.get();
                });
        when(categoryRepository.count()).thenReturn(0L);
        when(entryRepository.count()).thenReturn(0L);
        when(categoryRepository.save(any(LibraryCategory.class))).thenAnswer(invocation -> {
            LibraryCategory category = invocation.getArgument(0);
            category.setId((long) (category.getName().hashCode() & 0xFF));
            return category;
        });
        when(categoryRepository.findById(any())).thenAnswer(invocation -> {
            Long id = invocation.getArgument(0);
            LibraryCategory category = LibraryCategory.builder().name("seed").build();
            category.setId(id);
            return Optional.of(category);
        });
        when(entryRepository.save(any(LibraryEntry.class))).thenAnswer(invocation -> {
            LibraryEntry entry = invocation.getArgument(0);
            entry.setId(100L + Math.abs(entry.getTitle().hashCode() % 1000));
            return entry;
        });
        when(entryRepository.findById(any())).thenAnswer(invocation -> {
            Long id = invocation.getArgument(0);
            LibraryEntry entry = LibraryEntry.builder().title("entry").build();
            entry.setId(id);
            return Optional.of(entry);
        });
        when(connectionRepository.existsByFromEntry_IdAndToEntry_Id(any(), any())).thenReturn(false);
        User seedUser = User.builder().build();
        seedUser.setId(1L);
        when(userRepository.findAll()).thenReturn(List.of(seedUser));

        int created = seedService.seedDefaults(5L, "tenant_test");

        int expectedCategories = LibrarySeedDefinitions.categories().size();
        int expectedEntries = LibrarySeedDefinitions.entries().size();
        int expectedConnections = LibrarySeedDefinitions.connections().size();

        assertThat(created).isEqualTo(expectedCategories + expectedEntries + expectedConnections);
        verify(categoryRepository, atLeastOnce()).save(any(LibraryCategory.class));
        verify(entryRepository, atLeastOnce()).save(any(LibraryEntry.class));
        verify(connectionRepository, atLeastOnce()).save(any());
    }

    @Test
    @DisplayName("skips when categories and entries already exist")
    void skipsWhenAlreadySeeded() {
        when(tenantTransactionExecutor.executeWrite(eq(5L), eq("tenant_test"), any()))
                .thenAnswer(invocation -> {
                    Supplier<?> supplier = invocation.getArgument(2);
                    return supplier.get();
                });
        when(categoryRepository.count()).thenReturn(7L);
        when(entryRepository.count()).thenReturn(100L);

        assertThat(seedService.seedDefaults(5L, "tenant_test")).isZero();
        verify(categoryRepository, never()).save(any());
    }

    @Test
    @DisplayName("skips invalid schema inputs")
    void skipsInvalidSchemaInputs() {
        assertThat(seedService.seedDefaults(null, "tenant_test")).isZero();
        assertThat(seedService.seedDefaults(1L, null)).isZero();
        assertThat(seedService.seedDefaults(1L, "public")).isZero();
    }
}
