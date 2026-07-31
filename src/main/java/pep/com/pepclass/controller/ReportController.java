package pep.com.pepclass.controller;

import jakarta.validation.Valid;
import java.nio.charset.StandardCharsets;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import pep.com.pepclass.model.MedicalReport;
import pep.com.pepclass.model.User;
import pep.com.pepclass.repository.MedicalReportRepository;
import pep.com.pepclass.repository.PatientRepository;
import pep.com.pepclass.repository.UserRepository;

@Controller
public class ReportController {

    private final MedicalReportRepository medicalReportRepository;
    private final PatientRepository patientRepository;
    private final UserRepository userRepository;

    public ReportController(
            MedicalReportRepository medicalReportRepository,
            PatientRepository patientRepository,
            UserRepository userRepository) {
        this.medicalReportRepository = medicalReportRepository;
        this.patientRepository = patientRepository;
        this.userRepository = userRepository;
    }

    @GetMapping("/reports")
    @PreAuthorize("hasAnyRole('CHAIRMAN', 'DOCTOR')")
    public String reports(Model model) {
        model.addAttribute("reports", medicalReportRepository.findAll());
        return "reports";
    }

    @GetMapping("/reports/new")
    @PreAuthorize("hasAnyRole('CHAIRMAN', 'DOCTOR')")
    public String newReport(Model model) {
        model.addAttribute("report", new MedicalReport());
        model.addAttribute("patients", patientRepository.findAll());
        return "report-form";
    }

    @GetMapping("/reports/{id}/edit")
    @PreAuthorize("hasAnyRole('CHAIRMAN', 'DOCTOR')")
    public String editReport(@PathVariable("id") String id, Model model) {
        model.addAttribute("report", medicalReportRepository.findById(id).orElseThrow());
        model.addAttribute("patients", patientRepository.findAll());
        return "report-form";
    }

    @PostMapping("/reports")
    @PreAuthorize("hasAnyRole('CHAIRMAN', 'DOCTOR')")
    public String saveReport(
            @Valid @ModelAttribute("report") MedicalReport report,
            BindingResult result,
            Model model,
            Authentication authentication) {
        if (result.hasErrors()) {
            model.addAttribute("patients", patientRepository.findAll());
            return "report-form";
        }
        if (report.getId() != null && report.getId().isBlank()) {
            report.setId(null);
        }
        if (report.getPatient() != null && report.getPatient().getId() != null) {
            patientRepository.findById(report.getPatient().getId()).ifPresent(report::setPatient);
        }
        if (report.getUploadedBy() == null || report.getUploadedBy().isBlank()) {
            report.setUploadedBy(authentication.getName());
        }
        if (report.getFileName() == null || report.getFileName().isBlank()) {
            report.setFileName(report.getTitle().replaceAll("[^A-Za-z0-9_-]+", "_") + ".txt");
        }
        medicalReportRepository.save(report);
        return "redirect:/reports";
    }

    @GetMapping("/reports/{id}/delete")
    @PreAuthorize("hasAnyRole('CHAIRMAN', 'DOCTOR')")
    public String deleteReport(@PathVariable("id") String id) {
        medicalReportRepository.deleteById(id);
        return "redirect:/reports";
    }

    @GetMapping("/reports/{id}/print")
    @PreAuthorize("hasAnyRole('CHAIRMAN', 'DOCTOR', 'PATIENT')")
    public String printReport(
            @PathVariable("id") String id,
            @RequestParam(value = "download", required = false) String download,
            Model model,
            Authentication authentication) {

        if (id == null || id.isBlank()) {
            return "redirect:/reports";
        }

        MedicalReport report = medicalReportRepository.findById(id).orElse(null);
        if (report == null) {
            return "redirect:/reports";
        }

        String username = authentication.getName();
        User user = userRepository.findByUsername(username)
                .orElseGet(() -> userRepository.findByEmail(username).orElse(null));

        boolean patientOwnsReport = false;
        if (report.getPatient() != null && report.getPatient().getPatientUsername() != null) {
            String pUser = report.getPatient().getPatientUsername();
            if (pUser.equalsIgnoreCase(username) || (user != null && (pUser.equalsIgnoreCase(user.getUsername()) || pUser.equalsIgnoreCase(user.getEmail())))) {
                patientOwnsReport = true;
            }
        }

        boolean staff = authentication.getAuthorities().stream()
                .anyMatch(authority -> authority.getAuthority().equals("ROLE_CHAIRMAN") || authority.getAuthority().equals("ROLE_DOCTOR"));
        if (!staff && !patientOwnsReport) {
            return "redirect:/patient-reports";
        }

        model.addAttribute("report", report);
        return "report-print";
    }

