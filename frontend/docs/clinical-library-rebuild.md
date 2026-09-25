# Clinical Content Library — Technical Handoff & Rebuild Document

> Feature: A reusable **clinical content library** for session notes — hierarchical categories, individual and bulk entry management, entry-to-entry connections (Smart Connect), and in-note library pickers that copy pre-written text into structured session-note fields.
>
> **Target stack for the new project:** **React** frontend (TanStack Query) + **Java** backend (Spring Boot + JPA recommended) + **PostgreSQL**.
>
> The **HTTP API contract** (paths, JSON shapes, status codes) is preserved so the React UI can be ported with minimal changes. Backend sections below translate the original Node/Drizzle implementation into Java.

---

## 1. Feature Overview

The library is a **content repository**, not a relational link on session notes.

| Capability | Who uses it | Where |
|------------|-------------|-------|
| Manage categories & entries | Admin / Supervisor | `/library` page |
| Bulk import from spreadsheet paste | Admin / Supervisor | `/library` → Bulk Add |
| Manual create/edit with Smart Connect | Admin / Supervisor | `/library` → Add Entry |
| Pick pre-written content into note fields | Therapist (+ Admin) | Session note editor → Library button per field |

**Key design decisions:**

1. **Session notes store copied text only** — selecting a library entry appends `entry.content` into a text field (`session_focus`, `symptoms`, etc.). No `library_entry_id` FK on `session_notes`.
2. **Usage tracking** — `library_entries.usage_count` increments when a therapist picks an entry in session notes (not on admin create/import).
3. **Connections graph** — entries can link to other entries (`library_entry_connections`). Used for Smart Connect on admin create and for "show connected entries only" filtering in session notes.
4. **Soft delete** — categories and entries set `is_active = false`; connections are hard-deleted.
5. **Clinical coding pattern** — optional titles like `ANXS10`, `ANXI10_2` encode condition + type + pathway for auto-suggestions.

---

## 2. User Flows

### 2.1 Admin — `/library` page

1. Admin/Supervisor opens **Administration → Clinical Library** (`/library`).
2. Page loads root categories as **tabs** (first tab auto-selected).
3. Per tab:
   - **Search** — debounced 300ms, server-side `ILIKE` on title + content.
   - **Bulk Add** — paste Excel/CSV → map columns → preview → import.
   - **Add Entry** — single form with title, content, category, tags, sort order, Smart Connect suggestions.
4. Entry list shows title, content preview, category badge, tags, usage count, connection count, expandable connected entries.
5. Bulk select + delete, or edit/delete individual entries.

**Access:** Page blocked for therapists and accountants. Therapists use library only inside session notes.

### 2.2 Bulk upload flow

1. Click **Bulk Add** on a category tab.
2. Paste tab- or comma-separated data (Excel copy).
3. System auto-detects columns and maps headers:
   - `domain` / `category` → Domain
   - `subdomain` / `sub-domain` / `subcategory` → Subdomain
   - `composite` / `title` / `name` / `entry` → Title
   - `content` / `description` / `text` / `definition` → Content
4. Toggle **"First row is header"**.
5. Manually remap columns if needed (Domain, Subdomain, Composite, Content, Skip).
6. Preview table shows valid/invalid rows.
7. Click **Import** → `POST /api/library/bulk-entries`.

**Two import modes:**

| Mode | When | Payload |
|------|------|---------|
| **Category tab mode** | No Domain/Subdomain columns mapped; current tab has `categoryId` | `{ categoryId, entries: [{ title, content }] }` |
| **Domain mode** | Domain (and optional Subdomain) columns mapped | `{ entries: [{ domain, subdomain?, title, content }] }` — categories auto-created |

### 2.3 Manual entry flow

1. Click **Add Entry** (or pencil icon to edit).
2. Fill: Title*, Content*, Category, Tags (comma-separated), Sort Order.
3. **Smart Connect** panel suggests related entries when title is non-empty:
   - Pattern match (100%): same condition + pathway across S/I/P/G types.
   - Keyword match (60%): fallback across clinically related categories.
   - Manual catalog browse.
4. Submit → `POST /api/library/entries` (or `PUT` for edit).
5. If connections selected → `POST /api/library/connections/batch`.

### 2.4 Therapist — session note library picker

1. Therapist opens client → Session History → Add/Edit Session Note.
2. Five clinical fields have a **Library** button (`BookOpen` icon):
   - Session Focus, Symptoms, Short-term Goals, Intervention, Progress
3. Click Library → dialog loads all entries, **client-filters by hardcoded category ID** for that field.
4. Search filters title, content, tags (client-side).
5. Optional **"Show connected entries only"** — filters by connections to entries selected in prior fields (see §9).
6. Click entry → appends `entry.content` to field (`\n\n` separator if field already has text).
7. `POST /api/library/entries/:id/increment-usage` fires; entry ID tracked in component state for downstream field filtering.
8. On note save → text fields persist to `session_notes` (no library FK).

**Fields without library picker:** Remarks, Recommendations.

---

## 3. Functional Requirements

| # | Requirement |
|---|-------------|
| FR1 | Hierarchical categories (`parent_id` self-reference). |
| FR2 | Library entries belong to exactly one category; title + content required. |
| FR3 | Duplicate titles rejected on single create (`409`); skipped on bulk with per-row error. |
| FR4 | Bulk import supports domain/subdomain auto-category creation OR fixed `categoryId`. |
| FR5 | Soft delete for categories and entries (`is_active = false`). |
| FR6 | Entry connections are bidirectional for read; stored as directed `from_entry_id` → `to_entry_id`. |
| FR7 | Smart Connect suggests related entries on manual create using pattern + keyword matching. |
| FR8 | Session note picker copies text only; increments `usage_count` on select. |
| FR9 | Connected-entries filter in session notes chains across clinical fields (Symptoms → Goals → Interventions → Progress). |
| FR10 | `/library` admin page restricted to Admin/Supervisor; library APIs available to therapists (except accountant role). |
| FR11 | Search orders by `usage_count` DESC (frontend applies natural title sort within results). |
| FR12 | Bulk delete on admin page = parallel single DELETE calls (no dedicated bulk endpoint). |

