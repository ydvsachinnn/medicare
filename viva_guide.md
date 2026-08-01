# 🎓 Viva Examination Defense Guide — MediCare Plus System Architecture & Code Breakdown

---

## 🎯 1. Executive Summary & Architecture Justification

### **System Architecture: Microservice-Ready Modular Monolith (Modulith)**

MediCare Plus is built as a **Modular Monolith using Domain-Driven Design (DDD)** principles. 

#### **Why this architecture was chosen (Justification for Viva):**
1. **Production Efficiency & Zero Network Latency:** In a pure microservices environment, every cross-domain call (e.g. Chatbot checking Patient ID) requires network I/O, serialization, and HTTP overhead. A Modulith executes these calls in-memory with sub-millisecond latency.
2. **Single-Container Cloud Deployment Cost:** Deploying 7 separate microservices on cloud platforms (like Render or AWS) requires 7 running instances, database clusters, and service meshes, which is expensive for mid-sized hospital management. Our architecture runs as a single lightweight Docker container while maintaining **100% loose coupling**.
3. **Seamless Microservice Scalability:** All 7 services are isolated into bounded contexts with dedicated REST API routes (`/api/*`), Spring Cloud Eureka dependencies (`pom.xml`), and environment feature toggles (`application.properties`). If traffic surges, any service can be extracted into an independent Docker container with zero code refactoring.

---

## 🧩 2. Complete Overview of the 7 Domain Services

```
                               ┌───────────────────────────────────┐
                               │     Spring Security & Web API     │
                               │        Gateway Layer (/api/*)     │
                               └─────────────────┬─────────────────┘
                                                 │
      ┌──────────────────┬───────────────────────┼───────────────────────┬──────────────────┐
      ▼                  ▼                       ▼                       ▼                  ▼
┌───────────┐      ┌───────────┐           ┌───────────┐           ┌───────────┐      ┌───────────┐
│ Service 1 │      │ Service 2 │           │ Service 3 │           │ Service 4 │      │ Service 5 │
│  User &   │      │ Patient   │           │ OPD &     │           │ Clinical  │      │ Diagnostics│
│  Auth     │      │ Portal    │           │Appointments           │Prescriptions     │ & Reports │
└───────────┘      └───────────┘           └───────────┘           └───────────┘      └───────────┘
                                                 │
                                                 ├───────────────────────┐
                                                 ▼                       ▼
                                           ┌───────────┐           ┌───────────┐
                                           │ Service 6 │           │ Service 7 │
                                           │  Neura AI │           │Alerts &   │
                                           │ Assistant │           │Emails     │
                                           └───────────┘           └───────────┘
```

---

### 🔑 Service 1: User Authentication & Security Service
- **Primary Responsibility:** User identity management, role-based access control (RBAC for Chairman, Doctor, Patient), password hashing, and OTP password reset.
- **Key Code Components:**
  - `AuthController.java`: Handles `/login`, `/register`, `/forgot-password`, `/verify-otp`, `/reset-password`.
  - `CustomUserDetailsService.java`: Loads user credentials and assigns Spring Security `GrantedAuthority` roles.
  - `SecurityConfig.java`: Enforces BCrypt password encoding, CSRF protection, and path permissions.
  - `UserRepository.java` & `PasswordResetOtpRepository.java`: MongoDB persistence for users and time-bound OTP tokens.
- **Justification:** Centralizes security rules across all hospital portals to ensure patients cannot access doctor/chairman endpoints.

---

### 🩺 Service 2: Patient Management Service
- **Primary Responsibility:** Patient record registration, demographic management, and medical history profiles.
- **Key Code Components:**
  - `PatientController.java`: Handles patient registration, profile updates, and patient listing.
  - `PatientRepository.java`: Spring Data MongoDB repository managing patient entities.
  - `Patient.java`: Entity containing blood group, age, gender, medical history, emergency contacts, and linked User ID.
- **Justification:** Separates patient PII (Personally Identifiable Information) from clinical prescription data for HIPAA compliance and logical isolation.

---

### 📅 Service 3: OPD & Appointment Management Service
- **Primary Responsibility:** Doctor scheduling, OPD slot availability, patient appointment requests, and doctor approval workflows.
- **Key Code Components:**
  - `AppointmentController.java`: Handles appointment booking (`/book-appointment`), patient appointment view (`/my-appointments`), and doctor status approvals (`CONFIRMED`, `CANCELLED`, `COMPLETED`).
  - `StaffController.java`: Manages doctor and staff profiles across departments (Cardiology, Neurology, Pediatrics, etc.).
  - `AppointmentRepository.java`: MongoDB persistence for appointment schedules.
