package com.bevans;

import io.github.cdimascio.dotenv.Dotenv;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class VanappBackendApplication {
    public static void main(String[] args) {
        try {
            Dotenv dotenv = Dotenv.configure().ignoreIfMissing().load();
            if (dotenv.get("DATABASE_URL") != null) {
                System.setProperty("DATABASE_URL", dotenv.get("DATABASE_URL"));
                System.setProperty("DATABASE_USERNAME", dotenv.get("DATABASE_USERNAME"));
                System.setProperty("DATABASE_PASSWORD", dotenv.get("DATABASE_PASSWORD"));
            }
        } catch (Exception e) {
        }
        
        SpringApplication.run(VanappBackendApplication.class, args);
    }
}