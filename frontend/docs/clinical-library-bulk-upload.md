# Clinical Library Bulk Upload — Frontend Integration Guide

> **Audience:** Frontend / QA  
> **Backend endpoint:** `POST /api/v1/library/bulk-entries`  
> **Last updated:** 2026-06-24

---

## 1. Concept (read this first)

Bulk upload is **paste from Excel/CSV → JSON POST**. There is **no file upload**.

### Two separate ideas — do not mix them

| Concept | Source | Purpose |
|---------|--------|---------|
| **Filing category** | Active library tab → `categoryId` | Where the **new entry** is stored |
| **Domain / Subdomain** | Optional columns in pasted CSV | **Connection targets** (Smart Connect), not filing |

```
Bulk upload on Session Focus tab (categoryId: 1)
├── categoryId: 1              → new entry lives under Session Focus
├── title + content            → the new library entry
├── domain: Anxiety            → find/create Anxiety category (connection graph)
└── subdomain: Cognitive       → find/create Cognitive under Anxiety
                                 → find/create entry titled "Cognitive" there
                                 → CONNECT Session Focus entry ↔ Cognitive entry
```

**Domain/subdomain do not change where the new row is filed.**

---

## 2. UI flow (Bulk Add modal)

| Step | User action | Frontend |
|------|-------------|----------|
| 1 | Open **Clinical Library**, select tab (e.g. Session Focus) | Pass `categoryId` + `categoryName` into modal |
| 2 | Click **Bulk Add** | Open `BulkAddModal` |
| 3 | Paste TAB or CSV data | Parse with `parsePastedRows()` |
| 4 | Toggle **First row is header** | Skip row 1 when mapping if checked |
| 5 | Map columns | Domain / Subdomain / Composite (Title) / Content / Skip |
| 6 | Preview valid/invalid rows | Only **valid** rows go in POST body |
| 7 | Click **Import** | `POST /api/v1/library/bulk-entries` |
| 8 | Show results | Toast + results step with counts |
| 9 | Refresh lists | Refetch categories, entries, entries/with-connections |

### Related files

| File | Role |
|------|------|
| `src/pages/admin/content/library/index.tsx` | Tab, `handleBulkImport`, refetch, toast |
| `src/components/admin-library-sections/BulkAddModal.tsx` | Paste → map → preview → import |
| `src/utils/libraryBulkImport.ts` | Parser, validation, payload builder |
| `src/store/api/admin/libraryEntries.api.ts` | `bulkImportLibraryEntries` mutation |

---

## 3. Pasted CSV format

### Delimiter

- First line contains `\t` → **TAB** (Excel paste)
- Otherwise → **comma** (quoted CSV supported)

### Column mapping

| Map as | Header aliases (case-insensitive) | Required in paste? |
|--------|-----------------------------------|--------------------|
| **Composite (Title)** | composite, title, name, entry, code, label | **Yes** |
| **Content** | content, description, text, definition, body, notes | **Yes** |
| **Domain (connection)** | domain, category | No (optional) |
| **Subdomain (connection)** | subdomain, sub-domain, subcategory | No (optional) |
| **Skip** | anything else | — |

### Minimum paste examples

**Title + Content only** (no connections):

```csv
Composite,Content
ANXS10,Client reports excessive worry about daily activities
```

**With connection columns:**

```csv
Domain,Subdomain,Composite,Content
Anxiety,Cognitive,Cognitive Restructuring,A technique to identify and challenge negative automatic thoughts
```

---

## 4. Client validation (before POST)

Import is **disabled** unless all pass:

| Rule | Message (example) |
|------|-------------------|
| Active tab / `categoryId` exists | "Select a category tab before importing." |
| Composite (Title) column mapped | "Map Composite (Title) and Content columns…" |
| Content column mapped | (same) |
| Subdomain mapped without Domain column | "Subdomain cannot be mapped without Domain." |
| Row missing title or content | Row marked **Invalid** in preview — **not sent** |
| Row has subdomain cell but no domain on same row | Invalid — not sent |

### What is NOT required

- Domain column (connections are optional)
- Subdomain column
- `tags`, `sortOrder` (not sent by UI today)

---

## 5. Expected request payload

### Always required at root

| Field | Type | Source |
|-------|------|--------|
| `categoryId` | `number` | **Active library tab** |
| `entries` | `array` | Non-empty; valid rows only |

### Case A — Tab only (no Domain column mapped)

```json
{
  "categoryId": 1,
  "entries": [
    { "title": "ANXS10", "content": "Client reports excessive worry..." }
  ]
}
```

### Case B — Tab + connection

```json
{
  "categoryId": 1,
  "entries": [
    {
      "title": "Cognitive Restructuring",
      "content": "A technique to identify and challenge negative automatic thoughts",
      "domain": "Anxiety",
      "subdomain": "Cognitive"
    }
  ]
}
```

---

## 6. Expected response (`201 Created`)

```json
{
  "total": 2,
  "successful": 2,
  "skipped": 0,
  "failed": 0,
  "categoriesCreated": 3,
  "connectionsCreated": 2,
  "errors": []
}
```

| Field | Meaning |
|-------|---------|
| `successful` | Entries created under `categoryId` |
| `categoriesCreated` | New connection-side categories |
| `connectionsCreated` | Smart Connect links created |
| `skipped` | Duplicate title (global) |
| `errors[].row` | 1-based index in sent `entries` array |

---

## 7. After import — refetch

- `GET /api/v1/library/categories`
- `GET /api/v1/library/entries?categoryId=`
- `GET /api/v1/library/entries/with-connections?categoryId=`

---

## 8. Quick reference

```
POST /api/v1/library/bulk-entries

Required:
  categoryId     ← active tab
  entries[].title
  entries[].content

Optional (connections):
  entries[].domain
  entries[].subdomain
```
