package com.smart.therapy.flow.unit.service;

import com.smart.therapy.flow.common.TestDataFactory;
import com.smart.therapy.flow.common.exception.BadRequestException;
import com.smart.therapy.flow.common.exception.ForbiddenException;
import com.smart.therapy.flow.common.exception.ResourceNotFoundException;
import com.smart.therapy.flow.common.security.AuthPrincipal;
import com.smart.therapy.flow.common.security.PermissionChecker;
import com.smart.therapy.flow.system.dto.CreateOptionCategoryRequest;
import com.smart.therapy.flow.system.dto.CreateSystemOptionRequest;
import com.smart.therapy.flow.system.dto.OptionCategoryResponse;
import com.smart.therapy.flow.system.dto.SystemOptionResponse;
import com.smart.therapy.flow.system.dto.UpdateOptionCategoryRequest;
import com.smart.therapy.flow.system.entity.OptionCategory;
import com.smart.therapy.flow.system.entity.SystemOption;
import com.smart.therapy.flow.system.repository.OptionCategoryRepository;
import com.smart.therapy.flow.system.repository.SystemOptionRepository;
import com.smart.therapy.flow.system.service.SystemOptionService;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("SystemOptionService Unit Tests")
class SystemOptionServiceTest {

    @Mock
    private OptionCategoryRepository categoryRepository;

    @Mock
    private SystemOptionRepository optionRepository;

    @Spy
    private PermissionChecker permissionChecker = new PermissionChecker();

    @InjectMocks
    private SystemOptionService systemOptionService;

    private AuthPrincipal adminPrincipal;
    private AuthPrincipal therapistPrincipal;
    private OptionCategory category;
    private SystemOption option;

    @BeforeEach
    void setUp() {
        var admin = TestDataFactory.createTestAdmin();
        var therapist = TestDataFactory.createTestTherapist();
        adminPrincipal = TestDataFactory.createAuthPrincipal(admin, "ROLE_ADMIN", "CONSENT_ADMIN_VIEW");
        therapistPrincipal = TestDataFactory.createAuthPrincipal(therapist);

        category = OptionCategory.builder()
                .categoryKey("test_category")
                .categoryName("Test Category")
                .description("Test category description")
                .isActive(true)
                .isSystem(false)
                .build();

        category.setId(1L);

        option = SystemOption.builder()
                .category(category)
                .optionKey("test_option")
                .optionLabel("Test Option")
                .isActive(true)
                .build();
    }

    @Test
    @DisplayName("Should get all active categories")
    void shouldGetAllActiveCategories() {
        // Arrange
        when(categoryRepository.findByIsActive(true)).thenReturn(List.of(category));

        // Act
        List<OptionCategoryResponse> categories = systemOptionService.getCategories(false);

        // Assert
        assertThat(categories).isNotNull();
        assertThat(categories).hasSize(1);
        assertThat(categories.get(0).getCategoryName()).isEqualTo("Test Category");
        verify(categoryRepository).findByIsActive(true);
    }

    @Test
    @DisplayName("Should hide inactive options from consumers but list them for the management screen")
    void shouldOnlyListInactiveOptionsWhenAsked() {
        // Arrange
        SystemOption inactiveOption = SystemOption.builder()
                .category(category)
                .optionKey("retired_option")
                .optionLabel("Retired Option")
                .isActive(false)
                .build();
        category.setOptions(List.of(option, inactiveOption));
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(category));

        // Act
        OptionCategoryResponse consumerView = systemOptionService.getCategory(1L, false);
        OptionCategoryResponse adminView = systemOptionService.getCategory(1L, true);

