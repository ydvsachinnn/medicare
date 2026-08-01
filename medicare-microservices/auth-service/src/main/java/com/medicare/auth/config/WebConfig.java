package com.medicare.auth.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.format.FormatterRegistry;
import org.springframework.lang.NonNull;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import com.medicare.auth.model.Patient;
import com.medicare.auth.repository.PatientRepository;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    private final PatientRepository patientRepository;

    public WebConfig(PatientRepository patientRepository) {
        this.patientRepository = patientRepository;
    }

    @Override
    public void addFormatters(FormatterRegistry registry) {
        registry.addConverter(new Converter<String, Patient>() {
            @Override
            public Patient convert(@NonNull String source) {
                if (source.trim().isEmpty()) {
                    return null;
                }
                return patientRepository.findById(source).orElse(null);
            }
        });
    }
}
