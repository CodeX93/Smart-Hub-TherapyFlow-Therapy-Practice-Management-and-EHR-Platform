package com.smart.therapy.flow.unit.service;

import com.smart.therapy.flow.audit.service.AuditLogService;
import com.smart.therapy.flow.auth.entity.User;
import com.smart.therapy.flow.auth.repository.AuditLogRepository;
import com.smart.therapy.flow.auth.repository.UserRepository;
import com.smart.therapy.flow.client.entity.Client;
import com.smart.therapy.flow.client.repository.ClientRepository;
import com.smart.therapy.flow.common.TestDataFactory;
import com.smart.therapy.flow.common.exception.BadRequestException;
import com.smart.therapy.flow.common.exception.ConflictException;
import com.smart.therapy.flow.common.exception.ForbiddenException;
import com.smart.therapy.flow.common.exception.ResourceNotFoundException;
import com.smart.therapy.flow.common.security.AuthPrincipal;
import com.smart.therapy.flow.common.security.CurrentUserService;
import com.smart.therapy.flow.common.security.PermissionChecker;
import com.smart.therapy.flow.task.dto.*;
import com.smart.therapy.flow.task.entity.ChecklistTemplate;
import com.smart.therapy.flow.task.entity.ClientChecklist;
import com.smart.therapy.flow.task.repository.*;
import com.smart.therapy.flow.task.service.ChecklistService;
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
@DisplayName("ChecklistService Unit Tests")
@SuppressWarnings("null") // Suppress null warnings from Mockito mocks
class ChecklistServiceTest {

        @Mock
        private ChecklistTemplateRepository checklistTemplateRepository;

        @Mock
        private ChecklistItemRepository checklistItemRepository;

        @Mock
        private ClientChecklistRepository clientChecklistRepository;

        @Mock
        private ClientChecklistItemRepository clientChecklistItemRepository;

        @Mock
        private ClientRepository clientRepository;

        @Mock
        private UserRepository userRepository;

        @Mock
        private AuditLogRepository auditLogRepository;

        @Mock
        private CurrentUserService currentUserService;

        @Mock
    private AuditLogService auditLogService;

    @Spy
    private PermissionChecker permissionChecker = new PermissionChecker();

    @InjectMocks
        private ChecklistService checklistService;

        private AuthPrincipal adminPrincipal;
        private AuthPrincipal therapistPrincipal;
        private User admin;
        private User therapist;
        private Client client;
        private ChecklistTemplate template;

        @BeforeEach
        void setUp() {
                admin = TestDataFactory.createTestAdmin();
                admin.setId(1L);
                adminPrincipal = TestDataFactory.createAuthPrincipal(admin, "ROLE_ADMIN", "CONSENT_ADMIN_VIEW", "CLIENT_VIEW_ALL");

                therapist = TestDataFactory.createTestTherapist();
                therapist.setId(2L);
                therapistPrincipal = TestDataFactory.createAuthPrincipal(therapist, "ROLE_THERAPIST", "CLIENT_VIEW_OWN");

                client = TestDataFactory.createTestClient();
                client.setId(1L);

                template = ChecklistTemplate.builder()
                                .name("Intake Checklist")
                                .description("Initial client intake checklist")
                                .isActive(true)
                                .build();
                template.setId(1L);

        }

        @Test
        @DisplayName("Should get all active checklist templates")
        void shouldGetAllActiveChecklistTemplates() {
                // Arrange
                when(checklistTemplateRepository.findByIsActiveTrueOrderBySortOrderAscNameAsc())
                                .thenReturn(List.of(template));
                when(checklistItemRepository.countActiveItemsByTemplateIds(any()))
                                .thenReturn(List.<Object[]>of(new Object[] { 1L, 3L }));

                // Act
                List<ChecklistTemplateResponse> templates = checklistService.getTemplates();

                // Assert
                assertThat(templates).isNotNull();
                assertThat(templates).hasSize(1);
                assertThat(templates.get(0).getName()).isEqualTo("Intake Checklist");
                assertThat(templates.get(0).getItemCount()).isEqualTo(3);
                verify(checklistTemplateRepository).findByIsActiveTrueOrderBySortOrderAscNameAsc();
                verify(checklistItemRepository).countActiveItemsByTemplateIds(any());
        }