---

## 4. Technical Architecture

```
┌─────────────────────────────────────────────────────────────────────┐
│ Frontend — React + TanStack Query (port from ClientHubAI)           │
│   pages/LibraryPage.tsx        → Admin page (tabs, bulk, manual)    │
│   components/library/SmartConnectPanel.tsx                          │
│   hooks/use-smart-connect.ts   → Pattern + keyword (client-only)    │
│   hooks/use-connected-entries.ts                                    │
│   components/session-notes/SessionNotesManager.tsx                  │
│     └── LibraryPicker (inline) per clinical field                   │
│   pages/ClientDetailPage.tsx   → Hosts SessionNotesManager          │
│   lib/apiClient.ts             → fetch/axios + auth headers           │
└───────────────────────────────┬─────────────────────────────────────┘
                                │ JSON over HTTP (camelCase keys)
                                │ Base URL e.g. http://localhost:8080/api
┌───────────────────────────────▼─────────────────────────────────────┐
│ Backend — Java / Spring Boot                                        │
│   controller/LibraryCategoryController.java                         │
│   controller/LibraryEntryController.java                            │
│   controller/LibraryConnectionController.java                       │
│   service/LibraryCategoryService.java                               │
│   service/LibraryEntryService.java                                │
│   service/LibraryConnectionService.java                             │
│   service/LibraryBulkImportService.java                             │
│   repository/*Repository.java        → Spring Data JPA              │
│   entity/*Entity.java                → JPA @Entity                  │
│   dto/*Request.java, *Response.java  → Jackson JSON                 │
│   security/RoleAuthorization         → JWT + @PreAuthorize          │
└───────────────────────────────┬─────────────────────────────────────┘
                                │
┌───────────────────────────────▼─────────────────────────────────────┐
│ PostgreSQL                                                          │
│   library_categories                                                │
│   library_entries                                                   │
│   library_entry_connections                                         │
│   session_notes (receives copied text — no library FK)              │
└─────────────────────────────────────────────────────────────────────┘
```

### 4.1 JSON naming (critical for React compatibility)

The React app expects **camelCase** JSON — match the original API exactly:

| DB column (snake_case) | JSON field (camelCase) |
|------------------------|------------------------|
| `category_id` | `categoryId` |
| `created_by_id` | `createdById` |
| `parent_id` | `parentId` |
| `sort_order` | `sortOrder` |
| `usage_count` | `usageCount` |
| `is_active` | `isActive` |
| `from_entry_id` | `fromEntryId` |
| `to_entry_id` | `toEntryId` |
| `connection_type` | `connectionType` |
| `created_at` | `createdAt` |

Configure Jackson once globally:

```java
// application.yml
spring:
  jackson:
    property-naming-strategy: LOWER_CAMEL_CASE
```

Or use `@JsonNaming(PropertyNamingStrategies.LowerCamelCaseStrategy.class)` on DTOs. Map entity `snake_case` columns with `@Column(name = "category_id")` on camelCase Java fields.

---

## 5. Data Model (Schema / DB)

Source: `shared/schema.ts`

### 5.1 `library_categories`

| Column | Type | Notes |
|--------|------|-------|
| `id` | serial PK | |
| `name` | text NOT NULL | |
| `description` | text | optional |
| `parent_id` | integer | self-FK; `null` = root (domain) |
| `sort_order` | integer | default 0 |
| `is_active` | boolean | default true; false = soft delete |
| `created_at`, `updated_at` | timestamp | |

**Hierarchy:** Domain (root) → Subdomain (child). Bulk import auto-creates both levels.

### 5.2 `library_entries`

| Column | Type | Notes |
|--------|------|-------|
| `id` | serial PK | |
| `category_id` | integer FK → `library_categories` | ON DELETE CASCADE |
| `title` | text NOT NULL | duplicate check is case-insensitive trim |
| `content` | text NOT NULL | text copied into session notes |
| `tags` | text[] | optional |
| `created_by_id` | integer FK → `users` | ON DELETE CASCADE |
| `sort_order` | integer | default 0 |
| `usage_count` | integer | default 0; incremented on session-note pick |
| `is_active` | boolean | default true; false = soft delete |
| `created_at`, `updated_at` | timestamp | |

### 5.3 `library_entry_connections`

| Column | Type | Notes |
|--------|------|-------|
| `id` | serial PK | |
| `from_entry_id` | integer FK → `library_entries` | ON DELETE CASCADE |
| `to_entry_id` | integer FK → `library_entries` | ON DELETE CASCADE |
| `connection_type` | varchar(20) | e.g. `relates_to` |
| `description` | text | optional |
| `strength` | integer | 1–5, default 1 |
| `is_active` | boolean | default true |
| `created_by_id` | integer FK → `users` | |
| `created_at`, `updated_at` | timestamp | |

**Defined connection types (enum exists but column uses varchar):**
`relates_to`, `follows_from`, `supports`, `alternative_to`, `prerequisite_for`, `expands_on`

### 5.4 `session_notes` (library consumption target)

Library text lands in these **plain text** columns (no library FK):

| DB column | Form field | Hardcoded library `categoryId` in picker |
|-----------|------------|-------------------------------------------|
| `session_focus` | Session Focus | **1** |
| `symptoms` | Symptoms | **2** |
| `short_term_goals` | Short-term Goals | **3** |
| `intervention` | Intervention | **4** |
| `progress` | Progress | **5** |

> **Porting note:** Category IDs 1–5 are hardcoded in `session-notes-manager.tsx`. Seed your categories in this order or replace hardcoded IDs with a slug/name lookup.

### 5.5 Request / response DTOs (Java)

Use Jakarta Bean Validation on request DTOs. **Do not trust `createdById` from the client** on create — set from `SecurityContext` / JWT principal.

