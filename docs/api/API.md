# GST Compliance Agent — API Reference

**Base URL:** `http://localhost:8080/api/v1`  
**Auth:** All endpoints except `/auth/**` require `Authorization: Bearer <token>`.  
**Content-Type:** `application/json` unless noted.

All responses are wrapped in:

```json
{
  "success": true,
  "message": "Human-readable message",
  "data": { ... }
}
```

Errors follow:

```json
{
  "success": false,
  "message": "Error description",
  "errorCode": "ERROR_CODE"
}
```

---

## Authentication

### Register

```
POST /auth/register
```

**Body**
```json
{
  "email": "owner@business.com",
  "password": "SecurePass123",
  "businessName": "FabIndia Textiles Pvt. Ltd.",
  "gstin": "09FABIN1234E1Z8",
  "stateCode": "09",
  "phone": "9876543210",
  "turnoverSlab": "BELOW_5CR"
}
```

`turnoverSlab` values: `BELOW_5CR`, `ABOVE_5CR`

**Response `200`**
```json
{
  "success": true,
  "message": "User registered successfully",
  "data": {
    "id": "uuid",
    "email": "owner@business.com",
    "businessName": "FabIndia Textiles Pvt. Ltd.",
    "gstin": "09FABIN1234E1Z8"
  }
}
```

**Errors**
- `400 DUPLICATE_EMAIL` — email already registered

---

### Login

```
POST /auth/login
```

**Body**
```json
{
  "email": "owner@business.com",
  "password": "SecurePass123"
}
```

**Response `200`**
```json
{
  "success": true,
  "message": "Login successful",
  "data": {
    "token": "eyJhbGciOiJIUzI1NiJ9...",
    "expiresIn": 86400000
  }
}
```

---

### Get current user

```
GET /auth/me
Authorization: Bearer <token>
```

**Response `200`**
```json
{
  "data": {
    "id": "uuid",
    "email": "owner@business.com",
    "businessName": "FabIndia Textiles Pvt. Ltd.",
    "gstin": "09FABIN1234E1Z8",
    "stateCode": "09",
    "turnoverSlab": "BELOW_5CR",
    "role": "ROLE_USER"
  }
}
```

---

## Invoices

### Upload invoice

```
POST /invoices/upload
Content-Type: multipart/form-data
Authorization: Bearer <token>
```

**Form fields**
| Field | Type | Description |
|---|---|---|
| `file` | file | PDF, PNG, or JPG invoice |

**Response `200`**
```json
{
  "jobId": "a3f2c1d0-...",
  "invoiceId": "b7e4d2a1-...",
  "status": "QUEUED"
}
```

Use `jobId` to poll pipeline progress. Use `invoiceId` to fetch the invoice once done.

---

### Poll job status

```
GET /invoices/jobs/{jobId}
Authorization: Bearer <token>
```

**Response `200` — processing**
```json
{
  "jobId": "a3f2c1d0-...",
  "status": "CLASSIFYING",
  "completed": false
}
```

**Response `200` — done**
```json
{
  "jobId": "a3f2c1d0-...",
  "status": "COMPLETED",
  "completed": true,
  "allAgentsSuccessful": true,
  "errors": []
}
```

**Response `200` — failed**
```json
{
  "jobId": "a3f2c1d0-...",
  "status": "FAILED",
  "completed": false,
  "errors": ["Invoice parse failed: ..."]
}
```

Pipeline `status` values (in order): `PENDING` → `PARSING` → `CLASSIFYING` → `RECONCILING` → `DEADLINE_TRACKING` → `DRAFTING` → `COMPLETED` / `FAILED`

**Response `404`** — job not found or expired (server restarted)

---

### List invoices

```
GET /invoices
Authorization: Bearer <token>
```

**Query params**
| Param | Type | Default | Description |
|---|---|---|---|
| `page` | int | 0 | Page number (0-indexed) |
| `size` | int | 20 | Page size |
| `sort` | string | `createdAt,desc` | Sort field and direction |