    @GetMapping("/reports/{id}/download")
    @PreAuthorize("hasAnyRole('CHAIRMAN', 'DOCTOR', 'PATIENT')")
    public ResponseEntity<byte[]> downloadReport(
            @PathVariable("id") String id,
            Authentication authentication) {

        MedicalReport report = medicalReportRepository.findById(id).orElse(null);
        if (report == null) {
            return ResponseEntity.notFound().build();
        }

        String username = authentication.getName();
        User user = userRepository.findByUsername(username)
                .orElseGet(() -> userRepository.findByEmail(username).orElse(null));

        boolean patientOwnsReport = false;
        if (report.getPatient() != null && report.getPatient().getPatientUsername() != null) {
            String pUser = report.getPatient().getPatientUsername();
            if (pUser.equalsIgnoreCase(username) || (user != null && (pUser.equalsIgnoreCase(user.getUsername()) || pUser.equalsIgnoreCase(user.getEmail())))) {
                patientOwnsReport = true;
            }
        }

        boolean staff = authentication.getAuthorities().stream()
                .anyMatch(authority -> authority.getAuthority().equals("ROLE_CHAIRMAN") || authority.getAuthority().equals("ROLE_DOCTOR"));

        if (!staff && !patientOwnsReport) {
            return ResponseEntity.status(403).build();
        }

        StringBuilder sb = new StringBuilder();
        sb.append("====================================================\n");
        sb.append("         MEDICARE PLUS MEDICAL REPORT              \n");
        sb.append("====================================================\n\n");
        sb.append("Report Title  : ").append(report.getTitle() != null ? report.getTitle() : "Medical Report").append("\n");
        sb.append("Report ID     : ").append(report.getId()).append("\n");
        sb.append("Patient Name  : ").append(report.getPatient() != null ? report.getPatient().getFullName() : "N/A").append("\n");
        sb.append("Uploaded By   : ").append(report.getUploadedBy() != null ? report.getUploadedBy() : "Staff").append("\n");
        if (report.getDischargeDate() != null && !report.getDischargeDate().isBlank()) {
            sb.append("Discharge Date: ").append(report.getDischargeDate()).append("\n");
        }
        sb.append("\n----------------------------------------------------\n");
        sb.append("DIAGNOSTIC REPORT DETAILS:\n");
        sb.append("----------------------------------------------------\n");
        sb.append(report.getReportContent() != null ? report.getReportContent() : "No report content available.").append("\n\n");

        if (report.getDischargeSummary() != null && !report.getDischargeSummary().isBlank()) {
            sb.append("----------------------------------------------------\n");
            sb.append("DISCHARGE SUMMARY & ADVICE:\n");
            sb.append("----------------------------------------------------\n");
            sb.append(report.getDischargeSummary()).append("\n\n");
        }

        sb.append("====================================================\n");
        sb.append("MediCare Plus Digital Healthcare System\n");

        byte[] bytes = sb.toString().getBytes(StandardCharsets.UTF_8);
        String fileName = report.getFileName() != null && !report.getFileName().isBlank() 
                ? report.getFileName() 
                : (report.getTitle() != null ? report.getTitle().replaceAll("[^A-Za-z0-9_-]+", "_") + ".txt" : "Medical_Report.txt");

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment().filename(fileName).build().toString())
                .contentType(MediaType.TEXT_PLAIN)
                .body(bytes);
    }

    @GetMapping("/patient-reports")
    @PreAuthorize("hasRole('PATIENT')")
    public String patientReports(Model model, Authentication authentication) {
        String authName = authentication.getName();
        User user = userRepository.findByUsername(authName)
                .orElseGet(() -> userRepository.findByEmail(authName).orElse(null));

        java.util.Set<String> searchKeys = new java.util.HashSet<>();
        searchKeys.add(authName);
        if (user != null) {
            if (user.getUsername() != null) searchKeys.add(user.getUsername());
            if (user.getEmail() != null) searchKeys.add(user.getEmail());
            if (user.getFullName() != null) searchKeys.add(user.getFullName());
        }

        java.util.List<pep.com.pepclass.model.Patient> matchingPatients = new java.util.ArrayList<>();
        for (String key : searchKeys) {
            patientRepository.findByPatientUsername(key).ifPresent(matchingPatients::add);
            matchingPatients.addAll(patientRepository.findByEmailIgnoreCase(key));
            matchingPatients.addAll(patientRepository.findByFullNameIgnoreCase(key));
            matchingPatients.addAll(patientRepository.findByContact(key));
        }

        java.util.List<MedicalReport> userReports = new java.util.ArrayList<>();

        for (pep.com.pepclass.model.Patient p : matchingPatients) {
            if (p.getId() != null) {
                for (MedicalReport r : medicalReportRepository.findByPatientId(p.getId())) {
                    if (userReports.stream().noneMatch(x -> x.getId().equals(r.getId()))) {
                        userReports.add(r);
                    }
                }
            }
        }

        for (String key : searchKeys) {
            for (MedicalReport r : medicalReportRepository.findByPatientPatientUsername(key)) {
                if (userReports.stream().noneMatch(x -> x.getId().equals(r.getId()))) {
                    userReports.add(r);
                }
            }
            for (MedicalReport r : medicalReportRepository.findByPatientEmailIgnoreCase(key)) {
                if (userReports.stream().noneMatch(x -> x.getId().equals(r.getId()))) {
                    userReports.add(r);
                }
            }
            for (MedicalReport r : medicalReportRepository.findByPatientFullNameIgnoreCase(key)) {
                if (userReports.stream().noneMatch(x -> x.getId().equals(r.getId()))) {
                    userReports.add(r);
                }
            }
        }

        model.addAttribute("reports", userReports);

        MedicalReport dischargeReport = userReports.stream()
                .filter(r -> r.getDischargeDate() != null && !r.getDischargeDate().isBlank())
                .findFirst()
                .orElse(null);
        model.addAttribute("latestDischargeReport", dischargeReport);

        return "patient-reports";
    }
}