```java
// LibraryCategoryRequest.java
public record LibraryCategoryRequest(
    @NotBlank String name,
    String description,
    Long parentId,
    Integer sortOrder,
    Boolean isActive
) {}

// LibraryEntryRequest.java
public record LibraryEntryRequest(
    @NotNull Long categoryId,
    @NotBlank String title,
    @NotBlank String content,
    List<String> tags,
    Integer sortOrder,
    Boolean isActive
    // omit createdById — server sets from auth
) {}

// LibraryEntryConnectionRequest.java
public record LibraryEntryConnectionRequest(
    @NotNull Long fromEntryId,
    @NotNull Long toEntryId,
    @NotBlank String connectionType,
    String description,
    @Min(1) @Max(5) Integer strength
) {}

// BulkImportRequest.java
public record BulkImportRequest(
    Long categoryId,
    @NotEmpty List<BulkImportRow> entries
) {}

public record BulkImportRow(
    String domain,
    String subdomain,
    @NotBlank String title,
    @NotBlank String content,
    List<String> tags,
    Integer sortOrder
) {}

// ConnectedBulkRequest.java
public record ConnectedBulkRequest(@NotNull List<Long> entryIds) {}

// BatchConnectionsRequest.java
public record BatchConnectionsRequest(@NotEmpty List<LibraryEntryConnectionRequest> connections) {}
```

**TypeScript equivalents (for React types only):**

```typescript
interface InsertLibraryCategory {
  name: string;
  description?: string;
  parentId?: number | null;
  sortOrder?: number;
  isActive?: boolean;
}

interface InsertLibraryEntry {
  categoryId: number;
  title: string;
  content: string;
  tags?: string[] | null;
  sortOrder?: number;
  isActive?: boolean;
  // createdById set by server
}
```

---

## 6. API Design

**Security on all library routes:** authenticated user required; **accountant role → 403**.

| Original (Node) | Java (Spring Boot) |
|-----------------|-------------------|
| `requireAuth` | `@PreAuthorize("isAuthenticated()")` or JWT filter |
| `blockAccountant` | `@PreAuthorize("!hasRole('ACCOUNTANT')")` on controller class |
| `req.user.id` | `@AuthenticationPrincipal UserPrincipal user` → `user.getId()` |

**CORS:** allow React dev origin (e.g. `http://localhost:5173`) on `/api/**`.

**Controller base path:** `@RequestMapping("/api/library")` — keep paths identical to the React `queryKey` strings.

### 6.1 Categories

| Method | Path | Body | Response |
|--------|------|------|----------|
| GET | `/api/library/categories` | — | `LibraryCategory[]` tree with `children` |
| GET | `/api/library/categories/:id` | — | Category + children + entries |
| POST | `/api/library/categories` | `InsertLibraryCategory` | `201` category |
| PUT | `/api/library/categories/:id` | partial `InsertLibraryCategory` | Updated category |
| DELETE | `/api/library/categories/:id` | — | `204` (soft delete) |

### 6.2 Entries

| Method | Path | Query / Body | Response |
|--------|------|--------------|----------|
| GET | `/api/library/entries` | `?categoryId=` optional | `LibraryEntryWithDetails[]` (joins category + createdBy) |
| GET | `/api/library/entries/:id` | — | Single entry or `404` |
| POST | `/api/library/entries` | `InsertLibraryEntry` | `201` entry; `409` if duplicate title |
| PUT | `/api/library/entries/:id` | partial `InsertLibraryEntry` | Updated entry |
| DELETE | `/api/library/entries/:id` | — | `204` (soft delete) |
| GET | `/api/library/search` | `?q=` required, `?categoryId=` optional | Matching entries, ordered by `usageCount` DESC |
| POST | `/api/library/entries/:id/increment-usage` | — | `204` |

**GET entry response shape (enriched):**
```typescript
LibraryEntry & {
  category: LibraryCategory;
  createdBy: User;  // at minimum { id, username }
}
```

**Single create duplicate check (Java service):**

```java
public void assertTitleUnique(String title) {
  String normalized = title.trim().toLowerCase(Locale.ROOT);
  if (entryRepository.existsByIsActiveTrueAndLowerTitle(normalized)) {
    throw new DuplicateEntryException("Entry with title \"" + title + "\" already exists");
  }
}
// @ExceptionHandler → 409 { "message": "Duplicate entry", "error": "..." }
```

### 6.3 Bulk entries

**`POST /api/library/bulk-entries`**

Request:
```json
{
  "categoryId": 2,
  "entries": [
    {
      "title": "ANXS10",
      "content": "Client reports excessive worry...",
      "domain": "Anxiety",
      "subdomain": "Cognitive",
      "tags": ["anxiety"],
      "sortOrder": 0
    }
  ]
}
```

- `categoryId` optional if every row has `domain`.
- Rows with `subdomain` must also have `domain`.
- `createdById` set server-side from `req.user.id` (not from client).

Response `201`:
```json
{
  "total": 50,
  "successful": 45,
  "skipped": 3,
  "failed": 2,
  "categoriesCreated": 2,
  "errors": [
    { "row": 5, "title": "ANXS10", "error": "Duplicate - entry with this title already exists" }
  ]
}
```

**Server bulk algorithm:**
1. Validate `entries` is non-empty array.
2. Require `categoryId` OR at least one row with `domain`.
3. Load all existing titles into a `Set` (lowercase trim).
4. If domain mode: flatten existing category tree into cache `Map<"parentId::name", id>`.
5. For each row:
   - Skip if duplicate title → `skipped++`, push to `errors`.
   - `resolveCategory(domain, subdomain)` — create missing categories, increment `categoriesCreated`.
   - Validate with `insertLibraryEntrySchema`, insert, add title to Set, `successful++`.
   - On Zod/DB error → `failed++`, push to `errors`.

**Category resolution cache keys:**
- Root domain: `root::{domain.toLowerCase()}`
- Subdomain: `{domainId}::{subdomain.toLowerCase()}`

### 6.4 Connections

| Method | Path | Body | Response |
|--------|------|------|----------|
| GET | `/api/library/connections` | `?entryId=` optional | Connections with from/to entries |
| GET | `/api/library/entries/:id/connected` | — | Bidirectional connected entries + metadata |
| POST | `/api/library/entries/connected-bulk` | `{ entryIds: number[] }` | `Record<entryId, ConnectedEntry[]>` |
| POST | `/api/library/connections` | `InsertLibraryEntryConnection` + `createdById` from user | `201` |
| POST | `/api/library/connections/batch` | `{ connections: [...] }` | See below |
| PUT | `/api/library/connections/:id` | partial | Updated connection |
| DELETE | `/api/library/connections/:id` | — | `204` (hard delete) |
| DELETE | `/api/library/entries/:entryId/connections` | — | `204` (delete all for entry) |