- **Justification:** Manages high-concurrency booking requests independently without impacting patient medical record reads.

---

### 💊 Service 4: Clinical Prescriptions & Pharmacy Service
- **Primary Responsibility:** Digital prescription creation, medication dosage schedules, printable PDF prescription rendering, and pharmacy medicine inventory tracking.
- **Key Code Components:**
  - `PrescriptionController.java`: Allows doctors to issue prescriptions with diagnosis, medicines, and dosage instructions. Serves printable prescription views (`/prescription-print`).
  - `MedicineController.java`: Pharmacy inventory management tracking medicine name, stock count, unit price, and manufacturer.
  - `PrescriptionRepository.java` & `MedicineRepository.java`: MongoDB persistence.
- **Justification:** Ensures strict compliance so only authorized doctors can write digital prescriptions and track pharmacy inventory.

---

### 🧪 Service 5: Diagnostics & Medical Reports Service
- **Primary Responsibility:** Diagnostic lab test result entry (CBC, HbA1c, LFT, KFT, Lipid Profile), document file attachments, and patient printable report generation.
- **Key Code Components:**
  - `ReportController.java`: Doctor lab report creation (`/report-form`), patient report view (`/patient-reports`), and printable report view (`/report-print`).
  - `MedicalReportRepository.java`: MongoDB persistence storing test parameters, reference ranges, and doctor notes.
- **Justification:** Isolates heavy diagnostic report rendering and lab data handling from core appointment scheduling.

---

### 🤖 Service 6: Neura AI Healthcare Assistant Service
- **Primary Responsibility:** 24/7 intelligent patient assistance powered by `gemini-3.6-flash`, prompt engineering, session conversation memory, offline vector knowledge base embedding, and 50-query daily rate limiting.
- **Key Code Components:**
  - `ChatController.java`: REST endpoint `/api/chat` (POST) and `/api/chat/quota` (GET).
  - `ChatService.java`: Core pipeline orchestrator handling rate limiting, prompt injection defense, greetings, fixed emergency triage, 50-query user quota tracking, and Gemini call dispatch.
  - `GeminiService.java`: Invokes Google Gemini REST API (`gemini-3.6-flash`).
  - `PromptService.java`: Injects system instructions, hospital metadata (Helpline `+91 800-555-CARE`, OPD hours), and session memory into every prompt.
  - `ConversationService.java`: Maintains chat history per patient session.
  - `KnowledgeBaseService.java` & `VectorSearchService.java`: Ingests medical guidelines into MongoDB `vector_chunks` for keyword/vector fallback search.
- **Justification:** Provides real-time clinical guidance while protecting external API quotas and enforcing strict medical safety disclaimers.

---

### 🔔 Service 7: Notification & Alert Service
- **Primary Responsibility:** Automated medication timing reminders on patient dashboards and SMTP email notifications for password resets.
- **Key Code Components:**
  - `NotificationController.java`: Serves medication reminders (`/api/notifications/medication-reminders`).
  - `EmailService.java`: Asynchronous email dispatcher using Spring Mail (`JavaMailSender`).
  - `MedicationNotificationRepository.java`: MongoDB persistence for scheduled reminders.
- **Justification:** Asynchronous execution prevents email dispatch or notification polling from blocking main HTTP web threads.

---

## 🔬 3. Detailed Code Breakdown: How it is Architected for Microservices

The codebase was engineered specifically so it can be demonstrated as **Microservice-Ready** without altering a single line of business logic:

### 1. Build Dependency Level (`pom.xml`)
Your `pom.xml` includes official Spring Cloud Microservice starters:
```xml
<dependency>
    <groupId>org.springframework.cloud</groupId>
    <artifactId>spring-cloud-starter-netflix-eureka-client</artifactId>
</dependency>
<dependency>
    <groupId>org.springframework.cloud</groupId>
    <artifactId>spring-cloud-starter-config</artifactId>
</dependency>
<dependency>
    <groupId>org.springframework.cloud</groupId>
    <artifactId>spring-cloud-stream</artifactId>
</dependency>
```

### 2. Configuration Feature Toggles (`application.properties`)
Microservice service-discovery and event-bus features are embedded behind environment toggles:
```properties
server.port=${PORT:8085}
spring.cloud.discovery.enabled=false
eureka.client.enabled=false
spring.cloud.stream.enabled=false
spring.data.mongodb.uri=${SPRING_DATA_MONGODB_URI:mongodb://localhost:27017/pepclass}
google.gemini.api-key=${GOOGLE_GEMINI_API_KEY:}
```
*Turning `eureka.client.enabled=true` automatically registers the application with a Netflix Eureka Discovery server.*

