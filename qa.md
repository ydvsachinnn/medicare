# ❓ Architecture Q&A — MediCare Plus Hospital Management System

### **Q: Is this project a Monolithic or Microservices-based architecture?**

### **A: It is a Modular Monolithic Architecture.**

---

### 🏛️ Detailed Architectural Breakdown

The **MediCare Plus** application is designed as a **Modular Monolith** built on Spring Boot 4.x and Java 21.

#### 1. Why it is a Monolith:
- **Single Deployment Artifact:** All components (Patient Management, Doctor OPD, Appointment System, Prescriptions, Lab Reports, and Neura AI Chatbot) are compiled into a **single executable JAR file** (`pepclass-0.0.1-SNAPSHOT.jar`) and run inside a single Docker container / Web Service on Render.
- **Shared Unified Database:** The entire application shares a single MongoDB database (`pepclass`), with collections accessed through Spring Data Repositories within the same process.
- **Single Codebase & Process:** Controllers, Services, Security Filters, and Thymeleaf UI templates run within the same JVM instance.
- **Disabled Service Discovery:** Although Eureka/Spring Cloud dependencies are included in `pom.xml`, discovery services are explicitly disabled (`eureka.client.enabled=false`, `spring.cloud.discovery.enabled=false`), confirming the application operates as a standalone unit.

---

### 🧩 Core Modules Inside the Monolith:

| Module | Responsibility | Key Classes |
|--------|----------------|-------------|
| **Core & Security** | Role-based authentication (Chairman, Doctor, Patient), Spring Security | `SecurityConfig`, `CustomUserDetailsService` |
| **Patient & OPD** | Registration, appointments, doctor scheduling | `PatientController`, `AppointmentController` |
| **Clinical Records** | Prescriptions, PDF generation, lab test reports | `PrescriptionController`, `ReportController` |
| **AI Healthcare Assistant** | Neura AI Chatbot powered by `gemini-3.6-flash` | `ChatController`, `ChatService`, `GeminiService` |
| **Notifications** | Dosage alerts & automated email dispatch | `NotificationController`, `EmailService` |

---

### 💡 Key Benefits of this Monolithic Approach for this System:
1. **Simplified Deployment:** One-click Docker deployment to Render with zero inter-service network latency or service mesh overhead.
2. **ACID Transactions & Simpler Data Integrity:** Direct database operations without needing distributed sagas or complex eventual consistency patterns.
3. **Low Maintenance:** Easy local testing (`./mvnw spring-boot:run`) without needing Docker Compose for multiple microservice containers.

---

## 🛠️ How to Convert MediCare Plus to a Microservices Architecture

To decompose this Monolith into a scalable **Microservices Architecture**, follow this 5-phase roadmap:

```
                    ┌─────────────────────────┐
                    │   Spring Cloud Gateway  │ (Port 8080)
                    └────────────┬────────────┘
                                 │
     ┌───────────────────────────┼───────────────────────────┐
     ▼                           ▼                           ▼
┌──────────────┐      ┌─────────────────────┐     ┌──────────────────────┐
│  Auth & User │      │ Patient & OPD       │     │ Clinical & EHR       │
│  Service     │      │ Service             │     │ Service              │
│ (Port 8081)  │      │ (Port 8082)         │     │ (Port 8083)          │
└──────────────┘      └─────────────────────┘     └──────────────────────┘
                                 │
                                 ▼
                      ┌─────────────────────┐
                      │  Neura AI Chatbot   │
                      │  Service            │
                      │ (Port 8084)         │
                      └─────────────────────┘
```

---

### Phase 1: Split into Independent Domain Services (Bounded Contexts)

Decompose the core packages into 4 distinct Spring Boot Maven sub-projects:

1. **`user-auth-service` (Port 8081)**
   - **Responsibility:** Registration, Login, JWT Token Issuance, Password Reset OTP.
   - **DB:** `auth_db` (`users`, `otps`, `roles`)

2. **`patient-opd-service` (Port 8082)**
   - **Responsibility:** Patient registration records, Doctor schedules, Appointment booking.
   - **DB:** `patient_opd_db` (`patients`, `appointments`, `doctors`)

3. **`clinical-ehr-service` (Port 8083)**
   - **Responsibility:** Digital Prescriptions, Lab Reports (CBC, LFT, HbA1c), Medicines inventory.
   - **DB:** `clinical_db` (`prescriptions`, `medical_reports`, `medicines`)

4. **`ai-chatbot-service` (Port 8084)**
   - **Responsibility:** Neura AI Assistant, prompt synthesis, `gemini-3.6-flash` API integration, user query quota tracking.
   - **DB:** `chatbot_db` (`chat_history`, `user_quotas`)

---

### Phase 2: Add API Gateway & Service Discovery

1. **Service Registry (Netflix Eureka Server - Port 8761):**
   - Enable `@EnableEurekaServer` so all microservices register dynamically.
   - Enables logical routing (e.g. `http://PATIENT-OPD-SERVICE/api/appointments`).

2. **API Gateway (Spring Cloud Gateway - Port 8080):**
   - Single public entry point for the frontend UI.
   - Routes requests to downstream microservices and enforces global JWT authentication headers.

---

### Phase 3: Inter-Service Communication

1. **Synchronous Calls (Spring Cloud OpenFeign):**
   - Enable `@EnableFeignClients` for direct HTTP calls between services (e.g., `ai-chatbot-service` verifying patient identity with `user-auth-service`).
2. **Asynchronous Messaging (RabbitMQ / Apache Kafka):**
   - When an appointment is booked in `patient-opd-service`, publish an `AppointmentBookedEvent` to asynchronously trigger email dispatches.

---

### Phase 4: Database Decoupling (Database-Per-Service)
- Remove shared entity collections.
- Each service connects to its own isolated database instance or logical database URI (`auth_db`, `patient_opd_db`, `clinical_db`, `chatbot_db`).

---

### Phase 5: Container Orchestration (`docker-compose.yml`)
Create a root `docker-compose.yml` to spin up all microservices and databases with a single command:
```bash
docker-compose up --build
```