**Batch connections request:**
```json
{
  "connections": [
    {
      "fromEntryId": 123,
      "toEntryId": 456,
      "connectionType": "relates_to",
      "strength": 4,
      "description": "Auto-connected based on shared keywords"
    }
  ]
}
```

**Batch connections response `201`:**
```json
{
  "created": 3,
  "skipped": 1,
  "total": 4,
  "details": { "created": [...], "skipped": [{ "fromEntryId": 1, "toEntryId": 2 }] }
}
```

Duplicates (`DataIntegrityViolationException` / SQL state `23505`) are skipped, not errored.

**Java batch connections handler sketch:**

```java
@PostMapping("/connections/batch")
public ResponseEntity<BatchConnectionResponse> batchCreate(
    @RequestBody BatchConnectionsRequest body,
    @AuthenticationPrincipal UserPrincipal user) {
  List<LibraryEntryConnection> created = new ArrayList<>();
  List<SkippedConnection> skipped = new ArrayList<>();
  for (var conn : body.connections()) {
    try {
      created.add(connectionService.create(conn, user.getId()));
    } catch (DataIntegrityViolationException ex) {
      skipped.add(new SkippedConnection(conn.fromEntryId(), conn.toEntryId()));
    }
  }
  return ResponseEntity.status(201).body(new BatchConnectionResponse(
    created.size(), skipped.size(), body.connections().size(),
    new BatchConnectionDetails(created, skipped)));
}
```

**Connected entry shape (`getConnectedEntries`):**
```typescript
LibraryEntry & {
  category: LibraryCategory;
  connectionId: number;
  connectionType: string;
  connectionStrength: number;
}
```

**Connected-bulk:** For each `entryId`, calls `getConnectedEntries(id)` in parallel; returns object keyed by entry ID. Empty `entryIds` → `{}`.

---

## 7. Java Backend — Entities, Repositories & Services

> Replaces the original `server/storage.ts` (Drizzle) layer. Stack: **Spring Boot 3 + Spring Data JPA + PostgreSQL**.

### 7.1 Suggested package layout

```
com.yourapp.library
├── controller/
│   ├── LibraryCategoryController.java
│   ├── LibraryEntryController.java
│   └── LibraryConnectionController.java
├── service/
│   ├── LibraryCategoryService.java
│   ├── LibraryEntryService.java
│   ├── LibraryConnectionService.java
│   └── LibraryBulkImportService.java
├── repository/
│   ├── LibraryCategoryRepository.java
│   ├── LibraryEntryRepository.java
│   └── LibraryEntryConnectionRepository.java
├── entity/
│   ├── LibraryCategoryEntity.java
│   ├── LibraryEntryEntity.java
│   └── LibraryEntryConnectionEntity.java
└── dto/
    ├── LibraryCategoryDto.java
    ├── LibraryEntryWithDetailsDto.java
    ├── BulkImportResultDto.java
    └── ConnectedEntryDto.java
```

### 7.2 JPA entities (sketch)

```java
@Entity
@Table(name = "library_categories")
public class LibraryCategoryEntity {
  @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;
  @Column(nullable = false) private String name;
  private String description;
  @Column(name = "parent_id") private Long parentId;
  @Column(name = "sort_order") private Integer sortOrder = 0;
  @Column(name = "is_active") private Boolean isActive = true;
  @Column(name = "created_at") private Instant createdAt;
  @Column(name = "updated_at") private Instant updatedAt;
  // getters/setters
}

@Entity
@Table(name = "library_entries")
public class LibraryEntryEntity {
  @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;
  @Column(name = "category_id", nullable = false) private Long categoryId;
  @Column(nullable = false) private String title;
  @Column(nullable = false, columnDefinition = "text") private String content;
  @JdbcTypeCode(SqlTypes.ARRAY)
  @Column(columnDefinition = "text[]") private List<String> tags;
  @Column(name = "created_by_id", nullable = false) private Long createdById;
  @Column(name = "sort_order") private Integer sortOrder = 0;
  @Column(name = "usage_count") private Integer usageCount = 0;
  @Column(name = "is_active") private Boolean isActive = true;
  // timestamps...
}

@Entity
@Table(name = "library_entry_connections")
public class LibraryEntryConnectionEntity {
  @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;
  @Column(name = "from_entry_id") private Long fromEntryId;
  @Column(name = "to_entry_id") private Long toEntryId;
  @Column(name = "connection_type", length = 20) private String connectionType;
  private String description;
  private Integer strength = 1;
  @Column(name = "is_active") private Boolean isActive = true;
  @Column(name = "created_by_id") private Long createdById;
}
```

**Tags column:** use PostgreSQL `text[]` with Hibernate 6 `@JdbcTypeCode(SqlTypes.ARRAY)`, or store as JSON/`@ElementCollection` if your team prefers.

### 7.3 Repository interfaces

```java
public interface LibraryCategoryRepository extends JpaRepository<LibraryCategoryEntity, Long> {
  List<LibraryCategoryEntity> findByIsActiveTrueOrderBySortOrderAscNameAsc();
}

public interface LibraryEntryRepository extends JpaRepository<LibraryEntryEntity, Long> {
  List<LibraryEntryEntity> findByIsActiveTrueAndCategoryIdOrderBySortOrderAscTitleAsc(Long categoryId);
  List<LibraryEntryEntity> findByIsActiveTrueOrderBySortOrderAscTitleAsc();
  boolean existsByIsActiveTrueAndLowerTitle(String lowerTitle);

  @Modifying
  @Query("UPDATE LibraryEntryEntity e SET e.usageCount = e.usageCount + 1, e.updatedAt = CURRENT_TIMESTAMP WHERE e.id = :id")
  void incrementUsageCount(@Param("id") Long id);

  @Query("""
    SELECT e FROM LibraryEntryEntity e
    WHERE e.isActive = true
      AND (:categoryId IS NULL OR e.categoryId = :categoryId)
      AND (LOWER(e.title) LIKE LOWER(CONCAT('%', :q, '%'))
        OR LOWER(e.content) LIKE LOWER(CONCAT('%', :q, '%')))
    ORDER BY e.usageCount DESC
    """)
  List<LibraryEntryEntity> search(@Param("q") String q, @Param("categoryId") Long categoryId);
}

public interface LibraryEntryConnectionRepository extends JpaRepository<LibraryEntryConnectionEntity, Long> {
  void deleteByFromEntryIdOrToEntryId(Long fromEntryId, Long toEntryId);
}
```