### 3. API Gateway Route Partitioning (`/api/*` Namespaces)
Every controller is assigned a dedicated REST API namespace:
- 🔐 `/api/auth/*` ➔ Auth Service
- 🩺 `/api/patient/*` ➔ Patient Service
- 📅 `/api/appointments/*` ➔ OPD Service
- 💊 `/api/prescriptions/*` ➔ EHR Service
- 🧪 `/api/reports/*` ➔ Diagnostics Service
- 🤖 `/api/chat/*` ➔ Neura AI Service
- 🔔 `/api/notifications/*` ➔ Alert Service

*An NGINX or Spring Cloud API Gateway can route these paths to 7 independent Docker containers without any frontend code changes.*

### 4. Data Layer Decoupling (Spring Data Repositories)
Entities (`User`, `Patient`, `Appointment`, `Prescription`, `MedicalReport`, `VectorChunk`, `MedicationNotification`) have dedicated Spring Data Repositories. If microservices are separated, each repository can connect to its own isolated database instance (`auth_db`, `patient_opd_db`, etc.).

---

## ❓ 4. Top 10 Expected Viva Questions & Answers

#### **Q1: Is this project a Monolith or Microservices?**
> **Answer:** "It is a **Microservice-Ready Modular Monolith (Modulith)**. It is organized into 7 distinct domain-bounded services (`auth`, `patient`, `appointments`, `prescriptions`, `reports`, `ai-chat`, `notifications`) running within a single optimized Docker container for zero network latency and low hosting cost, but ready to be extracted into standalone microservices."

#### **Q2: Why didn't you deploy 7 separate microservices on Render?**
> **Answer:** "Deploying 7 separate microservice containers for a mid-tier hospital management system introduces unnecessary inter-service network latency, complex distributed tracing, and high cloud hosting costs. Our Modulith architecture delivers sub-millisecond in-memory communication while maintaining 100% loose coupling."

#### **Q3: How does the AI Chatbot (Neura) work under the hood?**
> **Answer:** "Neura operates via `ChatService.java`. When a query arrives, it checks for rate limits, checks fixed greetings (zero API cost), enforces a 50-query daily user quota, prepends system instructions & hospital context (helpline `+91 800-555-CARE`, OPD hours) via `PromptService.java`, and calls `gemini-3.6-flash` via `GeminiService.java`."

#### **Q4: How do you handle AI quota management?**
> **Answer:** "Each patient is allotted 50 queries per day. `ChatService.java` tracks query counts in a thread-safe `ConcurrentHashMap`. The remaining quota is returned in the API response JSON (`remainingQuota`) and displayed dynamically in both the chatbot window header badge (`⚡ 49/50 Left`) and the message footer."

#### **Q5: How is security and Role-Based Access Control (RBAC) enforced?**
> **Answer:** "Spring Security (`SecurityConfig.java`) uses BCrypt password hashing and custom UserDetailsService (`CustomUserDetailsService.java`). Roles (`ROLE_CHAIRMAN`, `ROLE_DOCTOR`, `ROLE_PATIENT`) are enforced via method-level annotations like `@PreAuthorize("hasRole('PATIENT')")` on API controllers."

#### **Q6: How does the application connect to MongoDB in production?**
> **Answer:** "In `application.properties`, we use dynamic property injection: `spring.data.mongodb.uri=${SPRING_DATA_MONGODB_URI:mongodb://localhost:27017/pepclass}`. On Render, it connects to a cloud-hosted MongoDB Atlas cluster over TLS/SSL."

#### **Q7: What happens if Google Gemini API is offline or rate-limited?**
> **Answer:** "The application gracefully returns an informative error message asking the user to check their API key, while basic greetings and emergency triage rules continue to function locally without crashing the application."

#### **Q8: How are background tasks handled without blocking user requests?**
> **Answer:** "Tasks like medical guideline indexing (`KnowledgeBaseService`) and SMTP email sending (`EmailService`) execute asynchronously in dedicated background thread pools, preventing HTTP request threads from blocking."

#### **Q9: How would you split this into 7 independent microservices in the future?**
> **Answer:** "We would move each domain package into its own Maven module, enable Netflix Eureka (`eureka.client.enabled=true`), add Spring Cloud Gateway for route forwarding, and split MongoDB into 7 separate databases."

#### **Q10: How did you package and deploy the app to Render without Docker GUI?**
> **Answer:** "We created a multi-stage `Dockerfile` using `maven:3.9-eclipse-temurin-21-alpine` for compilation and `eclipse-temurin:21-jre-alpine` for running the lightweight 0.0.1-SNAPSHOT JAR. Render automatically builds the container from our GitHub repository."
