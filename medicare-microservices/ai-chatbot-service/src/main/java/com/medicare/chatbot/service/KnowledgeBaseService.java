package com.medicare.chatbot.service;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import com.medicare.chatbot.model.VectorChunk;
import com.medicare.chatbot.repository.VectorChunkRepository;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Stream;

@Service
public class KnowledgeBaseService {

    private static final Logger log = LoggerFactory.getLogger(KnowledgeBaseService.class);

    private final VectorChunkRepository chunkRepository;
    private final EmbeddingService embeddingService;
    private final TextSplitter textSplitter;
    private final String knowledgeFolder;

    public KnowledgeBaseService(
            VectorChunkRepository chunkRepository,
            EmbeddingService embeddingService,
            TextSplitter textSplitter,
            @Value("${hospital.knowledge.path:src/main/resources/knowledge}") String knowledgeFolder) {
        this.chunkRepository = chunkRepository;
        this.embeddingService = embeddingService;
        this.textSplitter = textSplitter;
        this.knowledgeFolder = knowledgeFolder;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void bootstrapKnowledgeBase() {
        // Run indexing in a background thread so the application starts immediately
        Thread indexThread = new Thread(() -> {
            log.info("[KnowledgeBase] Starting background document embedding indexing...");
            try {
                Path folderPath = Paths.get(knowledgeFolder).toAbsolutePath().normalize();
                if (!Files.exists(folderPath)) {
                    Files.createDirectories(folderPath);
                    log.info("[KnowledgeBase] Created knowledge base directory: {}", folderPath);
                }

                // Write default clinic guideline files if the directory is empty
                if (isDirEmpty(folderPath)) {
                    writeDefaultKnowledgeFiles(folderPath);
                }

                // Collect file list first, then process sequentially
                List<Path> filesToProcess;
                try (Stream<Path> paths = Files.list(folderPath)) {
                    filesToProcess = paths.filter(Files::isRegularFile)
                            .filter(p -> p.toString().endsWith(".txt") || p.toString().endsWith(".md") || p.toString().endsWith(".pdf"))
                            .toList();
                }

                for (Path file : filesToProcess) {
                    processFile(file);
                }

                log.info("[KnowledgeBase] Background indexing completed. Active chunk registry size: {}", chunkRepository.count());

            } catch (Exception e) {
                log.error("[KnowledgeBase] Error initializing document registry: {}", e.getMessage(), e);
            }
        }, "knowledge-base-indexer");
        indexThread.setDaemon(true);
        indexThread.start();
    }

    private boolean isDirEmpty(Path path) throws IOException {
        try (Stream<Path> entries = Files.list(path)) {
            return !entries.findFirst().isPresent();
        }
    }

    private void processFile(Path filePath) {
        String filename = filePath.getFileName().toString();
        
        // Cache Check: Skip processing if this file's vectors are already registered
        if (!chunkRepository.findBySourceFile(filename).isEmpty()) {
            log.info("[KnowledgeBase] File '{}' is already indexed in database. Skipping.", filename);
            return;
        }

        log.info("[KnowledgeBase] Parsing and indexing new file: {}", filename);
        try {
            String text;
            if (filename.endsWith(".pdf")) {
                text = parsePdf(filePath.toFile());
            } else {
                text = Files.readString(filePath, StandardCharsets.UTF_8);
            }

            if (text == null || text.trim().isEmpty()) {
                log.warn("[KnowledgeBase] File '{}' is empty. Skipping.", filename);
                return;
            }

            // Split into overlapping chunks
            List<String> textChunks = textSplitter.split(text, 800, 150);
            text = null; // Release original text to free memory
            log.info("[KnowledgeBase] Segmented '{}' into {} chunks.", filename, textChunks.size());

            int indexedCount = 0;
            for (int i = 0; i < textChunks.size(); i++) {
                String chunkText = textChunks.get(i);
                
                List<Double> vector = embeddingService.getEmbedding(chunkText);
                List<Double> vectorList = (vector != null) ? vector : List.of();
                VectorChunk vectorChunk = new VectorChunk(chunkText, filename, i, vectorList);
                chunkRepository.save(vectorChunk);
                indexedCount++;

                // Small delay between API calls to avoid rate-limiting and reduce memory pressure
                try { Thread.sleep(200); } catch (InterruptedException ignored) { Thread.currentThread().interrupt(); }
            }
            log.info("[KnowledgeBase] Successfully indexed {} of {} chunks from file '{}'.", indexedCount, textChunks.size(), filename);

        } catch (Exception e) {
            log.error("[KnowledgeBase] Failed to index file '{}': {}", filename, e.getMessage(), e);
        }
    }

    private String parsePdf(File file) throws IOException {
        try (PDDocument doc = Loader.loadPDF(file)) {
            PDFTextStripper stripper = new PDFTextStripper();
            return stripper.getText(doc);
        }
    }

    private void writeDefaultKnowledgeFiles(Path folderPath) {
        log.info("[KnowledgeBase] Ingesting default clinical guidelines into knowledge folder...");
        
        // 1. Cardiovascular Emergency Guideline
        String heartEmergency = """
            # Cardiovascular Emergency and Stroke Guidelines (WHO/CDC)
            
            ## Heart Attack (Myocardial Infarction)
            - Symptoms: Chest pain or pressure (squeezing feeling in center of chest), radiation of pain to neck, jaw, left arm, or back, shortness of breath, cold sweat, dizziness, and nausea.
            - Immediate Action: Call emergency hotline (+91 800-555-CARE) immediately. Seat the patient in a resting position. Give 325mg chewable aspirin if not allergic. Do not leave the patient unattended.
            - Red Flags: Loss of consciousness, sudden collapse, absence of pulse (start CPR immediately if unconscious and not breathing).
            
            ## Stroke (Brain Attack) - FAST Protocol
            - Face Drooping: Ask the person to smile. Does one side of the face droop?
            - Arm Weakness: Ask the person to raise both arms. Does one arm drift downward?
            - Speech Difficulty: Ask the person to repeat a simple sentence. Is speech slurred or strange?
            - Time to Call: If any signs are present, call emergency services immediately. Time lost is brain lost.
            
            ## CPR (Cardiopulmonary Resuscitation) Step-by-Step
            1. Check Response: Shake shoulders and shout "Are you okay?".
            2. Call for Help: Dial emergency phone number (+91 800-555-CARE).
            3. Chest Compressions: Push hard and fast in center of chest (100-120 compressions per minute, 2 inches deep).
            4. Rescue Breaths: If trained, give 2 breaths after every 30 compressions. If untrained, perform hands-only CPR.
            """;

        // 2. Diabetes and Hypertension Diet & Disease Guideline
        String metabolicGuideline = """
            # Diabetes and Hypertension Prevention & Diet Guide (NIH/WHO)
            
            ## Diabetes Overview
            Diabetes mellitus is a chronic metabolic disease where the body cannot regulate glucose levels.
            - Symptoms: Excessive thirst (polydipsia), frequent urination (polyuria), unexplained weight loss, fatigue, blurry vision.
            - HbA1c Lab Test: Under 5.7% is normal; 5.7% to 6.4% is prediabetes; 6.5% or higher indicates diabetes.
            - Diet Advice: Low glycemic index carbs (oats, brown rice), high fiber vegetables, lean proteins. Avoid refined sugars, sodas, and white bread.
            
            ## Hypertension (High Blood Pressure) Overview
            Hypertension is defined as blood pressure consistently above 130/80 mmHg.
            - Symptoms: Often silent. Severe cases show morning headache, dizziness, nosebleeds, and fatigue.
            - Complications: Heart attack, stroke, kidney damage.
            - DASH Diet (Dietary Approaches to Stop Hypertension): Low sodium (under 2,300mg/day), rich in potassium, calcium, and magnesium. Focus on whole grains, poultry, fish, nuts, and green vegetables. Limit red meats, sweets, and saturated fats.
            
            ## Medical Disclaimer
            This information is educational. Patient diagnosis and drug dosages (e.g. insulin, metformin, amlodipine) must be verified by a medical doctor.
            """;

        // 3. General Emergency First Aid Guideline
        String firstAidGuideline = """
            # Emergency First Aid Guidelines (CDC/NIH)
            
            ## Snake Bites
            - Action: Keep the patient calm and restricted in movement. Immobilize the bitten limb below heart level. Remove rings or tight clothing. Clean bite with soap/water.
            - Warnings: DO NOT cut the wound, DO NOT attempt to suck venom, DO NOT apply ice or tight tourniquets. Note snake features for antivenom. Seek emergency room antivenom immediately.
            
            ## Dog Bites & Rabies Prevention
            - Action: Wash the bite wound immediately with soap and running water for at least 15 minutes to reduce rabies viral load. Apply sterile bandage.
            - Medical Care: Consult a doctor within 24 hours. The patient may require a tetanus booster and Post-Exposure Prophylaxis (PEP) Rabies Vaccine.
            
            ## Thermal Burns
            - First-Degree & Second-Degree (Minor): Cool the burn under running cool water (not cold/ice) for 10-15 minutes. Cover with sterile non-stick bandage. Do not pop blisters.
            - Third-Degree (Severe): Do not remove burned clothing. Cover with cool moist sterile sheet. Call emergency room immediately.
            
            ## Bleeding Control
            - Action: Apply direct pressure to the wound with a clean sterile dressing. Elevate the injured area if possible. Apply pressure bandage.
            
            ## Choking (Heimlich Maneuver)
            - Action: Stand behind the person, wrap arms around waist. Make a fist, place it above navel, grasp fist, and perform quick, upward abdominal thrusts.
            """;

        // 4. Insomnia and Sleep Disorder Guideline
        String insomniaGuideline = """
            # Insomnia and Sleep Hygiene Clinical Guidelines (NIH/WHO)
            
            ## Insomnia Overview
            Insomnia is a sleep disorder characterized by persistent difficulty falling asleep, staying asleep, or waking up too early and being unable to get back to sleep.
            - Symptoms: Daytime fatigue, irritability, difficulty concentrating, anxiety about sleep, low energy.
            - Causes: High stress, poor sleep hygiene, blue light exposure before bed, caffeine/alcohol consumption, shift work, depression, or underlying medical conditions.
            
            ## Sleep Hygiene & Non-Pharmacological Management
            1. Sleep Schedule: Maintain a consistent bed and wake time 7 days a week.
            2. Sleep Environment: Bedroom should be dark, quiet, and cool (65-68°F / 18-20°C). Use blackout curtains or earplugs.
            3. Stimulant Control: Avoid caffeine, tea, and heavy meals within 6 hours of bedtime. Avoid alcohol before sleep.
            4. Digital Detox: Turn off smartphones, tablets, and laptops 1 hour prior to sleep.
            5. Stimulus Control Therapy: Use bed only for sleep. If unable to fall asleep after 20 minutes, get out of bed, go to another quiet room, read a book in low light, and return only when sleepy.
            6. Relaxation Techniques: Deep abdominal breathing (4-7-8 method), progressive muscle relaxation, mindfulness meditation.
            
            ## Clinical Evaluation & Warnings
            Chronic insomnia lasting over 3 weeks requires medical evaluation by a physician or sleep specialist to rule out sleep apnea, restless legs syndrome (RLS), or thyroid disorders.
            """;

        try {
            Files.writeString(folderPath.resolve("cardiology_heart_attack.txt"), heartEmergency, StandardCharsets.UTF_8);
            Files.writeString(folderPath.resolve("diabetes_hypertension.txt"), metabolicGuideline, StandardCharsets.UTF_8);
            Files.writeString(folderPath.resolve("emergency_first_aid.txt"), firstAidGuideline, StandardCharsets.UTF_8);
            Files.writeString(folderPath.resolve("mental_health_insomnia_sleep.txt"), insomniaGuideline, StandardCharsets.UTF_8);
            log.info("[KnowledgeBase] Default knowledge base files written successfully.");
        } catch (IOException e) {
            log.error("[KnowledgeBase] Failed to write default knowledge files: {}", e.getMessage(), e);
        }
    }
}
