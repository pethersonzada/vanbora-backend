package com.bevans.security;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import com.bevans.model.AlunoTurma;
import com.bevans.model.Endereco;
import com.bevans.model.Turma;
import com.bevans.model.Usuario;
import com.bevans.repository.AlunoTurmaRepository;
import com.bevans.repository.EnderecoRepository;
import com.bevans.repository.TurmaRepository;
import com.bevans.repository.UsuarioRepository;

@Component("access")
public class AccessPolicy {
    private final UsuarioRepository usuarios;
    private final TurmaRepository turmas;
    private final AlunoTurmaRepository vinculos;
    private final EnderecoRepository enderecos;

    public AccessPolicy(UsuarioRepository usuarios, TurmaRepository turmas,
                        AlunoTurmaRepository vinculos, EnderecoRepository enderecos) {
        this.usuarios = usuarios;
        this.turmas = turmas;
        this.vinculos = vinculos;
        this.enderecos = enderecos;
    }

    public String uid() {
        Authentication a = SecurityContextHolder.getContext().getAuthentication();
        if (a == null || !a.isAuthenticated() || "anonymousUser".equals(a.getPrincipal())) {
            return null;
        }
        return a.getName();
    }

    public Usuario usuario() {
        String uid = uid();
        return uid == null ? null : usuarios.findByFirebaseUid(uid).orElse(null);
    }

    public boolean own(Long id) {
        Usuario u = usuario();
        return u != null && Objects.equals(u.getId(), id);
    }

    public boolean ownsUid(String firebaseUid) {
        return uid() != null && Objects.equals(uid(), firebaseUid);
    }

    public boolean isMotorista() {
        Usuario u = usuario();
        return u != null && "MOTORISTA".equals(u.getTipo());
    }

    public boolean canCreateTurma(Turma turma) {
        return turma != null && turma.getMotorista() != null
                && isMotorista() && own(turma.getMotorista().getId());
    }

    public boolean driverOfTurma(Long turmaId) {
        if (!isMotorista() || turmaId == null) return false;
        return turmas.findById(turmaId).map(t -> t.getMotorista() != null
                && own(t.getMotorista().getId())).orElse(false);
    }

    public boolean driverOfVinculo(Long vinculoId) {
        if (vinculoId == null) return false;
        return vinculos.findById(vinculoId)
                .map(v -> v.getTurma() != null && driverOfTurma(v.getTurma().getId()))
                .orElse(false);
    }

    public boolean canReadTurma(Long turmaId) {
        if (driverOfTurma(turmaId)) return true;
        Usuario u = usuario();
        return u != null && turmaId != null
                && vinculos.existsByTurmaIdAndAlunoIdAndStatus(
                    turmaId, u.getId(), "APROVADO");
    }

    public boolean ownAddress(Long enderecoId) {
        Usuario u = usuario();
        if (u == null || enderecoId == null) return false;
        return enderecos.findById(enderecoId)
                .map(e -> e.getUsuario() != null &&
                        Objects.equals(e.getUsuario().getId(), u.getId()))
                .orElse(false);
    }

    @Transactional(readOnly = true)
    public List<Usuario> passageirosVisiveis() {
        Usuario u = usuario();
        if (u == null) return List.of();
        if (!"MOTORISTA".equals(u.getTipo())) {
            return "PASSAGEIRO".equals(u.getTipo()) ? List.of(u) : List.of();
        }
        Map<Long, Usuario> visiveis = new LinkedHashMap<>();
        for (Turma turma : turmas.findByMotoristaId(u.getId())) {
            for (AlunoTurma vinculo : vinculos.findByTurmaIdAndStatus(turma.getId(), "APROVADO")) {
                if (vinculo.getAluno() != null) {
                    visiveis.put(vinculo.getAluno().getId(), vinculo.getAluno());
                }
            }
        }
        return new ArrayList<>(visiveis.values());
    }

    @Transactional(readOnly = true)
    public Usuario motoristaVisivel() {
        Usuario u = usuario();
        if (u == null) return null;
        if ("MOTORISTA".equals(u.getTipo())) return u;
        for (AlunoTurma vinculo : vinculos.findByAlunoIdAndStatus(u.getId(), "APROVADO")) {
            if (vinculo.getTurma() != null && vinculo.getTurma().getMotorista() != null) {
                return vinculo.getTurma().getMotorista();
            }
        }
        return null;
    }
}
