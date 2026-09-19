package com.vanapp.controller;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseToken;
import com.vanapp.model.Usuario;
import com.vanapp.repository.UsuarioRepository;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "4. Autenticação", description = "Segurança e acesso ao sistema")
@RestController
@RequestMapping("/auth")
@CrossOrigin(origins = "*")
public class AuthController {

    private final UsuarioRepository usuarioRepository;

    public AuthController(UsuarioRepository usuarioRepository) {
        this.usuarioRepository = usuarioRepository;
    }

    @Operation(summary = "Login/Sincronização com Firebase", description = "Valida o token JWT do Firebase, identifica o utilizador pelo CPF e retorna os dados de perfil.")
    @PostMapping("/login")
    public ResponseEntity<Object> login(
            @RequestHeader("Authorization") String authHeader,
            @RequestBody Map<String, String> payload) {
        
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return ResponseEntity.status(401).body("Token de autenticação ausente ou mal formatado.");
        }

        String cpf = payload.get("cpf");
        if (cpf == null || cpf.isBlank()) {
            return ResponseEntity.badRequest().body("O CPF é obrigatório para vincular o utilizador.");
        }

        try {
            String idToken = authHeader.substring(7);
            FirebaseToken decodedToken = FirebaseAuth.getInstance().verifyIdToken(idToken);
            String uid = decodedToken.getUid();

            Optional<Usuario> usuarioOpt = usuarioRepository.findByCpf(cpf);

            if (usuarioOpt.isEmpty()) {
                return ResponseEntity.status(404).body("Utilizador não encontrado com este CPF.");
            }

            Usuario u = usuarioOpt.get();
            
            if (u.getFirebaseUid() == null || !u.getFirebaseUid().equals(uid)) {
                u.setFirebaseUid(uid);
                usuarioRepository.save(u);
            }

            Map<String, String> resposta = new HashMap<>();
            resposta.put("id", u.getId().toString());
            resposta.put("tipo", u.getTipo());
            resposta.put("nome", u.getNome() != null ? u.getNome() : "");
            resposta.put("endereco", u.getEnderecoCompleto() != null ? u.getEnderecoCompleto() : "");
            
            return ResponseEntity.ok(resposta);

        } catch (Exception e) {
            return ResponseEntity.status(401).body("Falha na autenticação com o Firebase: " + e.getMessage());
        }
    }
}