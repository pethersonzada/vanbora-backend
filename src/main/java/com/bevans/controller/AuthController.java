package com.bevans.controller;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.bevans.model.Usuario;
import com.bevans.repository.UsuarioRepository;
import com.bevans.security.AccessPolicy;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "4. Autenticação", description = "Segurança e acesso ao sistema")
@RestController
@RequestMapping("/auth")
public class AuthController {
    private final UsuarioRepository usuarios;
    private final AccessPolicy access;

    public AuthController(UsuarioRepository usuarios, AccessPolicy access) {
        this.usuarios = usuarios;
        this.access = access;
    }

    @Operation(summary = "Login vinculado à identidade Firebase verificada")
    @PostMapping("/login")
    public ResponseEntity<Object> login(@RequestBody Map<String, String> payload,
                                        HttpServletRequest request) {
        String uid = access.uid();
        String emailToken = (String) request.getAttribute("firebaseEmail");
        String emailInformado = payload == null ? null : payload.get("email");
        if (uid == null || emailToken == null || emailInformado == null
                || !emailToken.equalsIgnoreCase(emailInformado.trim())) {
            return ResponseEntity.status(403).body(Map.of("erro", "Identidade não corresponde ao token Firebase."));
        }

        Usuario usuario = usuarios.findByEmail(emailToken)
                .orElseThrow(() -> new IllegalArgumentException("Perfil não encontrado."));
        if (usuario.getFirebaseUid() != null
                && !Objects.equals(usuario.getFirebaseUid(), uid)) {
            return ResponseEntity.status(403).body(Map.of("erro", "Perfil pertence a outra identidade."));
        }
        if (usuario.getFirebaseUid() == null) {
            usuario.setFirebaseUid(uid);
            usuarios.save(usuario);
        }
        Map<String, String> resposta = new HashMap<>();
        resposta.put("id", usuario.getId().toString());
        resposta.put("tipo", usuario.getTipo());
        resposta.put("nome", usuario.getNome() == null ? "" : usuario.getNome());
        resposta.put("endereco", usuario.getEnderecoCompleto() == null
                ? "" : usuario.getEnderecoCompleto());
        return ResponseEntity.ok(resposta);
    }
}