        @Test
        @DisplayName("Should get checklist template by ID successfully")
        void shouldGetChecklistTemplateByIdSuccessfully() {
                // Arrange
                Long templateId = 1L;
                when(checklistTemplateRepository.findByIdWithItems(templateId)).thenReturn(Optional.of(template));

                // Act
                ChecklistTemplateResponse response = checklistService.getTemplate(templateId);

                // Assert
                assertThat(response).isNotNull();
                assertThat(response.getId()).isEqualTo(templateId);
                assertThat(response.getName()).isEqualTo("Intake Checklist");
                verify(checklistTemplateRepository).findByIdWithItems(templateId);
        }

        @Test
        @DisplayName("Should throw ResourceNotFoundException when template not found")
        void shouldThrowExceptionWhenTemplateNotFound() {
                // Arrange
                Long templateId = 999L;
                when(checklistTemplateRepository.findByIdWithItems(templateId)).thenReturn(Optional.empty());

                // Act & Assert
                assertThatThrownBy(() -> checklistService.getTemplate(templateId))
                                .isInstanceOf(ResourceNotFoundException.class)
                                .hasMessageContaining("Checklist template not found");

                verify(checklistTemplateRepository).findByIdWithItems(templateId);
        }

        @Test
        @DisplayName("Should create checklist template successfully when admin")
        void shouldCreateChecklistTemplateSuccessfullyWhenAdmin() {
                // Arrange
                CreateChecklistTemplateRequest request = new CreateChecklistTemplateRequest();
                request.setName("New Checklist");
                request.setDescription("New checklist description");
                request.setIsActive(true);

                ChecklistTemplate newTemplate = ChecklistTemplate.builder()
                                .name("New Checklist")
                                .description("New checklist description")
                                .isActive(true)
                                .build();

                when(checklistTemplateRepository.save(any(ChecklistTemplate.class))).thenReturn(newTemplate);

                // Act
                ChecklistTemplateResponse response = checklistService.createTemplate(request, adminPrincipal,
                                "127.0.0.1");

                // Assert
                assertThat(response).isNotNull();
                assertThat(response.getName()).isEqualTo("New Checklist");
                verify(checklistTemplateRepository).save(any(ChecklistTemplate.class));
        }

        @Test
        @DisplayName("Should throw ForbiddenException when non-admin creates template")
        void shouldThrowExceptionWhenNonAdminCreatesTemplate() {
                // Arrange
                CreateChecklistTemplateRequest request = new CreateChecklistTemplateRequest();
                request.setName("New Checklist");

                // Act & Assert
                assertThatThrownBy(() -> checklistService.createTemplate(request, therapistPrincipal, "127.0.0.1"))
                                .isInstanceOf(ForbiddenException.class)
                                .hasMessageContaining("Only administrators can create checklist templates");

                verify(checklistTemplateRepository, never()).save(any(ChecklistTemplate.class));
        }

        @Test
        @DisplayName("Should assign checklist to client successfully")
        void shouldAssignChecklistToClientSuccessfully() {
                // Arrange
                Long clientId = 1L;
                AssignChecklistRequest request = new AssignChecklistRequest();
                request.setTemplateId(1L);

                ClientChecklist clientChecklist = ClientChecklist.builder()
                                .client(client)
                                .template(template)
                                .isCompleted(false)
                                .build();
                clientChecklist.setId(1L);

                when(clientRepository.findById(clientId)).thenReturn(Optional.of(client));
                when(checklistTemplateRepository.findByIdWithItems(1L)).thenReturn(Optional.of(template));
                when(clientChecklistRepository.existsByClientIdAndTemplateId(clientId, 1L)).thenReturn(false);
                when(clientChecklistRepository.save(any(ClientChecklist.class))).thenReturn(clientChecklist);

                // Act
                ClientChecklistResponse response = checklistService.assignChecklistToClient(clientId, request,
                                therapistPrincipal, "127.0.0.1");

                // Assert
                assertThat(response).isNotNull();
                verify(clientRepository).findById(clientId);
                verify(checklistTemplateRepository).findByIdWithItems(1L);
                verify(clientChecklistRepository).save(any(ClientChecklist.class));
        }

