package com.bevans.security;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import com.bevans.model.Endereco;
import com.bevans.model.Turma;
import com.bevans.model.Usuario;
import com.bevans.repository.AlunoTurmaRepository;
import com.bevans.repository.EnderecoRepository;
import com.bevans.repository.TurmaRepository;
import com.bevans.repository.UsuarioRepository;

@ExtendWith(MockitoExtension.class)
class AccessPolicyTest {
    @Mock UsuarioRepository usuarios;
    @Mock TurmaRepository turmas;
    @Mock AlunoTurmaRepository vinculos;
    @Mock EnderecoRepository enderecos;
    AccessPolicy access;
    Usuario motorista;
    Usuario outro;

    @BeforeEach
    void setUp() {
        access = new AccessPolicy(usuarios, turmas, vinculos, enderecos);
        motorista = new Usuario();
        motorista.setId(1L);
        motorista.setTipo("MOTORISTA");
        outro = new Usuario();
        outro.setId(2L);
        outro.setTipo("PASSAGEIRO");
        SecurityContextHolder.getContext().setAuthentication(
            new UsernamePasswordAuthenticationToken("uid-motorista", null, java.util.List.of()));
    }

    @AfterEach
    void clear() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void naoPermiteUsuarioDeOutroPerfil() {
        when(usuarios.findByFirebaseUid("uid-motorista")).thenReturn(Optional.of(motorista));
        assertTrue(access.own(1L));
        assertFalse(access.own(2L));
        assertFalse(access.ownsUid("outro-uid"));
    }

    @Test
    void restringeTurmaAoMotoristaDono() {
        when(usuarios.findByFirebaseUid("uid-motorista")).thenReturn(Optional.of(motorista));
        Turma turma = new Turma();
        turma.setMotorista(motorista);
        when(turmas.findById(7L)).thenReturn(Optional.of(turma));
        assertTrue(access.driverOfTurma(7L));
        turma.setMotorista(outro);
        assertFalse(access.driverOfTurma(7L));
    }

    @Test
    void enderecoDeOutroUsuarioEhNegado() {
        when(usuarios.findByFirebaseUid("uid-motorista")).thenReturn(Optional.of(motorista));
        Endereco endereco = new Endereco();
        endereco.setUsuario(outro);
        when(enderecos.findById(9L)).thenReturn(Optional.of(endereco));
        assertFalse(access.ownAddress(9L));
        endereco.setUsuario(motorista);
        assertTrue(access.ownAddress(9L));
    }

    @Test
    void semTokenNaoHaPermissao() {
        SecurityContextHolder.clearContext();
        assertFalse(access.own(1L));
        assertFalse(access.driverOfTurma(7L));
    }
}
