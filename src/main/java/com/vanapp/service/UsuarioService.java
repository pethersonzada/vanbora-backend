package com.vanapp.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.google.firebase.auth.FirebaseAuth;
import com.vanapp.model.Usuario;
import com.vanapp.repository.PresencaRepository;
import com.vanapp.repository.UsuarioRepository;
import com.vanapp.repository.ViagemRepository;

@Service
public class UsuarioService {

    private final UsuarioRepository usuarioRepository;
    private final PresencaRepository presencaRepository;
    private final ViagemRepository viagemRepository;

    public UsuarioService(UsuarioRepository usuarioRepository, PresencaRepository presencaRepository, ViagemRepository viagemRepository) {
        this.usuarioRepository = usuarioRepository;
        this.presencaRepository = presencaRepository;
        this.viagemRepository = viagemRepository;
    }

    public Usuario cadastrarUsuario(Usuario usuario) {
        if (usuarioRepository.findByEmail(usuario.getEmail()).isPresent()) {
            throw new RuntimeException("E-mail já cadastrado");
        }
        if (usuarioRepository.findByTelefone(usuario.getTelefone()).isPresent()) {
            throw new RuntimeException("Telefone já cadastrado");
        }
        
        return usuarioRepository.save(usuario);
    }

    public Usuario buscarPorId(Long id) {
        if (id == null) throw new RuntimeException("id não pode ser nulo");
        return usuarioRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Usuário não encontrado"));
    }

    public List<Usuario> listarPassageiros() {
        return usuarioRepository.findAll()
                .stream()
                .filter(u -> "PASSAGEIRO".equals(u.getTipo()))
                .toList();
    }

    @Transactional
    public void excluirUsuario(Long id) {
        if (id == null) throw new RuntimeException("O ID fornecido para exclusão não pode ser nulo.");

        Usuario usuario = usuarioRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Usuário não encontrado no sistema."));
        
        if ("MOTORISTA".equals(usuario.getTipo())) {
            if (viagemRepository.existsByTurmaMotoristaIdAndStatus(id, "EM_ANDAMENTO")) {
                throw new RuntimeException("Não é possível excluir a conta com uma rota em andamento. Encerre a viagem primeiro.");
            }
            viagemRepository.deleteAllByTurmaMotoristaId(id);
        }

        try {
            if (usuario.getFirebaseUid() != null && !usuario.getFirebaseUid().isBlank()) {
                FirebaseAuth.getInstance().deleteUser(usuario.getFirebaseUid());
            }
        } catch (Exception e) {
            throw new RuntimeException("Erro ao excluir o utilizador no Firebase: " + e.getMessage());
        }

        presencaRepository.deleteAllByUsuarioId(id); 
        usuarioRepository.deleteById(id);
    }
}