For **bidirectional connected entries**, use a native query or JPQL with `OR` on `from_entry_id` / `to_entry_id` (same logic as original `getConnectedEntries`).

### 7.4 Service method map (original storage → Java)

| Original `storage.ts` method | Java service method |
|------------------------------|---------------------|
| `getLibraryCategories()` | Load flat list `is_active=true`, build tree in service (Map by id + parentId) |
| `getLibraryCategory(id)` | Find category + children + entries |
| `createLibraryCategory` / `update` | `save()` + set `updatedAt` |
| `deleteLibraryCategory(id)` | `isActive = false` |
| `getLibraryEntries(categoryId?)` | Repository query + map to DTO with joined category & user |
| `createLibraryEntry` | Set `createdById` from principal; duplicate check → throw `DuplicateEntryException` → 409 |
| `deleteLibraryEntry(id)` | Soft delete |
| `searchLibraryEntries` | Repository `search()` + enrich DTOs |
| `incrementLibraryEntryUsage` | `@Modifying` increment query |
| `getConnectedEntries(entryId)` | Custom query, bidirectional, order by `strength DESC` |
| `createLibraryEntryConnection` | `save()`; catch unique violation in batch |
| `deleteLibraryEntryConnection` | `deleteById` (hard delete) |
| `deleteAllLibraryEntryConnections` | `deleteByFromEntryIdOrToEntryId` |

### 7.5 Controller endpoint map

```java
@RestController
@RequestMapping("/api/library")
@PreAuthorize("isAuthenticated() and !hasRole('ACCOUNTANT')")
public class LibraryEntryController {

  @GetMapping("/entries")
  public List<LibraryEntryWithDetailsDto> list(@RequestParam(required = false) Long categoryId) { ... }

  @PostMapping("/entries")
  public ResponseEntity<LibraryEntryDto> create(
      @Valid @RequestBody LibraryEntryRequest body,
      @AuthenticationPrincipal UserPrincipal user) { ... }

  @PostMapping("/bulk-entries")
  public ResponseEntity<BulkImportResultDto> bulkImport(
      @RequestBody BulkImportRequest body,
      @AuthenticationPrincipal UserPrincipal user) { ... }

  @GetMapping("/search")
  public List<LibraryEntryWithDetailsDto> search(
      @RequestParam String q,
      @RequestParam(required = false) Long categoryId) { ... }

  @PostMapping("/entries/{id}/increment-usage")
  public ResponseEntity<Void> incrementUsage(@PathVariable Long id) {
    entryService.incrementUsage(id);
    return ResponseEntity.noContent().build(); // 204
  }

  @PostMapping("/entries/connected-bulk")
  public Map<Long, List<ConnectedEntryDto>> connectedBulk(@RequestBody ConnectedBulkRequest body) { ... }
}
```

### 7.6 Bulk import service (`LibraryBulkImportService`)

Port the Node algorithm verbatim:

1. Validate non-empty `entries`.
2. Require `categoryId` OR any row with non-blank `domain`.
3. Reject rows with `subdomain` but no `domain`.
4. `Set<String> existingTitles` — all active titles lowercased/trimmed.
5. `Map<String, Long> categoryCache` — keys `root::{domain}` and `{parentId}::{subdomain}`.
6. Loop rows: skip duplicate → resolve/create category → validate → save → update sets.
7. Return `BulkImportResultDto` with counts + per-row errors.

Use `@Transactional` on the bulk method. For large imports, consider batch `saveAll` in chunks.

### 7.7 Exception → HTTP status mapping

| Condition | HTTP | Response body |
|-----------|------|---------------|
| Validation failure | `400` | `{ "message": "Invalid entry data", "errors": [...] }` |
| Duplicate title (single create) | `409` | `{ "message": "Duplicate entry", "error": "..." }` |
| Not found | `404` | `{ "message": "..." }` |
| Accountant / unauthorized | `403` | `{ "message": "Forbidden" }` |
| Unauthenticated | `401` | standard |

Use `@ControllerAdvice` + `@ExceptionHandler` for consistent JSON errors.

### 7.8 Database migrations

Use **Flyway** or **Liquibase** SQL migrations (not Hibernate `ddl-auto=update` in production).

```sql
-- V1__library_tables.sql (abbreviated)
CREATE TABLE library_categories (
  id BIGSERIAL PRIMARY KEY,
  name TEXT NOT NULL,
  description TEXT,
  parent_id BIGINT REFERENCES library_categories(id),
  sort_order INT DEFAULT 0,
  is_active BOOLEAN DEFAULT TRUE,
  created_at TIMESTAMPTZ DEFAULT NOW(),
  updated_at TIMESTAMPTZ DEFAULT NOW()
);

CREATE TABLE library_entries (
  id BIGSERIAL PRIMARY KEY,
  category_id BIGINT NOT NULL REFERENCES library_categories(id) ON DELETE CASCADE,
  title TEXT NOT NULL,
  content TEXT NOT NULL,
  tags TEXT[],
  created_by_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  sort_order INT DEFAULT 0,
  usage_count INT DEFAULT 0,
  is_active BOOLEAN DEFAULT TRUE,
  created_at TIMESTAMPTZ DEFAULT NOW(),
  updated_at TIMESTAMPTZ DEFAULT NOW()
);

CREATE TABLE library_entry_connections (
  id BIGSERIAL PRIMARY KEY,
  from_entry_id BIGINT NOT NULL REFERENCES library_entries(id) ON DELETE CASCADE,
  to_entry_id BIGINT NOT NULL REFERENCES library_entries(id) ON DELETE CASCADE,
  connection_type VARCHAR(20) NOT NULL,
  description TEXT,
  strength INT DEFAULT 1,
  is_active BOOLEAN DEFAULT TRUE,
  created_by_id BIGINT NOT NULL REFERENCES users(id),
  created_at TIMESTAMPTZ DEFAULT NOW(),
  updated_at TIMESTAMPTZ DEFAULT NOW(),
  UNIQUE (from_entry_id, to_entry_id, connection_type)
);
```

