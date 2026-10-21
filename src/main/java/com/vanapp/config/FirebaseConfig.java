package com.vanapp.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

@Configuration
public class FirebaseConfig {

    @Value("${FIREBASE_CONFIG_JSON:}")
    private String firebaseConfigJson;

    @PostConstruct
    public void initialize() {
        if (FirebaseApp.getApps().isEmpty()) {
            try {
                if (firebaseConfigJson == null || firebaseConfigJson.isBlank()) {
                    System.out.println("Firebase Config não informada. Pulando inicialização.");
                    return;
                }

                String rawJson = firebaseConfigJson.trim();
                byte[] jsonBytes;

                // Se começar com '{', é JSON cru. Se não, decodifica do Base64
                if (rawJson.startsWith("{")) {
                    jsonBytes = rawJson.getBytes(StandardCharsets.UTF_8);
                } else {
                    // Sanitiza a string removendo espaços e tratando caracteres de URL
                    String sanitized = rawJson.replaceAll("\\s+", "").replace('-', '+').replace('_', '/');
                    jsonBytes = Base64.getDecoder().decode(sanitized);
                }

                try (InputStream stream = new ByteArrayInputStream(jsonBytes)) {
                    FirebaseOptions options = FirebaseOptions.builder()
                            .setCredentials(GoogleCredentials.fromStream(stream))
                            .build();
                    FirebaseApp.initializeApp(options);
                    System.out.println("Firebase inicializado com sucesso!");
                }
            } catch (Exception e) {
                System.err.println("Erro ao inicializar Firebase: " + e.getMessage());
                // Tratado sem lançar RuntimeException para não crashar a API no boot
            }
        }
    }
}