**Response `200`**
```json
{
  "data": {
    "content": [
      {
        "id": "uuid",
        "vendorName": "FabIndia Textiles Pvt. Ltd.",
        "vendorGstin": "09FABIN1234E1Z8",
        "invoiceNumber": "FAB-2026-006",
        "invoiceDate": "2026-01-30",
        "taxableValue": 237500.00,
        "totalAmount": 250000.00,
        "totalGst": 12500.00,
        "cgstAmount": 6250.00,
        "sgstAmount": 6250.00,
        "igstAmount": 0.00,
        "parseStatus": "DONE",
        "confidenceScore": 0.95,
        "createdAt": "2026-01-30T14:22:00",
        "lineItems": [
          {
            "id": "uuid",
            "description": "Cotton Silk Saree",
            "quantity": 20,
            "unitPrice": 8000.00,
            "hsnCode": "52113150",
            "gstRate": 5.00,
            "taxableValue": 160000.00,
            "cgstAmount": 4000.00,
            "sgstAmount": 4000.00,
            "igstAmount": 0.00,
            "hsnConfidence": 0.92,
            "needsReview": false,
            "reviewReason": null
          }
        ]
      }
    ],
    "totalElements": 42,
    "totalPages": 3,
    "size": 20,
    "number": 0
  }
}
```

`parseStatus` values: `PENDING`, `PROCESSING`, `DONE`, `FAILED`, `MANUAL_REVIEW`

---

### Get invoice by ID

```
GET /invoices/{id}
Authorization: Bearer <token>
```

Returns the same shape as a single item in the list above, including `lineItems`.

**Errors**
- `404` — invoice not found or belongs to another user

---

### Delete invoice

```
DELETE /invoices/{id}
Authorization: Bearer <token>
```

**Response `200`**
```json
{ "success": true, "message": "Invoice deleted successfully", "data": null }
```

---

## GSTR-2B

### Upload GSTR-2B data

```
POST /gstr2b/upload
Authorization: Bearer <token>
```

**Body**
```json
{
  "invoices": [
    {
      "supplierName": "FabIndia Textiles Pvt. Ltd.",
      "supplierGstin": "09FABIN1234E1Z8",
      "buyerGstin": "27BUYER5678E1Z9",
      "invoiceNumber": "FAB-2026-006",
      "invoiceDate": "2026-01-30",
      "taxableValue": 237500.00,
      "cgst": 6250.00,
      "sgst": 6250.00,
      "igst": 0.00,
      "grandTotal": 250000.00,
      "lineItems": [
        {
          "description": "Cotton Silk Saree",
          "quantity": 20,
          "unitPrice": 8000.00,
          "hsnCode": "52113150",
          "gstRate": 5.00,
          "taxableValue": 160000.00,
          "cgstAmount": 4000.00,
          "sgstAmount": 4000.00,
          "igstAmount": 0.00
        }
      ]
    }
  ]
}
```

**Response `201`**
```json
{
  "data": {
    "totalProcessed": 1,
    "successfulCount": 1,
    "failedCount": 0,
    "results": [
      {
        "invoiceNumber": "FAB-2026-006",
        "success": true,
        "message": "Invoice uploaded successfully",
        "invoiceId": "uuid"
      }
    ],
    "timestamp": "2026-01-30T14:22:00"
  }
}
```

---

### List GSTR-2B invoices

```
GET /gstr2b/invoices?month=1&year=2026
Authorization: Bearer <token>
```

**Query params**
| Param | Type | Required | Description |
|---|---|---|---|
| `month` | int | yes | 1–12 |
| `year` | int | yes | e.g. 2026 |

**Response `200`** — array of GSTR-2B invoice objects

---

### GSTR-2B summary

```
GET /gstr2b/summary?month=1&year=2026
Authorization: Bearer <token>
```

**Response `200`**
```json
{
  "data": {
    "invoiceCount": 12,
    "taxableValue": 1850000.00,
    "cgst": 46250.00,
    "sgst": 46250.00,
    "igst": 0.00,
    "grandTotal": 1942500.00,
    "totalItc": 92500.00
  }
}
```

---

## Reconciliation

### Run reconciliation

```
POST /reconciliation/run
Authorization: Bearer <token>
```

**Body**
```json
{
  "month": 1,
  "year": 2026
}
```

`userEmail` is resolved server-side from the JWT — do not send it.