Optional unique index on `LOWER(TRIM(title))` where `is_active = true` for DB-level duplicate enforcement.

### 7.9 Original Node reference (ClientHubAI only)

If you need to compare behavior against the source app:

| Layer | ClientHubAI path |
|-------|------------------|
| Routes | `server/routes.ts` (~8891–9369) |
| Storage | `server/storage.ts` (~3925–4172) |
| Schema | `shared/schema.ts` (~852–903) |

---

## 8. Frontend — React `/library` Page

> **Port from ClientHubAI** with minimal changes if the Java API matches §6 exactly. Smart Connect logic stays **100% client-side** — no Java code needed for suggestions.

**Source files to copy/adapt from ClientHubAI:**

| Component | ClientHubAI path |
|-----------|------------------|
| Library page | `client/src/pages/library.tsx` |
| Smart Connect panel | `client/src/components/library/SmartConnectPanel.tsx` |
| Smart Connect hook | `client/src/hooks/use-smart-connect.ts` |
| Connected entries hook | `client/src/hooks/use-connected-entries.ts` |
| Session notes + picker | `client/src/components/session-notes/session-notes-manager.tsx` |

### 8.0 React porting checklist

- [ ] Point API base URL to Java backend (e.g. `VITE_API_URL=http://localhost:8080`)
- [ ] Ensure auth token attached to all `/api/library/*` requests (Bearer JWT or session cookie)
- [ ] Confirm JSON is **camelCase** (`categoryId`, `usageCount`, not snake_case)
- [ ] Keep TanStack Query keys identical: `["/api/library/entries"]`, etc.
- [ ] Remove hardcoded `createdById: 6` from `EntryForm` — omit field; Java sets from principal
- [ ] CORS enabled on Java for React origin
- [ ] `increment-usage` expects **204 No Content** (no JSON body)

**Example API client (Vite + fetch):**

```typescript
// lib/apiClient.ts
const API_BASE = import.meta.env.VITE_API_URL ?? 'http://localhost:8080';

export async function apiRequest(path: string, method: string, body?: unknown) {
  const token = localStorage.getItem('accessToken'); // or your auth store
  const res = await fetch(`${API_BASE}${path}`, {
    method,
    headers: {
      'Content-Type': 'application/json',
      ...(token ? { Authorization: `Bearer ${token}` } : {}),
    },
    body: body ? JSON.stringify(body) : undefined,
    credentials: 'include', // if using session cookies instead of JWT
  });
  if (!res.ok) throw new Error(await res.text());
  return res;
}
```

### 8.1 State

| State | Purpose |
|-------|---------|
| `activeTab` | Current root category ID (string) |
| `searchQuery` | Search input; debounced 300ms |
| `showAddEntryDialog` / `showBulkAddDialog` | Dialog visibility |
| `editingEntry` | Entry being edited (null = create) |
| `selectedEntries` | `Set<number>` for bulk delete |
| `connectedEntriesMap` | `Record<entryId, ConnectedEntry[]>` from connected-bulk |
| `visibleConnectionsByEntry` | Progressive disclosure (show 10, load more) |

### 8.2 TanStack Query keys

| Query key | Endpoint |
|-----------|----------|
| `["/api/library/categories"]` | `GET /api/library/categories` |
| `["/api/library/entries", categoryId]` | `GET /api/library/entries?categoryId=` |
| `["/api/library/entries"]` | `GET /api/library/entries` (all — for connections + Smart Connect) |
| `["/api/library/search", query, categoryId]` | `GET /api/library/search?q=&categoryId=` |

### 8.3 Entry list display (per card)

- Checkbox, title, content preview (2-line clamp)
- Category badge, tags (max 2 shown + "+N more")
- "Used N times" (from `usageCount`)
- Connection badge + expandable connected-entries section (search + category filter within connections)
- Edit / Delete buttons
- Sort: `sortOrder` ASC, then natural sort on title (`ANX1, ANX2, ANX10`)

### 8.4 `BulkAddForm` (inline component)

**Client validation before API:**
- Composite + Content columns must be mapped
- Subdomain without Domain → blocked
- No Domain/Subdomain and no `categoryId` → blocked
- Per row: title + content required (shown in preview as invalid)

**Import payload decision:**
```typescript
const useCategoryMode = !hasDomainMapping && !hasSubdomainMapping && !!categoryId;

if (useCategoryMode) {
  body = { categoryId, entries: validEntries.map(e => ({ title: e.title, content: e.content })) };
} else {
  body = { entries: validEntries.map(e => ({ domain, subdomain, title, content })) };
}
```

### 8.5 `EntryForm` (inline component)

| Field | Notes |
|-------|-------|
| title | required |
| content | required, 6-row textarea |
| categoryId | hierarchical select (`"—".repeat(level)` indent) |
| tags | comma-separated → split/trim array on submit |
| sortOrder | number, default 0 |
| createdById | **omit on create** — Java backend sets from authenticated user |

**Create mutation sequence:**
1. `POST /api/library/entries`
2. If `selectedConnections.length > 0` → `POST /api/library/connections/batch`

### 8.6 `SmartConnectPanel` + `use-smart-connect.ts`

**Clinical title pattern:** `[CONDITION][TYPE][NUMBER]_[VARIANT]`

| Part | Meaning | Examples |
|------|---------|----------|
| CONDITION | 2–5 letters | ANX, DEPR, PTSD |
| TYPE | S=Symptom, I=Intervention, P=Progress, G=Goal | |
| NUMBER | Pathway 1–99 | |
| VARIANT | Optional `_1`, `_2` | |