        // Assert
        assertThat(consumerView.getOptions())
                .extracting(SystemOptionResponse::getOptionKey)
                .containsExactly("test_option");
        assertThat(adminView.getOptions())
                .extracting(SystemOptionResponse::getOptionKey)
                .containsExactlyInAnyOrder("test_option", "retired_option");
        assertThat(adminView.getOptions())
                .filteredOn(opt -> opt.getOptionKey().equals("retired_option"))
                .allMatch(opt -> !opt.getIsActive());
    }

    @Test
    @DisplayName("Should get category by ID successfully")
    void shouldGetCategoryByIdSuccessfully() {
        // Arrange
        Long categoryId = 1L;
        when(categoryRepository.findById(categoryId)).thenReturn(Optional.of(category));

        // Act
        OptionCategoryResponse response = systemOptionService.getCategory(categoryId, false);

        // Assert
        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo(categoryId);
        assertThat(response.getCategoryName()).isEqualTo("Test Category");
        verify(categoryRepository).findById(categoryId);
    }

    @Test
    @DisplayName("Should throw ResourceNotFoundException when category not found")
    void shouldThrowExceptionWhenCategoryNotFound() {
        // Arrange
        Long categoryId = 999L;
        when(categoryRepository.findById(categoryId)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> systemOptionService.getCategory(categoryId, false))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Option category not found");

        verify(categoryRepository).findById(categoryId);
    }

    @Test
    @DisplayName("Should create category successfully when admin")
    void shouldCreateCategorySuccessfullyWhenAdmin() {
        // Arrange
        CreateOptionCategoryRequest request = new CreateOptionCategoryRequest();
        request.setCategoryKey("new_category");
        request.setCategoryName("New Category");
        request.setDescription("New category description");
        request.setIsActive(true);

        OptionCategory newCategory = OptionCategory.builder()
                .categoryKey("new_category")
                .categoryName("New Category")
                .isActive(true)
                .build();

        when(categoryRepository.findByCategoryKey("new_category")).thenReturn(Optional.empty());
        when(categoryRepository.save(any(OptionCategory.class))).thenReturn(newCategory);

        // Act
        OptionCategoryResponse response = systemOptionService.createCategory(request, adminPrincipal);

        // Assert
        assertThat(response).isNotNull();
        assertThat(response.getCategoryName()).isEqualTo("New Category");
        verify(categoryRepository).findByCategoryKey("new_category");
        verify(categoryRepository).save(any(OptionCategory.class));
    }

    @Test
    @DisplayName("Should throw BadRequestException when category key already exists")
    void shouldThrowExceptionWhenCategoryKeyExists() {
        // Arrange
        CreateOptionCategoryRequest request = new CreateOptionCategoryRequest();
        request.setCategoryKey("test_category");

        when(categoryRepository.findByCategoryKey("test_category")).thenReturn(Optional.of(category));

        // Act & Assert
        assertThatThrownBy(() -> systemOptionService.createCategory(request, adminPrincipal))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Category key already exists");

        verify(categoryRepository, never()).save(any(OptionCategory.class));
    }

    @Test
    @DisplayName("Should throw ForbiddenException when non-admin creates category")
    void shouldThrowExceptionWhenNonAdminCreatesCategory() {
        // Arrange
        CreateOptionCategoryRequest request = new CreateOptionCategoryRequest();
        request.setCategoryKey("new_category");

        // Act & Assert
        assertThatThrownBy(() -> systemOptionService.createCategory(request, therapistPrincipal))
                .isInstanceOf(ForbiddenException.class)
                .hasMessageContaining("Only administrators can create option categories");

        verify(categoryRepository, never()).save(any(OptionCategory.class));
    }

    @Test
    @DisplayName("Should update category successfully")
    void shouldUpdateCategorySuccessfully() {
        // Arrange
        Long categoryId = 1L;
        UpdateOptionCategoryRequest request = new UpdateOptionCategoryRequest();
        request.setCategoryName("Updated Category Name");
        request.setIsActive(false);

        when(categoryRepository.findById(categoryId)).thenReturn(Optional.of(category));
        when(categoryRepository.save(any(OptionCategory.class))).thenReturn(category);

        // Act
        OptionCategoryResponse response = systemOptionService.updateCategory(categoryId, request, adminPrincipal);

        // Assert
        assertThat(response).isNotNull();
        verify(categoryRepository).findById(categoryId);
        verify(categoryRepository).save(any(OptionCategory.class));
    }

    @Test
    @DisplayName("Should throw ForbiddenException when updating system category")
    void shouldThrowExceptionWhenUpdatingSystemCategory() {
        // Arrange
        Long categoryId = 1L;
        UpdateOptionCategoryRequest request = new UpdateOptionCategoryRequest();
        category.setIsSystem(true);

        when(categoryRepository.findById(categoryId)).thenReturn(Optional.of(category));

        // Act & Assert
        assertThatThrownBy(() -> systemOptionService.updateCategory(categoryId, request, adminPrincipal))
                .isInstanceOf(ForbiddenException.class)
                .hasMessageContaining("Cannot modify system categories");

        verify(categoryRepository, never()).save(any(OptionCategory.class));
    }

    @Test
    @DisplayName("Should delete category successfully")
    void shouldDeleteCategorySuccessfully() {
        // Arrange
        Long categoryId = 1L;
        when(categoryRepository.findById(categoryId)).thenReturn(Optional.of(category));
        when(optionRepository.findByCategoryId(categoryId)).thenReturn(List.of());
        doNothing().when(categoryRepository).delete(category);

        // Act
        systemOptionService.deleteCategory(categoryId, adminPrincipal);

        // Assert
        verify(categoryRepository).findById(categoryId);
        verify(optionRepository).findByCategoryId(categoryId);
        verify(categoryRepository).delete(category);
    }

    @Test
    @DisplayName("Should throw BadRequestException when deleting category with options")
    void shouldThrowExceptionWhenDeletingCategoryWithOptions() {
        // Arrange
        Long categoryId = 1L;
        when(categoryRepository.findById(categoryId)).thenReturn(Optional.of(category));
        when(optionRepository.findByCategoryId(categoryId)).thenReturn(List.of(option));

        // Act & Assert
        assertThatThrownBy(() -> systemOptionService.deleteCategory(categoryId, adminPrincipal))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Cannot delete category with existing options");

        verify(categoryRepository, never()).delete(any(OptionCategory.class));
    }

    @Test
    @DisplayName("Should get options by category ID successfully")
    void shouldGetOptionsByCategoryIdSuccessfully() {
        // Arrange
        Long categoryId = 1L;
        when(optionRepository.findByCategoryId(categoryId)).thenReturn(List.of(option));

        // Act
        List<SystemOptionResponse> options = systemOptionService.getOptions(categoryId);

        // Assert
        assertThat(options).isNotNull();
        assertThat(options).hasSize(1);
        assertThat(options.get(0).getOptionLabel()).isEqualTo("Test Option");
        verify(optionRepository).findByCategoryId(categoryId);
    }

    @Test
    @DisplayName("Should create option successfully when admin")
    void shouldCreateOptionSuccessfullyWhenAdmin() {
        // Arrange
        CreateSystemOptionRequest request = new CreateSystemOptionRequest();
        request.setCategoryId(1L);
        request.setOptionKey("new_option");
        request.setOptionLabel("New Option");
        request.setIsActive(true);

        SystemOption newOption = SystemOption.builder()
                .category(category)
                .optionKey("new_option")
                .optionLabel("New Option")
                .isActive(true)
                .build();

        when(categoryRepository.findById(1L)).thenReturn(Optional.of(category));
        when(optionRepository.findByCategoryId(1L)).thenReturn(List.of());
        when(optionRepository.save(any(SystemOption.class))).thenReturn(newOption);

        // Act
        SystemOptionResponse response = systemOptionService.createOption(request, adminPrincipal);

        // Assert
        assertThat(response).isNotNull();
        assertThat(response.getOptionLabel()).isEqualTo("New Option");
        verify(categoryRepository).findById(1L);
        verify(optionRepository).save(any(SystemOption.class));
    }

    @Test
    @DisplayName("Should throw exception when requester is null")
    void shouldThrowExceptionWhenRequesterIsNull() {
        // Arrange
        CreateOptionCategoryRequest request = new CreateOptionCategoryRequest();
        request.setCategoryKey("test");

        // Act & Assert
        assertThatThrownBy(() -> systemOptionService.createCategory(request, null))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("Requester is required");
    }
}