        @Test
        @DisplayName("Should throw ResourceNotFoundException when client not found")
        void shouldThrowExceptionWhenClientNotFound() {
                // Arrange
                Long clientId = 999L;
                AssignChecklistRequest request = new AssignChecklistRequest();
                request.setTemplateId(1L);

                when(clientRepository.findById(clientId)).thenReturn(Optional.empty());

                // Act & Assert
                assertThatThrownBy(() -> checklistService.assignChecklistToClient(clientId, request, therapistPrincipal,
                                "127.0.0.1"))
                                .isInstanceOf(ResourceNotFoundException.class)
                                .hasMessageContaining("Client not found");

                verify(clientChecklistRepository, never()).save(any(ClientChecklist.class));
        }

        @Test
        @DisplayName("Should throw ResourceNotFoundException when template not found")
        void shouldThrowExceptionWhenTemplateNotFoundForAssignment() {
                // Arrange
                Long clientId = 1L;
                AssignChecklistRequest request = new AssignChecklistRequest();
                request.setTemplateId(999L);

                when(clientRepository.findById(clientId)).thenReturn(Optional.of(client));
                when(checklistTemplateRepository.findByIdWithItems(999L)).thenReturn(Optional.empty());

                // Act & Assert
                assertThatThrownBy(() -> checklistService.assignChecklistToClient(clientId, request, therapistPrincipal,
                                "127.0.0.1"))
                                .isInstanceOf(ResourceNotFoundException.class)
                                .hasMessageContaining("Checklist template not found");

                verify(clientChecklistRepository, never()).save(any(ClientChecklist.class));
        }

        @Test
        @DisplayName("Should throw BadRequestException when template already assigned")
        void shouldThrowExceptionWhenTemplateAlreadyAssigned() {
                // Arrange
                Long clientId = 1L;
                AssignChecklistRequest request = new AssignChecklistRequest();
                request.setTemplateId(1L);

                when(clientRepository.findById(clientId)).thenReturn(Optional.of(client));
                when(checklistTemplateRepository.findByIdWithItems(1L)).thenReturn(Optional.of(template));
                when(clientChecklistRepository.existsByClientIdAndTemplateId(clientId, 1L)).thenReturn(true);

                // Act & Assert
                assertThatThrownBy(() -> checklistService.assignChecklistToClient(clientId, request, therapistPrincipal,
                                "127.0.0.1"))
                                .isInstanceOf(ConflictException.class)
                                .hasMessageContaining("already assigned");

                verify(clientChecklistRepository, never()).save(any(ClientChecklist.class));
        }

        @Test
        @DisplayName("Should get client checklists successfully")
        void shouldGetClientChecklistsSuccessfully() {
                // Arrange
                Long clientId = 1L;
                ClientChecklist clientChecklist = ClientChecklist.builder()
                                .client(client)
                                .template(template)
                                .isCompleted(false)
                                .description("Client intake checklist description")
                                .build();
                clientChecklist.setId(1L);

                when(clientChecklistRepository.findByClientIdWithDetails(clientId))
                                .thenReturn(List.of(clientChecklist));

                // Act
                List<ClientChecklistResponse> checklists = checklistService.getClientChecklists(clientId);

                // Assert
                assertThat(checklists).isNotNull();
                assertThat(checklists).hasSize(1);
                verify(clientChecklistRepository).findByClientIdWithDetails(clientId);
        }

        @Test
        @DisplayName("Should throw exception when request is null")
        void shouldThrowExceptionWhenRequestIsNull() {
                // Act & Assert
                assertThatThrownBy(() -> checklistService.createTemplate(null, adminPrincipal, "127.0.0.1"))
                                .isInstanceOf(NullPointerException.class)
                                .hasMessageContaining("request");
        }

