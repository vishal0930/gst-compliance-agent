# GST Compliance Agent

An agentic AI system for Indian SMB GST compliance. Upload invoices, let the AI pipeline parse, classify, reconcile, and generate GSTR-3B drafts — all automatically.

---

## Table of Contents
1. [What it Does](#what-it-does)
2. [Tech Stack](#tech-stack)
3. [Project Structure](#project-structure)
4. [Prerequisites & Setup](#prerequisites--setup)
5. [Environment Configuration](#environment-configuration)
6. [AI Pipeline Flow](#ai-pipeline-flow)
7. [Frontend UI Customizations (Dynamic Pill Badges)](#frontend-ui-customizations-dynamic-pill-badges)
8. [API Reference Documentation](#api-reference-documentation)
    - [Authentication](#authentication)
    - [Invoices](#invoices)
    - [GSTR-2B](#gstr-2b)
    - [Reconciliation](#reconciliation)
    - [Return Drafts](#return-drafts)
    - [Deadlines](#deadlines)

---

## What it Does

1. **Invoice Upload** — Upload a PDF or image invoice. The system creates a placeholder record and kicks off the pipeline.
2. **Invoice Parsing** — `InvoiceParserAgent` extracts vendor name, GSTIN, invoice number, date, amounts, and line items using Apache Tika + Ollama LLM.
3. **HSN Classification** — `HsnClassifierAgent` matches each line item to an HSN code using vector similarity search (pgvector + nomic-embed-text) and LLM re-ranking.
4. **GSTR-2B Reconciliation** — `Gstr2bReconcilerAgent` compares your book invoices against the uploaded GSTR-2B portal data, flags mismatches, and calculates ITC at risk.
5. **Deadline Tracking** — `DeadlineTrackerAgent` calculates GSTR-1, GSTR-3B, and GSTR-9 filing deadlines with penalty projections.
6. **Return Draft** — `ReturnDrafterAgent` generates a GSTR-3B compliance brief using the LLM, with action items derived from reconciliation results.

---

## Tech Stack

### Backend
* **Runtime**: Java 21
* **Framework**: Spring Boot 3.3
* **Database**: PostgreSQL 16 + pgvector
* **Migrations**: Flyway
* **ORM**: Spring Data JPA / Hibernate
* **Cache**: Redis 7
* **File Storage**: MinIO (S3-compatible)
* **AI / LLM**: LangChain4j 0.31 + Ollama (llama3.2:3b)
* **Embeddings**: Ollama nomic-embed-text
* **Security**: Spring Security + JWT
* **Text Extraction**: Apache Tika

### Frontend
* **Framework**: React 19 + Vite 8
* **UI Library**: Ant Design 6
* **Styling**: Tailwind CSS 4 + Custom Premium Dark/Light Themes
* **State Management**: Zustand 5
* **Data Fetching**: TanStack Query v5
* **HTTP Client**: Axios
* **Charts**: Recharts
* **Routing**: React Router v7

---

## Project Structure

```
gst-compliance-agent/
├── docker/
│   └── docker-compose.yml          # PostgreSQL, Redis, MinIO
├── frontend/                       # React + Vite SPA
│   └── src/
│       ├── api/                    # Axios API clients
│       ├── components/             # Reusable UI components
│       ├── hooks/                  # TanStack Query hooks
│       ├── pages/                  # Route-level pages
│       ├── store/                  # Zustand stores
│       └── utils/                  # Formatters, validators
└── gst-compliance/                 # Spring Boot backend
    └── src/main/java/com/gstcompliance/
        ├── agent/                  # AI agents (Parser, HSN, Reconciler, etc.)
        ├── batch/                  # Spring Batch jobs
        ├── config/                 # Spring configs (Security, MinIO, LLM, etc.)
        ├── controller/             # REST controllers
        ├── dto/                    # Request/Response DTOs
        ├── model/                  # JPA entities
        ├── pipeline/               # AgentPipelineService, PipelineState
        └── repository/             # Spring Data JPA repositories
```

---

## Prerequisites & Setup

### Requirements
* Java 21+
* Node.js 18+
* Docker + Docker Compose
* [Ollama](https://ollama.ai) running locally

### 1. Pull required Ollama models
```bash
ollama pull llama3.2:3b
ollama pull nomic-embed-text
```

### 2. Start infrastructure (Docker)
```bash
cd docker
docker compose up -d
```
This starts PostgreSQL (`localhost:5433`), Redis (`localhost:6379`), and MinIO (`localhost:9000`).

### 3. Start the backend
```bash
cd gst-compliance
./mvnw spring-boot:run
```
The API will run on `http://localhost:8080`.

### 4. Start the frontend
```bash
cd frontend
npm install
npm run dev
```
The UI will be accessible at `http://localhost:5173`.

---

## Environment Configuration

### Backend (`application.properties`)
* `spring.datasource.url`: `jdbc:postgresql://localhost:5433/gst_db`
* `spring.data.redis.host`: `localhost`
* `aws.s3.endpoint`: `http://localhost:9000`
* `jwt.secret`: JWT signing token
* `ollama.embedding.url`: `http://localhost:11434/api/embeddings`

### Frontend (`.env`)
```env
VITE_API_BASE_URL=/api/v1
```

---

## AI Pipeline Flow

```
POST /api/v1/invoices/upload
        │
        ▼
  Invoice placeholder saved (status: PENDING)
        │
        ▼
  AgentPipelineService.runPipeline()  [async]
        │
        ├── Step 1: InvoiceParserAgent (Tika OCR → LLM extraction)
        │
        ├── Step 2: HsnClassifierAgent (Similarity Search → LLM re-rank)
        │
        ├── Step 2.5: persistLineItems()
        │
        ├── Step 3: Gstr2bReconcilerAgent (Compare books vs GSTR-2B)
        │
        ├── Step 4: DeadlineTrackerAgent (Calculate GSTR-1, 3B, 9 deadlines)
        │
        └── Step 5: ReturnDrafterAgent (LLM Compliance Brief)
```

Poll job status via `GET /api/v1/invoices/jobs/{jobId}`.

---

## Frontend UI Customizations (Dynamic Pill Badges)

The frontend features a premium custom badge UI designed to look beautiful on the dark background. Instead of basic white text, dynamic information is represented as light-pastel badges with high-contrast dark text, styled like pill-shaped containers:

1. **Alternating Row Themes**:
   - Each row is color-coordinated with a rotating color theme based on its index (`green`, `blue`, `orange`, `purple`, `gold`, `magenta`).
   - Standard text fields like `Invoice #`, `Supplier Name`, `GSTIN`, and `Invoice Date` render inside badges sharing the row's theme.

2. **Critical Overrides**:
   - **Negative Values**: If a currency amount is negative (like refunds/credits), it automatically overrides to a **red/rose** warning badge.
   - **Needs Review / Failures**: Badges override to **red** if the status is `FAILED` or needs manual review.
   - **Confidence**: Percentages under 80% are colored **orange**, and under 50% are colored **red**.

---

## API Reference Documentation

**Base URL:** `http://localhost:8080/api/v1`  
**Authentication:** All endpoints (except `/auth/**`) require `Authorization: Bearer <token>`.

### Authentication

#### Register a new user
`POST /auth/register`
* **Request Body**:
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
* **Response `200`**:
```json
{
  "success": true,
  "message": "User registered successfully",
  "data": { "id": "uuid", "email": "owner@business.com", "businessName": "FabIndia Textiles Pvt. Ltd.", "gstin": "09FABIN1234E1Z8" }
}
```

#### Login
`POST /auth/login`
* **Request Body**:
```json
{ "email": "owner@business.com", "password": "SecurePass123" }
```
* **Response `200`**:
```json
{
  "success": true,
  "message": "Login successful",
  "data": { "token": "eyJhbGciOiJIUz...", "expiresIn": 86400000 }
}
```

---

### Invoices

#### Upload Invoice File
`POST /invoices/upload` (`multipart/form-data`)
* **Form fields**: `file` (PDF, PNG, or JPG)
* **Response `200`**:
```json
{ "jobId": "a3f2c1d0-...", "invoiceId": "b7e4d2a1-...", "status": "QUEUED" }
```

#### Poll pipeline job status
`GET /invoices/jobs/{jobId}`
* **Response `200`**:
```json
{ "jobId": "a3f2c1d0-...", "status": "COMPLETED", "completed": true, "allAgentsSuccessful": true, "errors": [] }
```

#### List invoices
`GET /invoices` (Accepts `page`, `size`, `sort`)
* **Response `200`**:
```json
{
  "data": {
    "content": [
      {
        "id": "uuid",
        "vendorName": "FabIndia",
        "vendorGstin": "09FABIN1234E1Z8",
        "invoiceNumber": "FAB-2026-006",
        "invoiceDate": "2026-01-30",
        "taxableValue": 237500.00,
        "totalAmount": 250000.00,
        "totalGst": 12500.00,
        "parseStatus": "DONE",
        "confidenceScore": 0.95,
        "lineItems": [...]
      }
    ],
    "totalElements": 42
  }
}
```

#### Delete Invoice
`DELETE /invoices/{id}`
* **Response `200`**:
```json
{ "success": true, "message": "Invoice deleted successfully", "data": null }
```

---

### GSTR-2B

#### Upload GSTR-2B data
`POST /gstr2b/upload`
* **Request Body**: Array of invoice records from GSTR-2B.
* **Response `201`**:
```json
{
  "data": { "totalProcessed": 1, "successfulCount": 1, "failedCount": 0, "results": [...] }
}
```

#### Fetch GSTR-2B summary
`GET /gstr2b/summary?month=1&year=2026`
* **Response `200`**:
```json
{
  "data": { "invoiceCount": 12, "taxableValue": 1850000.00, "cgst": 46250.00, "sgst": 46250.00, "igst": 0.00, "totalItc": 92500.00 }
}
```

---

### Reconciliation

#### Run GSTR-2B reconciliation
`POST /reconciliation/run`
* **Request Body**: `{ "month": 1, "year": 2026 }`
* **Response `200`**:
```json
{
  "data": {
    "period": "01-2026",
    "totalInvoices": 12,
    "matchedCount": 10,
    "mismatchCount": 2,
    "itcAtRisk": 8500.00,
    "mismatches": [
      {
        "status": "AMOUNT_MISMATCH",
        "invoiceNumber": "FAB-2026-006",
        "riskAmount": 125.00,
        "description": "Amount mismatch: Book ₹237500.00 vs Portal ₹235000.00",
        "recommendation": "Verify the invoice amount with supplier"
      }
    ]
  }
}
```

#### Resolve mismatch
`POST /reconciliation/{id}/resolve/{mismatchId}`
* **Request Body** (plain text): Reason for resolving mismatch.
* **Response `200`**:
```json
{ "data": { "resolved": true, "note": "Confirmed with supplier", "message": "Mismatch resolved successfully" } }
```

---

### Return Drafts

#### Generate return draft (GSTR-3B)
`POST /returns/draft`
* **Request Body**: `{ "month": 1, "year": 2026 }`
* **Response `200`**:
```json
{ "data": { "period": "01-2026", "status": "GENERATING", "message": "Draft is being generated..." } }
```

#### List drafts
`GET /returns`
* **Response `200`**: Array of generated briefs and drafts with tax liabilities and action items.

#### Approve draft
`POST /returns/{id}/approve`
* **Response `200`**:
```json
{ "data": { "status": "APPROVED", "message": "Return draft approved successfully" } }
```

---

### Deadlines

#### Get upcoming deadlines
`GET /deadlines/upcoming?month=1&year=2026`
* **Response `200`**:
```json
{
  "data": [
    {
      "formType": "GSTR-3B",
      "dueDate": "2026-02-20",
      "daysRemaining": 20,
      "isOverdue": false,
      "priority": "MEDIUM",
      "penaltyPerDay": 0,
      "totalPenalty": 0,
      "daysOverdue": 0
    }
  ]
}
```