Regex (strict, in hook): `/^([A-Z]{3,5})([SIPG])(\d+)(?:_(\d+))?$/`

**Matching priority:**
1. Pattern match (100%) — same condition + pathway, different type
2. Keyword match (60%) — related categories only
3. Manual catalog — remaining entries

On submit, selected connection IDs become batch connections with `connectionType: "relates_to"`, `strength: 4`.

### 8.7 Route registration (`App.tsx`)

```typescript
<Route path="/library" component={() => {
  if (isAccountant(user)) return <AccessRestricted ... />;
  if (!isAdminOrSupervisor(user)) return <AccessRestricted ... />;
  return <LibraryPage />;
}} />
```

Nav item under Administration for `admin`, `administrator`, `supervisor` only.

---

## 9. Frontend — Session Notes `LibraryPicker`

**File:** `client/src/components/session-notes/session-notes-manager.tsx` (inline `LibraryPicker` component)

### 9.1 Field → category mapping (hardcoded)

```typescript
const categoryIds = {
  'session-focus': 1,
  'symptoms': 2,
  'short-term-goals': 3,
  'interventions': 4,
  'progress': 5
};
```

### 9.2 Cross-field connection filter chain

```typescript
const fieldRelationships = {
  'session-focus': [],
  'symptoms': [],
  'short-term-goals': ['symptoms'],
  'interventions': ['symptoms', 'short-term-goals'],
  'progress': ['symptoms', 'short-term-goals', 'interventions']
};
```

When "Show connected entries only" is ON:
1. Collect entry IDs from `selectedLibraryEntries` for relevant prior fields.
2. `useConnectedEntries(previouslySelectedIds)` → `POST /api/library/entries/connected-bulk`.
3. Filter connected results to current field's `categoryId`.
4. Only show entries whose ID is in that filtered connected set.

### 9.3 Selection handler

```typescript
const handleSelect = (entry: LibraryEntry) => {
  onSelect(entry.content, entry.id);  // parent appends content to textarea
  setIsOpen(false);
  incrementUsageMutation.mutate(entry.id);  // POST increment-usage
  setSelectedLibraryEntries(prev => ({
    ...prev,
    [fieldType]: [...prev[fieldType], entry.id]
  }));
};
```

Parent `onSelect` appends with `\n\n` separator:
```typescript
const newValue = currentValue ? `${currentValue}\n\n${content}` : content;
field.onChange(newValue);
```

### 9.4 Picker list UI (each row)

- Title (font-medium)
- Content (2-line clamp)
- Tags (up to 3 badges)
- "Used Nx" (right-aligned)

### 9.5 `use-connected-entries.ts` hook

```typescript
export const useConnectedEntries = (selectedIds: number[]) => {
  return useQuery({
    queryKey: ['/api/library/entries/connected-bulk', selectedIds.sort().join(',')],
    queryFn: async () => {
      if (selectedIds.length === 0) return [];
      const response = await apiRequest('/api/library/entries/connected-bulk', 'POST', { entryIds: selectedIds });
      const connectionsMap = await response.json();
     	const allConnected = Object.values(connectionsMap).flat();
      return dedupeById(allConnected);
    },
    enabled: selectedIds.length > 0,
    staleTime: 5 * 60 * 1000,
  });
};
```

### 9.6 Session note save (no library FK)

`onSubmit` → `POST /api/session-notes` or `PUT /api/session-notes/:id` with plain text in `sessionFocus`, `symptoms`, `shortTermGoals`, `intervention`, `progress`, etc.

**Hosted in:** `client/src/pages/client-detail.tsx` → `<SessionNotesManager clientId={...} sessions={...} />`

---

## 10. Access Control

| Role | `/library` page | `/api/library/*` | Session note Library picker |
|------|-----------------|------------------|----------------------------|
| Admin / Supervisor | ✅ | ✅ | ✅ |
| Therapist | ❌ | ✅ | ✅ |
| Accountant | ❌ | ❌ (`blockAccountant`) | ❌ |

---

## 11. Edge Cases & Known Quirks

| Issue | Current behavior | Recommendation when porting |
|-------|------------------|----------------------------|
| `createdById: 6` hardcoded in ClientHubAI EntryForm | Bulk correctly uses auth user | **Java:** always set from principal; **React:** omit from POST body |
| Category IDs 1–5 hardcoded in session notes | Breaks if categories reordered | Use slug/name lookup or config map |
| No `library_entry_id` on session notes | No audit of which entries were used | Add junction table if needed |
| `ConnectionForm` in library.tsx | Defined but never rendered | Skip or implement |
| Category add/edit dialogs | Wired but no UI trigger | Add admin UI or seed via migration |
| Duplicate titles | Case-insensitive trim | Keep same rule for consistency |
| Connection strength UI | Smart Connect uses 1–5 | Align all UI to 1–5 |
| Search ordering | Server: `usage_count DESC`; page: natural title sort | Document both layers |
| Bulk delete | N parallel DELETEs | Acceptable; add bulk endpoint if scale requires |

---

## 12. Seed Data (recommended for new project)

Create 5 root categories in this order (matches hardcoded session-note IDs):

| ID | Name | Used by session note field |
|----|------|---------------------------|
| 1 | Session Focus | `session_focus` |
| 2 | Symptoms | `symptoms` |
| 3 | Short-term Goals | `short_term_goals` |
| 4 | Intervention | `intervention` |
| 5 | Progress | `progress` |

Optional: seed subdomain categories under each domain for bulk import domain mode.

---

## 13. Rebuild Checklist

### Phase 1 — Java backend (database & API)
- [ ] Flyway/Liquibase migration: `library_categories`, `library_entries`, `library_entry_connections`
- [ ] JPA entities + repositories (§7.2–7.3)
- [ ] DTOs with Jakarta validation (§5.5)
- [ ] Services: category tree builder, entry CRUD, search, increment usage (§7.4)
- [ ] `LibraryBulkImportService` with category resolution cache (§7.6)
- [ ] Controllers matching §6 paths exactly; camelCase JSON (§4.1)
- [ ] Spring Security: authenticated + block accountant role (§6)
- [ ] `@ControllerAdvice` for 400/409/404 responses
- [ ] CORS for React dev/prod origins
- [ ] Seed 5 root categories (§12)

