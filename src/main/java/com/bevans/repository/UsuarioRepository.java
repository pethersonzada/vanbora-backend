package com.bevans.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import com.bevans.model.Usuario;

public interface UsuarioRepository extends JpaRepository<Usuario, Long> {

    Optional<Usuario> findByEmail(String email);

    Optional<Usuario> findByTelefone(String telefone);
    
    Optional<Usuario> findByFirebaseUid(String firebaseUid);

    List<Usuario> findByTipo(String tipo);
}