**Response `200`**
```json
{
  "data": {
    "period": "01-2026",
    "totalInvoices": 12,
    "matchedCount": 10,
    "mismatchCount": 2,
    "itcAtRisk": 8500.00,
    "summary": "2 mismatches found for period 01-2026...",
    "mismatches": [
      {
        "status": "AMOUNT_MISMATCH",
        "invoiceNumber": "FAB-2026-006",
        "supplierGstin": "09FABIN1234E1Z8",
        "bookAmount": 237500.00,
        "portalAmount": 235000.00,
        "diffAmount": 2500.00,
        "bookInvoiceDate": "2026-01-30",
        "portalInvoiceDate": "2026-01-30",
        "riskAmount": 125.00,
        "description": "Amount mismatch: Book ₹237500.00 vs Portal ₹235000.00 (1.06% diff)",
        "recommendation": "Verify the invoice amount with supplier"
      }
    ]
  }
}
```

**Mismatch `status` values**
| Value | Meaning |
|---|---|
| `AMOUNT_MISMATCH` | Taxable value differs beyond 1% threshold |
| `GST_MISMATCH` | Total GST differs |
| `DATE_MISMATCH` | Invoice date differs |
| `HSN_MISMATCH` | HSN code frequency differs |
| `LINE_ITEM_MISMATCH` | Line item count differs |
| `LINE_ITEM_DESCRIPTION_MISMATCH` | Item description mismatch |
| `LINE_ITEM_QUANTITY_MISMATCH` | Item quantity mismatch |
| `LINE_ITEM_PRICE_MISMATCH` | Unit price mismatch |
| `MISSING_IN_ERP` | In GSTR-2B but not in books |
| `MISSING_IN_GSTR2B` | In books but not in GSTR-2B |

**Errors**
- `400 RECONCILIATION_ERROR` — agent execution failed

---

### List reconciliation history

```
GET /reconciliation
Authorization: Bearer <token>
```

**Query params:** standard Spring `Pageable` (`page`, `size`, `sort`)

**Response `200`** — paged `ReconciliationRecord` list with `id`, `taxPeriod`, `status`, `matchedCount`, `mismatchCount`, `itcAtRisk`, `completedAt`

---

### Get reconciliation by ID

```
GET /reconciliation/{id}
Authorization: Bearer <token>
```

Returns the full reconciliation response including mismatches for a specific record.

---

### Get mismatches for a reconciliation

```
GET /reconciliation/{id}/mismatches?type=AMOUNT_MISMATCH
Authorization: Bearer <token>
```

**Query params**
| Param | Type | Required | Description |
|---|---|---|---|
| `type` | string | no | Filter by mismatch status. Omit or `all` for all types. |

---

### Resolve a mismatch

```
POST /reconciliation/{id}/resolve/{mismatchId}
Content-Type: text/plain
Authorization: Bearer <token>
```

**Body** (plain text)
```
Confirmed with supplier — amount corrected in revised invoice
```

`mismatchId` is the `invoiceNumber` of the mismatch.

**Response `200`**
```json
{
  "data": {
    "resolved": true,
    "note": "Confirmed with supplier — amount corrected in revised invoice",
    "message": "Mismatch resolved successfully"
  }
}
```

---

### Export reconciliation (Excel)

```
GET /reconciliation/{id}/export
Authorization: Bearer <token>
```

**Response** — binary `.xlsx` file download  
`Content-Disposition: attachment; filename=reconciliation-{id}.xlsx`

---

## Return Drafts

### Generate a return draft

```
POST /returns/draft
Authorization: Bearer <token>
```

**Body**
```json
{
  "month": 1,
  "year": 2026
}
```

Triggers async generation. The draft is saved to the database once the LLM finishes (typically 10–30 seconds).

**Response `200`**
```json
{
  "data": {
    "period": "01-2026",
    "status": "GENERATING",
    "message": "Return draft is being generated. Check the drafts list shortly."
  }
}
```

---

### List return drafts

```
GET /returns
Authorization: Bearer <token>
```

**Query params:** standard Spring `Pageable`

**Response `200`**
```json
{
  "data": {
    "content": [
      {
        "id": "uuid",
        "userId": "owner@business.com",
        "period": "01-2026",
        "brief": "Your GST compliance for January 2026 is strong...",
        "totalSales": 1850000.00,
        "totalGst": 92500.00,
        "totalItc": 84000.00,
        "taxLiability": 8500.00,
        "itcAtRisk": 8500.00,
        "isComplete": true,
        "isApproved": false,
        "generatedAt": "2026-01-31T10:00:00",
        "approvedAt": null,
        "actionItems": [
          {
            "title": "⚠️ 2 invoice(s) not in GSTR-2B. Contact suppliers.",
            "description": "⚠️ 2 invoice(s) not in GSTR-2B. Contact suppliers.",
            "priority": "MEDIUM",
            "isCompleted": false
          }
        ]
      }
    ],
    "totalElements": 3,
    "totalPages": 1
  }
}
```

