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

                // Se começar com '{', já é o JSON cru. Caso contrário, faz o decode Base64 de forma segura
                if (rawJson.startsWith("{")) {
                    jsonBytes = rawJson.getBytes(StandardCharsets.UTF_8);
                } else {
                    // Trata caracteres de Base64 URL safe e remove quebras de linha
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
                // Não lança RuntimeException para não derrubar a aplicação inteira no boot
            }
        }
    }
}