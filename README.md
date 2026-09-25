# SmartHub TherapyFlow - Full-Stack Practice Management & EHR Platform

**A secure, scalable, HIPAA-compliant Electronic Health Record (EHR) & Practice Management System (PMS) for therapy practices, clinics, and health providers.**

[Monorepo Architecture](https://www.google.com/search?q=%2523-monorepo-structure&utm_source=gemini) • [Frontend Guide](https://www.google.com/search?q=%2523-frontend-module&utm_source=gemini) • [Backend Guide](https://www.google.com/search?q=%2523-backend-module&utm_source=gemini) • [Quick Start](https://www.google.com/search?q=%2523-quick-start&utm_source=gemini) • [Security & Compliance](https://www.google.com/search?q=%2523-security--hipaa-compliance&utm_source=gemini)

---

## 📋 Table of Contents

* [Overview](https://www.google.com/search?q=%2523-overview&utm_source=gemini)
* [System Architecture & Features](https://www.google.com/search?q=%2523-system-architecture--features&utm_source=gemini)
* [Monorepo Structure](https://www.google.com/search?q=%2523-monorepo-structure&utm_source=gemini)
* [Quick Start](https://www.google.com/search?q=%2523-quick-start&utm_source=gemini)
* [Frontend Module](https://www.google.com/search?q=%2523-frontend-module&utm_source=gemini)
* [Backend Module](https://www.google.com/search?q=%2523-backend-module&utm_source=gemini)
* [Environment Configuration](https://www.google.com/search?q=%2523-environment-configuration&utm_source=gemini)
* [API Documentation](https://www.google.com/search?q=%2523-api-documentation&utm_source=gemini)
* [Security & HIPAA Compliance](https://www.google.com/search?q=%2523-security--hipaa-compliance&utm_source=gemini)
* [License & Support](https://www.google.com/search?q=%2523-license--support&utm_source=gemini)

---

## 🎯 Overview

SmartHub TherapyFlow is a comprehensive, enterprise-grade therapy practice management system designed to streamline clinical operations and patient outcomes for mental health providers. The platform integrates client self-service portals, role-based workflows, automated billing, timezone-aware scheduling, clinical form builders, and AI-assisted clinical documentation.

### Core Highlights

* **Full-Stack Integration:** React 19 single-page application backed by Spring Boot RESTful services.
* **Role-Based Workflows:** Tailored interfaces for Clients, Therapists, Supervisors, Admins, and Billing Specialists.
* **280+ RESTful API Endpoints:** Complete backend domain model and controller architecture.
* **HIPAA-Compliant Design:** Audit logging with Hibernate Envers, payload encryption, and granular RBAC.
* **Telehealth & Integrations:** Native Zoom API integration for remote care and Stripe for billing.
* **AI Clinical Copilot:** OpenAI-assisted session notes, clinical summaries, and dynamic auto-completions.

---

## ✨ System Architecture & Features

```
┌────────────────────────────────────────────────────────────────────────┐
│                        React 19 Frontend Client                        │
│         (Client Portal / Therapist Dashboard / Admin Console)          │
└───────────────────────────────────┬────────────────────────────────────┘
                                    │ HTTPS / RESTful APIs / JWT
┌───────────────────────────────────▼────────────────────────────────────┐
│                        Spring Boot 3.5.7 Backend                       │
│    ┌──────────────┬──────────────┬───────────────┬────────────────┐    │
│    │ Auth & RBAC  │ Scheduling   │ Billing/Stripe│ AI Copilot     │    │
│    └──────────────┴──────────────┴───────────────┴────────────────┘    │
└───────────────┬───────────────────┬───────────────────┬────────────────┘
                │                   │                   │
   ┌────────────▼─────────┐ ┌───────▼────────┐ ┌────────▼────────┐
   │ PostgreSQL 18 DB     │ │ Redis Cache    │ │ External Services│
   │ (Flyway Migrations)  │ │ (Session State)│ │ (Stripe, Zoom)   │
   └──────────────────────┘ └────────────────┘ └──────────────────┘

```

### Core Features

* **👥 User & Identity Management:** JWT-based authentication with refresh tokens, multi-organization role switching, and MFA enforcement.
* **🏥 Client Lifecycle Management:** 9-tab client profile, intake tracking, duplicate detection, and privacy consent management.
* **📅 Scheduling & Telehealth:** Conflict-aware booking engine, timezone transformations, business hours validation, and automated Zoom link provisioning.
* **💰 Billing & Automated Invoicing:** Automated claim generation, custom discount codes, payment status tracking, and Stripe webhooks.
* **📝 Clinical Documentation & AI:** Session note creation, template name mapping, risk-score calculation, and AI-powered transcript summary generators.
* **✅ Tasks & Checklists:** Custom checklist builders, client assignment tracking, and collaborative note commenting.

---

## 📁 Monorepo Structure

```
TherapyFlow/
├── frontend/                     # React 19 + TypeScript + Vite Client
│   ├── src/
│   │   ├── components/           # UI, layout, and domain-specific components
│   │   ├── pages/                # Client, Therapist, and Admin views
│   │   ├── routes/               # Role-guarded router definitions
│   │   └── utils/                # Date/time, note, and card state helpers
│   ├── package.json
│   └── vite.config.ts
├── backend/                      # Spring Boot 3.5.7 + Java 21 Engine
│   ├── src/main/java/com/smart/therapy/flow/
│   │   ├── admin/                # Platform management APIs
│   │   ├── ai/                   # OpenAI prompts and integration
│   │   ├── auth/                 # Spring Security & JWT filters
│   │   ├── billing/              # Invoice and payment processors
│   │   ├── client/               # Patient record management
│   │   └── session/              # Appointments and calendar engine
│   ├── src/main/resources/
│   │   ├── db/migration/         # Flyway schema scripts
│   │   └── application.yaml      # Master Spring configuration
│   └── pom.xml
└── docker-compose.yml            # Local development orchestration

```

---

## 🚀 Quick Start

### Prerequisites

Ensure the following dependencies are installed locally:

* **Node.js**: v20+ and `npm`
* **Java Development Kit (JDK)**: Java 21 or higher
* **Build Tool**: Maven 3.8+
* **Database**: PostgreSQL 18+ and Redis 6+ (or Docker)

### 1. Launch Support Services (Docker)

```bash
# Set a local PostgreSQL password
export POSTGRES_PASSWORD='your_local_secure_password'

# Start PostgreSQL and Redis containers
docker compose up -d

```

### 2. Launch the Backend API

```bash
cd backend

# Build and execute Spring Boot application
mvn clean install
mvn spring-boot:run

```

* The API server will boot at `http://localhost:8080`.

### 3. Launch the Frontend Development Server

Open a new terminal tab:

```bash
cd frontend

# Install Node dependencies
npm install

# Start Vite dev server
npm run dev

```

* The UI web application will be accessible at `http://localhost:5173`.

---

## 🖥️ Frontend Module

Built with modern web standards, the frontend provides responsive, accessible interfaces for all platform user types.

### Tech Stack

* **Framework:** React 19
* **Language:** TypeScript
* **Bundler:** Vite
* **Styling:** Tailwind CSS 4, Class Variance Authority
* **Form & Validation:** React Hook Form, Zod
* **UI Primitives:** Radix UI, Lucide Icons

### Available Scripts

Run from the `frontend/` directory:

```bash
npm run dev      # Start development server on port 5173
npm run build    # Compile production bundle
npm run preview  # Local preview of production build
npm run lint     # Run ESLint rules check

```

---

## ☕ Backend Module

The backend service handles system logic, persistence, authorization, and third-party integrations using an enterprise Spring architecture.

### Tech Stack

* **Framework:** Spring Boot 3.5.7
* **Language:** Java 21
* **Database Engine:** PostgreSQL 18 with Hibernate 6.6 / JPA
* **Database Migrations:** Flyway 11.7
* **Security:** Spring Security 6.5, JWT, BCrypt
* **Caching:** Spring Data Redis

### Maven Commands

Run from the `backend/` directory:

```bash
mvn compile              # Compile source code
mvn test                 # Execute unit and integration tests
mvn package -DskipTests  # Build executable JAR file

```

---

## ⚙️ Environment Configuration

### Frontend Settings (`frontend/.env`)

```env
VITE_API_URL=http://localhost:8080/api

```

### Backend Settings (`backend/src/main/resources/application-local.yml`)

```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/therapy_flow
    username: postgres
    password: your_local_secure_password
  redis:
    host: localhost
    port: 6379

jwt:
  secret: YOUR_GENERATED_JWT_SECRET_KEY_HERE
  expiration-ms: 86400000

stripe:
  secret-key: sk_test_your_stripe_key

```

---

## 📚 API Documentation

When the backend server is running, you can access the OpenAPI/Swagger documentation to inspect and test all endpoints:

* **Swagger UI:** `http://localhost:8080/swagger-ui.html`
* **OpenAPI Specs:** `http://localhost:8080/v3/api-docs`

---

## 🔒 Security & HIPAA Compliance

SmartHub TherapyFlow is designed according to regulatory security principles for Health Information Technology:

1. **Access Control:** Role-Based Access Control (RBAC) enforced on both API and UI routes.
2. **Audit Logging:** System modification tracking powered by Hibernate Envers.
3. **Data Security:** Strict encryption standards for data at rest and TLS 1.3 for data in transit.
4. **Secret Protection:** Automated pre-commit scans prevent confidential keys from being pushed to source control.

---

## 📄 License & Support

This software package is proprietary. All rights reserved.

For technical assistance or system inquiries, reach out to the development team:

* **Support Email:** support@therapyflow.com