---

### Get draft by ID

```
GET /returns/{id}
Authorization: Bearer <token>
```

Returns a single `ComplianceBriefResponse` (same shape as above).

---

### Get GSTR-3B draft data

```
GET /returns/{id}/gstr3b
Authorization: Bearer <token>
```

**Response `200`**
```json
{
  "data": {
    "id": "uuid",
    "period": "01-2026",
    "totalSales": 1850000.00,
    "totalGst": 92500.00,
    "totalItc": 84000.00,
    "taxLiability": 8500.00
  }
}
```

---

### Approve a draft

```
POST /returns/{id}/approve
Authorization: Bearer <token>
```

**Response `200`**
```json
{
  "data": {
    "status": "APPROVED",
    "message": "Return draft approved successfully"
  }
}
```

---

## Deadlines

### Get upcoming deadlines

```
GET /deadlines/upcoming?month=1&year=2026
Authorization: Bearer <token>
```

**Query params**
| Param | Type | Required | Description |
|---|---|---|---|
| `month` | int | no | Defaults to current month |
| `year` | int | no | Defaults to current year |

**Response `200`**
```json
{
  "data": [
    {
      "formType": "GSTR-3B",
      "dueDate": "2026-02-20",
      "daysRemaining": 20,
      "isOverdue": false,
      "priority": "MEDIUM",
      "description": "GSTR-3B filing",
      "penaltyPerDay": 0,
      "totalPenalty": 0,
      "daysOverdue": 0
    },
    {
      "formType": "GSTR-1",
      "dueDate": "2026-02-11",
      "daysRemaining": 11,
      "isOverdue": false,
      "priority": "HIGH",
      "description": "GSTR-1 filing",
      "penaltyPerDay": 0,
      "totalPenalty": 0,
      "daysOverdue": 0
    },
    {
      "formType": "GSTR-9",
      "dueDate": "2027-12-31",
      "daysRemaining": 700,
      "isOverdue": false,
      "priority": "MEDIUM",
      "description": "Annual return filing",
      "penaltyPerDay": 0,
      "totalPenalty": 0,
      "daysOverdue": 0
    }
  ]
}
```

**`priority` values:** `CRITICAL` (overdue), `HIGH` (≤3 days), `MEDIUM` (≤15 days), `LOW` (>15 days)

**Overdue response** (when `isOverdue: true`)
```json
{
  "formType": "GSTR-1",
  "dueDate": "2026-01-11",
  "daysRemaining": 0,
  "isOverdue": true,
  "priority": "CRITICAL",
  "penaltyPerDay": 25.00,
  "totalPenalty": 500.00,
  "daysOverdue": 20
}
```

---

## Common HTTP status codes

| Code | Meaning |
|---|---|
| `200` | Success |
| `201` | Created |
| `400` | Validation error or business rule violation |
| `401` | Missing or invalid JWT token |
| `403` | Authenticated but not authorised |
| `404` | Resource not found |
| `500` | Internal server error |

---

## Pagination

All list endpoints accept standard Spring Pageable parameters:

| Param | Example | Description |
|---|---|---|
| `page` | `0` | Zero-indexed page number |
| `size` | `20` | Items per page |
| `sort` | `createdAt,desc` | Field and direction |

Paged responses include:

```json
{
  "content": [...],
  "totalElements": 42,
  "totalPages": 3,
  "size": 20,
  "number": 0,
  "first": true,
  "last": false
}
```

---

## Data types

| Type | Format | Example |
|---|---|---|
| UUID | String (UUID v4) | `"a3f2c1d0-4b5e-6f7a-8b9c-0d1e2f3a4b5c"` |
| Date | ISO 8601 date | `"2026-01-30"` |
| DateTime | ISO 8601 datetime | `"2026-01-30T14:22:00"` |
| Currency | Decimal (2dp) | `237500.00` |
| GSTIN | 15-char string | `"09FABIN1234E1Z8"` |
| Period | `MM-YYYY` string | `"01-2026"` |