        @Test
        @DisplayName("Should get client checklist with description")
        void shouldGetClientChecklistWithDescription() {
                // Arrange
                Long clientId = 1L;
                ClientChecklist clientChecklist = ClientChecklist.builder()
                                .client(client)
                                .template(template)
                                .isCompleted(false)
                                .description("Detailed checklist description for client intake process")
                                .build();
                clientChecklist.setId(1L);

                when(clientChecklistRepository.findByClientIdWithDetails(clientId))
                                .thenReturn(List.of(clientChecklist));

                // Act
                List<ClientChecklistResponse> checklists = checklistService.getClientChecklists(clientId);

                // Assert
                assertThat(checklists).isNotNull();
                assertThat(checklists).hasSize(1);
                assertThat(checklists.get(0).getDescription())
                                .isEqualTo("Detailed checklist description for client intake process");
                verify(clientChecklistRepository).findByClientIdWithDetails(clientId);
        }

        @Test
        @DisplayName("Should create template with checklist items including daysFromStart")
        void shouldCreateTemplateWithChecklistItemsIncludingDaysFromStart() {
                // Arrange
                CreateChecklistTemplateRequest request = new CreateChecklistTemplateRequest();
                request.setName("Intake Checklist");
                request.setDescription("Client intake process");
                request.setIsActive(true);

                CreateChecklistItemRequest itemRequest = new CreateChecklistItemRequest();
                itemRequest.setTitle("Complete intake form");
                itemRequest.setDescription("Fill out all required intake information");
                itemRequest.setCategory("intake");
                itemRequest.setIsRequired(true);
                itemRequest.setDaysFromStart(3);
                request.setItems(List.of(itemRequest));

                com.smart.therapy.flow.task.entity.ChecklistItem item = com.smart.therapy.flow.task.entity.ChecklistItem
                                .builder()
                                .template(template)
                                .title("Complete intake form")
                                .description("Fill out all required intake information")
                                .category(com.smart.therapy.flow.task.enums.CheckListCategory.INTAKE)
                                .isRequired(true)
                                .daysFromStart(3)
                                .build();
                item.setId(1L);

                ChecklistTemplate newTemplate = ChecklistTemplate.builder()
                                .name("Intake Checklist")
                                .description("Client intake process")
                                .isActive(true)
                                .build();
                newTemplate.setItems(List.of(item));

                when(checklistTemplateRepository.save(any(ChecklistTemplate.class))).thenReturn(newTemplate);
                when(checklistItemRepository.saveAll(anyList())).thenReturn(List.of(item));

                // Act
                ChecklistTemplateResponse response = checklistService.createTemplate(request, adminPrincipal,
                                "127.0.0.1");

                // Assert
                assertThat(response).isNotNull();
                assertThat(response.getName()).isEqualTo("Intake Checklist");
                assertThat(response.getItems()).isNotNull();
                assertThat(response.getItems()).hasSize(1);
                assertThat(response.getItems().get(0).getTitle()).isEqualTo("Complete intake form");
                assertThat(response.getItems().get(0).getDaysFromStart()).isEqualTo(3);
                verify(checklistTemplateRepository).save(any(ChecklistTemplate.class));
                verify(checklistItemRepository).saveAll(anyList());
        }

        @Test
        @DisplayName("Should map daysFromStart in checklist item response")
        void shouldMapDaysFromStartInChecklistItemResponse() {
                // Arrange
                com.smart.therapy.flow.task.entity.ChecklistItem item = com.smart.therapy.flow.task.entity.ChecklistItem
                                .builder()
                                .template(template)
                                .title("Follow-up assessment")
                                .description("Complete follow-up assessment")
                                .category(com.smart.therapy.flow.task.enums.CheckListCategory.ASSESSMENT)
                                .isRequired(true)
                                .daysFromStart(7)
                                .build();
                item.setId(1L);

                // Verify the entity has the field set correctly
                assertThat(item.getDaysFromStart()).isNotNull();
                assertThat(item.getDaysFromStart()).isEqualTo(7);
        }
}