### Phase 2 — React admin UI (`/library`)
- [ ] Port `library.tsx` + `SmartConnectPanel` + hooks from ClientHubAI
- [ ] Wire API client to Java base URL
- [ ] Category tab navigation
- [ ] Entry list with search, sort, usage count, connections
- [ ] `BulkAddForm` with column mapping + preview
- [ ] `EntryForm` with Smart Connect panel (no `createdById` in payload)
- [ ] Bulk delete (checkbox selection)
- [ ] Role-gate route to Admin/Supervisor only

### Phase 3 — React session notes integration
- [ ] Port `LibraryPicker` from `session-notes-manager.tsx`
- [ ] Hardcoded or config-driven category mapping (prefer config/API)
- [ ] Connected-entries filter with field relationship chain
- [ ] Append content + `POST increment-usage` on select
- [ ] Session note save stores text only in `session_notes` columns

### Phase 4 — Verify (end-to-end against Java API)
- [ ] Bulk import: domain mode creates categories + entries
- [ ] Bulk import: category tab mode assigns to fixed category
- [ ] Duplicate skip in bulk; 409 on manual create
- [ ] Smart Connect → batch connections (201 with created/skipped counts)
- [ ] Session note picker filters by category
- [ ] Connected-only filter chains across fields
- [ ] `usageCount` increments on pick, visible on admin page
- [ ] Therapist blocked from `/library` route but can use picker
- [ ] Accountant gets 403 on all `/api/library/*`

---

## 14. Source File Index

### React frontend (port from ClientHubAI)

| Purpose | ClientHubAI path |
|---------|------------------|
| Admin page | `client/src/pages/library.tsx` |
| Smart Connect UI | `client/src/components/library/SmartConnectPanel.tsx` |
| Smart Connect logic | `client/src/hooks/use-smart-connect.ts` |
| Connected entries hook | `client/src/hooks/use-connected-entries.ts` |
| Routing / nav | `client/src/App.tsx` |
| Session notes + LibraryPicker | `client/src/components/session-notes/session-notes-manager.tsx` |
| Client page host | `client/src/pages/client-detail.tsx` |
| API client | `client/src/lib/queryClient.ts` |

### Java backend (create new — suggested names)

| Purpose | Suggested path |
|---------|----------------|
| Category controller | `.../library/controller/LibraryCategoryController.java` |
| Entry controller | `.../library/controller/LibraryEntryController.java` |
| Connection controller | `.../library/controller/LibraryConnectionController.java` |
| Bulk import service | `.../library/service/LibraryBulkImportService.java` |
| Entry service | `.../library/service/LibraryEntryService.java` |
| Repositories | `.../library/repository/Library*Repository.java` |
| Entities | `.../library/entity/Library*Entity.java` |
| DTOs | `.../library/dto/*.java` |
| Flyway SQL | `src/main/resources/db/migration/V*__library_tables.sql` |
| Exception handler | `.../common/GlobalExceptionHandler.java` |

### ClientHubAI backend reference (behavior spec only)

| Layer | Path |
|-------|------|
| Routes | `server/routes.ts` (~8891–9369) |
| Storage | `server/storage.ts` (~3925–4172) |
| Schema | `shared/schema.ts` (~852–903) |

---

## 15. AI Agent Rebuild Prompt

```
You are building a Clinical Content Library for a HIPAA-sensitive therapy app.

STACK:
- Frontend: React + TanStack Query (port UI from ClientHubAI reference)
- Backend: Java Spring Boot 3 + Spring Data JPA + PostgreSQL
- API contract: REST JSON, camelCase field names, paths under /api/library/*

GOAL: Reusable clinical text entries in hierarchical categories, bulk spreadsheet
import, manual CRUD with Smart Connect (client-side suggestions + server-side
connection batch save), and session-note library pickers that COPY text into
structured note fields.

DATA MODEL (PostgreSQL):
- library_categories (id, name, description, parent_id, sort_order, is_active)
- library_entries (id, category_id, title, content, tags text[], created_by_id,
  sort_order, usage_count, is_active)
- library_entry_connections (id, from_entry_id, to_entry_id, connection_type,
  strength 1-5, description, is_active, created_by_id)
- session_notes: session_focus, symptoms, short_term_goals, intervention, progress
  as plain TEXT — NO library_entry_id FK

SEED 5 ROOT CATEGORIES (IDs 1-5): Session Focus, Symptoms, Short-term Goals,
Intervention, Progress

JAVA BACKEND:
- @RestController at /api/library/* with @PreAuthorize authenticated + !ACCOUNTANT
- Set createdById from SecurityContext, never trust client
- Flyway migrations for tables
- LibraryBulkImportService: domain/subdomain category auto-create, duplicate skip,
  return { total, successful, skipped, failed, categoriesCreated, errors[] }
- increment-usage → 204 No Content
- connections/batch: catch DataIntegrityViolationException, skip duplicates
- connected-bulk: POST { entryIds } → Map<Long, List<ConnectedEntryDto>>
- search: ILIKE title+content, order by usage_count DESC
- Jackson camelCase JSON matching React types

REACT FRONTEND (port from ClientHubAI):
- /library page: Admin/Supervisor only; tabs, bulk paste import, entry CRUD
- Smart Connect: 100% client-side (use-smart-connect.ts), pattern ANXS10 etc.
- Session note LibraryPicker on 5 fields; filter by categoryId; connected-only
  chain Goals←Symptoms, Interventions←Symptoms+Goals, Progress←all three
- On pick: append content with \n\n; POST increment-usage; track entry IDs in state
- TanStack Query keys: ["/api/library/entries"], ["/api/library/categories"], etc.

VERIFY: Java API matches React expectations (camelCase, status codes 201/204/409),
bulk import, duplicate handling, usage_count increment, role gates.
```

---

*Last verified against ClientHubAI codebase: June 2026*
*Target port stack: React frontend + Java (Spring Boot) backend*
