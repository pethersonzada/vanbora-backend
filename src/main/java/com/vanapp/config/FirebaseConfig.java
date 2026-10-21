package com.vanapp.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import jakarta.annotation.PostConstruct;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
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

            if (!FirebaseApp.getApps().isEmpty()) {
                return;
            }

            InputStream serviceAccount = resolveInputStream(firebaseConfigJson.trim());

            FirebaseOptions options = FirebaseOptions.builder()
                    .setCredentials(GoogleCredentials.fromStream(serviceAccount))
                    .build();

            FirebaseApp.initializeApp(options);

        } catch (Exception e) {
            throw new RuntimeException("Erro ao inicializar o Firebase: " + e.getMessage(), e);
        }
    }

    private InputStream resolveInputStream(String input) throws Exception {
        // 1. Se for caminho de um arquivo existente no sistema
        File file = new File(input);
        if (file.exists() && file.isFile()) {
            return new FileInputStream(file);
        }

        // 2. Se for JSON puro em texto
        if (input.startsWith("{")) {
            return new ByteArrayInputStream(input.getBytes(StandardCharsets.UTF_8));
        }

        // 3. Tenta decodificar como Base64 se não for arquivo nem JSON puro
        try {
            byte[] decodedBytes = Base64.getDecoder().decode(input);
            String jsonFromBase64 = new String(decodedBytes, StandardCharsets.UTF_8);
            if (jsonFromBase64.trim().startsWith("{")) {
                return new ByteArrayInputStream(decodedBytes);
            }
        } catch (IllegalArgumentException ignored) {
        }

        throw new IllegalArgumentException("O valor de FIREBASE_CONFIG_JSON não é um caminho válido de arquivo, JSON ou Base64 válido.");
    }
}