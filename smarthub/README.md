# TherapyFlow - Enterprise Therapy Practice Management System

<div align="center">

![Java](https://img.shields.io/badge/Java-21-orange.svg)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.5.7-brightgreen.svg)
![PostgreSQL](https://img.shields.io/badge/PostgreSQL-18-blue.svg)
![License](https://img.shields.io/badge/license-Proprietary-red.svg)

**A secure, scalable, HIPAA-compliant Electronic Health Record (EHR) & Practice Management System (PMS) for therapy practices, clinics, and health providers.**

[Features](#-features) • [Technology Stack](#-technology-stack) • [Quick Start](#-quick-start) • [Documentation](#-documentation) • [Contributing](#-contributing)

</div>

---

## 📋 Table of Contents

- [Overview](#-overview)
- [Features](#-features)
- [Technology Stack](#-technology-stack)
- [Architecture](#-architecture)
- [Quick Start](#-quick-start)
- [Configuration](#-configuration)
- [API Documentation](#-api-documentation)
- [Development](#-development)
- [Security](#-security)
- [Deployment](#-deployment)
- [Contributing](#-contributing)
- [License](#-license)

---

## 🎯 Overview

TherapyFlow is a comprehensive, enterprise-grade therapy practice management system designed to streamline operations for mental health providers. The system supports multiple user roles, client portals, billing, scheduling, clinical documentation, and AI-powered features.

### Key Highlights

- ✅ **280+ RESTful API Endpoints** - Complete backend implementation
- ✅ **Role-Based Access Control (RBAC)** - Granular permissions system
- ✅ **HIPAA-Compliant** - Audit logging, encryption, and security best practices
- ✅ **Client Portal** - Self-service portal for clients
- ✅ **AI Integration** - OpenAI-powered clinical documentation and suggestions
- ✅ **Payment Processing** - Stripe integration for secure payments
- ✅ **Video Conferencing** - Zoom integration for telehealth sessions
- ✅ **Multi-tenant Ready** - Scalable architecture for multiple practices

---

## ✨ Features

### Core Modules

#### 👥 **User Management & Authentication**
- JWT-based authentication with refresh tokens
- Multi-role support (Client, Therapist, Supervisor, Admin, Billing Specialist, Super Admin)
- Password reset and account activation flows
- Session management and activity tracking
- Audit logging for compliance

#### 🏥 **Client Management**
- Comprehensive client profiles with 9-tab interface
- Client history tracking
- Duplicate detection system
- Privacy & consent management (GDPR/HIPAA compliant)
- Client portal with self-service capabilities

#### 📅 **Session Management**
- Advanced scheduling system with conflict detection
- Business hours validation
- Timezone-aware scheduling
- Zoom integration for telehealth
- Session notes with AI-powered templates
- Availability management

#### 💰 **Billing & Payments**
- Automated billing generation
- Stripe payment integration
- Invoice management
- Payment tracking and reconciliation
- Discount and adjustment support

#### 📝 **Clinical Documentation**
- Session notes with AI assistance
- Assessment templates and assignments
- Clinical forms system
- Document management (Azure Blob Storage / AWS S3 / Local)
- Library system for clinical resources

#### ✅ **Task & Checklist Management**
- Task assignment and tracking
- Checklist templates
- Client-specific checklists
- Task comments and collaboration

#### 🔔 **Notifications**
- Email notifications (SparkPost integration)
- In-app notifications
- Scheduled notifications
- Notification preferences

#### 🤖 **AI Features**
- AI-powered session note generation
- Clinical template suggestions
- Connected resource recommendations
- Field auto-completion

---

## 🛠 Technology Stack

### Backend
- **Framework**: Spring Boot 3.5.7
- **Language**: Java 21
- **Build Tool**: Maven
- **Database**: PostgreSQL 18
- **ORM**: Hibernate 6.6.33 / JPA
- **Migrations**: Flyway 11.7.2

### Security
- **Authentication**: JWT (JSON Web Tokens)
- **Authorization**: Spring Security 6.5.6
- **Password Hashing**: BCrypt
- **Audit**: Hibernate Envers

### Caching & Performance
- **Cache**: Redis / Spring Data Redis
- **Connection Pooling**: HikariCP

### External Integrations
- **Payment**: Stripe API
- **Email**: SparkPost API / Spring Mail
- **Video Conferencing**: Zoom API (OAuth 2.0)
- **AI**: OpenAI API
- **Storage**: Azure Blob Storage / AWS S3

### Development Tools
- **API Documentation**: SpringDoc OpenAPI (Swagger)
- **Logging**: Logback / SLF4J
- **Monitoring**: Spring Boot Actuator / Micrometer
- **Testing**: JUnit 5, Mockito

---

## 🏗 Architecture

### Project Structure

```
therapy-flow-mi6/
├── src/main/java/com/smart/therapy/flow/
│   ├── admin/              # Admin-specific controllers
│   ├── ai/                 # AI service integration
│   ├── assessment/         # Assessment management
│   ├── auth/               # Authentication & authorization
│   ├── billing/            # Billing & invoicing
│   ├── client/             # Client management & portal
│   ├── common/             # Shared utilities & configs
│   ├── document/           # Document & form management
│   ├── integration/        # External service integrations
│   ├── notification/       # Notification system
│   ├── payment/            # Payment processing
│   ├── session/            # Session scheduling
│   ├── system/             # System configuration
│   ├── task/               # Task & checklist management
│   └── user/               # User profile management
├── src/main/resources/
│   ├── application.yaml    # Main configuration
│   ├── application-dev.yaml
│   ├── application-local.yml
│   └── db/migration/       # Flyway migrations
└── pom.xml
```

### Design Patterns

- **Layered Architecture**: Controller → Service → Repository
- **DTO Pattern**: Request/Response DTOs for API contracts
- **Repository Pattern**: Spring Data JPA repositories
- **Service Layer**: Business logic encapsulation
- **Exception Handling**: Global exception handler with custom exceptions
- **Audit Pattern**: BaseEntity with automatic audit fields

---

## 🚀 Quick Start

### Prerequisites

- **Java**: JDK 21 or higher
- **Maven**: 3.8+ 
- **PostgreSQL**: 12+ (or use Docker)
- **Redis**: 6+ (optional, for caching)
- **Git**: For version control

### Installation

1. **Clone the repository**
   ```bash
   git clone <repository-url>
   cd therapy-flow-mi6
   ```

2. **Configure database**
   ```bash
   # Update src/main/resources/application.yaml with your database credentials
   ```

3. **Set environment variables**
   ```bash
   # Required for Stripe (optional for development)
   export STRIPE_SECRET_KEY=sk_test_your_key_here
   ```

4. **Build the project**
   ```bash
   mvn clean install
   ```

5. **Run the application**
   ```bash
   mvn spring-boot:run
   ```

   Or use your IDE to run `Application.java`

6. **Access the application**
   - API: http://localhost:8080
   - Swagger UI: http://localhost:8080/swagger-ui.html
   - Actuator: http://localhost:8080/actuator

### Docker Setup (Optional)

```bash
# Choose a local-only development password (do not commit it)
export POSTGRES_PASSWORD='replace-with-a-local-password'

# Start PostgreSQL and Redis. Their ports bind to 127.0.0.1 only.
docker compose up -d

# Run the application
mvn spring-boot:run
```

Override `POSTGRES_USER`, `POSTGRES_DB`, `POSTGRES_PORT`, or `REDIS_PORT` if the
defaults conflict with other local services. Docker Compose refuses to start
until `POSTGRES_PASSWORD` is set.

---

## ⚙️ Configuration

### Application Profiles

The application supports multiple profiles:

- **`local`**: Local development (default)
- **`dev`**: Development environment
- **`prod`**: Production environment

### Key Configuration Files

- `application.yaml` - Base configuration
- `application-local.yml` - Local development settings
- `application-dev.yaml` - Development environment settings

### Environment Variables

| Variable | Description | Required |
|----------|-------------|----------|
| `STRIPE_SECRET_KEY` | Stripe API secret key | No (for payments) |
| `SPARKPOST_API_KEY` | SparkPost API key | No (for emails) |
| `REDIS_HOST` | Redis host | No (default: localhost) |
| `REDIS_PORT` | Redis port | No (default: 6379) |
| `DB_URL` | Database URL | Yes (in dev/prod) |
| `DB_USERNAME` | Database username | Yes (in dev/prod) |
| `DB_PASSWORD` | Database password | Yes (in dev/prod) |
| `JWT_SECRET` | JWT signing secret | Yes |

### Database Configuration

The application uses Flyway for database migrations. Migrations are located in `src/main/resources/db/migration/`.

**Important**: Ensure Flyway is enabled in your profile:
```yaml
spring:
  flyway:
    enabled: true
    locations: classpath:db/migration
    baseline-on-migrate: true
```

---

## 📚 API Documentation

### Swagger UI

Once the application is running, access the interactive API documentation at:
```
http://localhost:8080/swagger-ui.html
```

### API Endpoints Overview

#### Authentication (`/api/auth`)
- `POST /api/auth/login` - User login
- `POST /api/auth/refresh` - Refresh JWT token
- `POST /api/auth/logout` - User logout

#### Clients (`/api/clients`)
- `GET /api/clients` - List clients
- `POST /api/clients` - Create client
- `GET /api/clients/{id}` - Get client details
- `PUT /api/clients/{id}` - Update client
- `DELETE /api/clients/{id}` - Delete client

#### Sessions (`/api/sessions`)
- `GET /api/sessions` - List sessions
- `POST /api/sessions` - Create session
- `GET /api/sessions/{id}` - Get session details
- `PUT /api/sessions/{id}` - Update session
- `GET /api/sessions/availability` - Get availability

#### Billing (`/api/billing`)
- `GET /api/billing/invoices` - List invoices
- `POST /api/billing/invoices` - Create invoice
- `GET /api/billing/invoices/{id}` - Get invoice

#### Payments (`/api/stripe`)
- `POST /api/stripe/invoices/{invoiceId}/pay` - Initiate payment
- `POST /api/stripe/webhook` - Stripe webhook handler

#### Client Portal (`/api/portal`)
- `POST /api/portal/login` - Portal login
- `GET /api/portal/dashboard` - Portal dashboard
- `GET /api/portal/appointments` - Client appointments
- `GET /api/portal/documents` - Client documents
- `GET /api/portal/invoices` - Client invoices

**Full API documentation**: See Swagger UI for complete endpoint list with request/response schemas.

---

## 💻 Development

### Code Style

- Follow Java naming conventions
- Use Lombok annotations to reduce boilerplate
- Maintain consistent package structure
- Write meaningful commit messages

### Building

```bash
# Compile
mvn compile

# Run tests
mvn test

# Package JAR
mvn package

# Skip tests
mvn package -DskipTests
```

### Running Tests

```bash
# All tests
mvn test

# Specific test class
mvn test -Dtest=ClientServiceTest

# With coverage
mvn test jacoco:report
```

### Security and release evidence

For reproducible security, tenant-isolation, concurrency, and migration evidence,
run the disposable PostgreSQL/Redis harness with JDK 17:

```bash
python3 scripts/security-test-harness.py --output-dir qa-results/security-$(date -u +%Y%m%dT%H%M%SZ)
```

It performs fresh database bootstrap/teardown, verifies Redis available/unavailable/
recovery behavior, runs the focused Surefire and Failsafe suites, and writes
`summary.json` plus aggregate `junit.xml`. See
[`docs/testing/security-release-evidence.md`](docs/testing/security-release-evidence.md)
for focused unit, migration, security, integration, and concurrency commands.

### Database Migrations

Create a new migration:
```bash
# Create migration file: V{timestamp}__{description}.sql
# Example: V20241209__add_new_feature.sql
```

Migration files should be placed in `src/main/resources/db/migration/`

### Code Quality

- **Linting**: Configure your IDE with Checkstyle or SpotBugs
- **Formatting**: Use Google Java Format or similar
- **Documentation**: Javadoc for public APIs

---

## 🔒 Security

### Security Features

- ✅ JWT-based authentication
- ✅ Role-based access control (RBAC)
- ✅ Password encryption (BCrypt)
- ✅ SQL injection prevention (JPA/Hibernate)
- ✅ XSS protection
- ✅ CSRF protection
- ✅ Audit logging
- ✅ Rate limiting (Bucket4j)
- ✅ Secure session management

### Security Best Practices

1. **Never commit secrets** - Use environment variables
2. **Use HTTPS in production** - Configure SSL/TLS
3. **Regular security updates** - Keep dependencies updated
4. **Audit logs** - Monitor access and changes
5. **Input validation** - Validate all user inputs

### HIPAA Compliance

- Audit logging for all data access
- Encryption at rest and in transit
- Access controls and permissions
- Data retention policies
- Privacy & consent management

---

## 🚢 Deployment

### Production Checklist

- [ ] Set `spring.profiles.active=prod`
- [ ] Configure production database
- [ ] Set all required environment variables
- [ ] Enable HTTPS/TLS
- [ ] Configure CORS properly
- [ ] Set up monitoring and logging
- [ ] Configure backup strategy
- [ ] Review security settings
- [ ] Load test the application
- [ ] Set up CI/CD pipeline

### Docker Deployment

```dockerfile
FROM openjdk:21-jdk-slim
WORKDIR /app
COPY target/therapy-flow-*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
```

### Environment-Specific Configuration

Use Spring profiles for environment-specific settings:
- Development: `application-dev.yaml`
- Production: `application-prod.yaml`

---

## 📊 Monitoring & Health Checks

### Actuator Endpoints

- **Health**: `/actuator/health`
- **Metrics**: `/actuator/metrics`
- **Info**: `/actuator/info`

### Logging

Logs are configured via Logback. Log levels can be adjusted in `application.yaml`:

```yaml
logging:
  level:
    root: INFO
    com.smart.therapy.flow: DEBUG
    org.hibernate.SQL: DEBUG
```

---

## 🤝 Contributing

We welcome contributions! Please follow these guidelines:

1. **Fork the repository**
2. **Create a feature branch** (`git checkout -b feature/amazing-feature`)
3. **Commit your changes** (`git commit -m 'Add amazing feature'`)
4. **Push to the branch** (`git push origin feature/amazing-feature`)
5. **Open a Merge Request**

### Contribution Guidelines

- Follow the existing code style
- Write tests for new features
- Update documentation as needed
- Ensure all tests pass
- Request review before merging

---

## 📄 License

This project is proprietary software. All rights reserved.

---

## 📞 Support

For support, please contact:
- **Email**: support@therapyflow.com
- **Documentation**: [Internal Wiki]
- **Issues**: Create an issue in GitLab

---

## 🙏 Acknowledgments

- Spring Boot team for the excellent framework
- All open-source contributors whose libraries make this project possible
- The therapy community for feedback and requirements

---

<div align="center">

**Built with ❤️ for the therapy community**

[⬆ Back to Top](#therapyflow---enterprise-therapy-practice-management-system)

</div>
