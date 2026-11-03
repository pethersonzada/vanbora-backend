package com.vanapp.controller;

import java.util.HashMap;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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

    @Operation(summary = "Login/Sincronização com Firebase", description = "Valida o token JWT do Firebase, identifica o utilizador pelo E-mail e retorna os dados de perfil.")
    @PostMapping("/login")
    public ResponseEntity<Object> login(
            @RequestHeader("Authorization") String authHeader,
            @RequestBody Map<String, String> payload) throws Exception {
        
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            throw new IllegalArgumentException("Token de autenticação ausente ou mal formatado.");
        }

        String email = payload.get("email");
        if (email == null || email.isBlank()) {
            throw new IllegalArgumentException("O E-mail é obrigatório para vincular o utilizador.");
        }

        String idToken = authHeader.substring(7);
        FirebaseToken decodedToken = FirebaseAuth.getInstance().verifyIdToken(idToken);
        String uid = decodedToken.getUid();

        Usuario u = usuarioRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Utilizador não encontrado com este e-mail."));
        
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
    }
}