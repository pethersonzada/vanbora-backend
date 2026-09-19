package com.vanapp.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import jakarta.annotation.PostConstruct;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

@Configuration
public class FirebaseConfig {

    @Value("${FIREBASE_CONFIG_JSON}")
    private String firebaseConfigJson;

    @PostConstruct
    public void initialize() {
        try {
            if (firebaseConfigJson == null || firebaseConfigJson.isBlank()) {
                throw new IllegalStateException("A propriedade FIREBASE_CONFIG_JSON não está definida.");
            }

            byte[] decodedBytes = Base64.getDecoder().decode(firebaseConfigJson.trim());
            String jsonString = new String(decodedBytes, StandardCharsets.UTF_8);

            InputStream serviceAccount = new ByteArrayInputStream(jsonString.getBytes(StandardCharsets.UTF_8));

            FirebaseOptions options = FirebaseOptions.builder()
                    .setCredentials(GoogleCredentials.fromStream(serviceAccount))
                    .build();

            if (FirebaseApp.getApps().isEmpty()) {
                FirebaseApp.initializeApp(options);
            }
        } catch (Exception e) {
            throw new RuntimeException("Erro ao inicializar o Firebase: " + e.getMessage(), e);
        }
    }
}