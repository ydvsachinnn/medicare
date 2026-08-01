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
