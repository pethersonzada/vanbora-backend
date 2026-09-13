package com.vanapp.service;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import com.vanapp.model.AlunoTurma;
import com.vanapp.model.Turma;
import com.vanapp.model.Usuario;
import com.vanapp.model.Viagem;
import com.vanapp.repository.AlunoTurmaRepository;
import com.vanapp.repository.TurmaRepository;
import com.vanapp.repository.UsuarioRepository;
import com.vanapp.repository.ViagemRepository;

@Service
public class TurmaService {

    private final TurmaRepository turmaRepository;
    private final UsuarioRepository usuarioRepository;
    private final AlunoTurmaRepository alunoTurmaRepository;
    private final ViagemRepository viagemRepository;

    public TurmaService(
            TurmaRepository turmaRepository, 
            UsuarioRepository usuarioRepository, 
            AlunoTurmaRepository alunoTurmaRepository,
            ViagemRepository viagemRepository
    ) {
        this.turmaRepository = turmaRepository;
        this.usuarioRepository = usuarioRepository;
        this.alunoTurmaRepository = alunoTurmaRepository;
        this.viagemRepository = viagemRepository;
    }

    public Turma criarTurma(Turma turma) {
        if (turma.getMotorista() == null || turma.getMotorista().getId() == null) {
            throw new RuntimeException("O ID do motorista é obrigatório para criar uma turma.");
        }
        
        Usuario motorista = usuarioRepository.findById(turma.getMotorista().getId())
                .orElseThrow(() -> new RuntimeException("Motorista não encontrado."));
        
        turma.setMotorista(motorista);
        
        if (turma.getCodigoConvite() == null || turma.getCodigoConvite().isEmpty()) {
            turma.setCodigoConvite(UUID.randomUUID().toString().substring(0, 6).toUpperCase());
        }
        
        return turmaRepository.save(turma);
    }

    public List<Turma> listarTurmasPorMotorista(Long motoristaId) {
        return turmaRepository.findByMotoristaId(motoristaId);
    }

    public List<Usuario> listarPassageirosPorTurma(Long turmaId) {
        List<AlunoTurma> vinculos = alunoTurmaRepository.findByTurmaIdAndStatus(turmaId, "APROVADO");
        return vinculos.stream().map(AlunoTurma::getAluno).collect(Collectors.toList());
    }

    public void adicionarAluno(Long turmaId, Long alunoId) {
        Turma turma = turmaRepository.findById(turmaId)
                .orElseThrow(() -> new RuntimeException("Turma não encontrada."));
        Usuario aluno = usuarioRepository.findById(alunoId)
                .orElseThrow(() -> new RuntimeException("Aluno não encontrado."));

        AlunoTurma vinculo = new AlunoTurma();
        vinculo.setTurma(turma);
        vinculo.setAluno(aluno);
        vinculo.setStatus("APROVADO");
        alunoTurmaRepository.save(vinculo);
    }

    public void removerAluno(Long turmaId, Long alunoId) {
        AlunoTurma vinculo = alunoTurmaRepository.findByTurmaIdAndAlunoId(turmaId, alunoId)
                .orElseThrow(() -> new RuntimeException("Vínculo não encontrado."));
        alunoTurmaRepository.delete(vinculo);
    }

    public Turma buscarTurmaPorAlunoId(Long alunoId) {
        List<AlunoTurma> vinculos = alunoTurmaRepository.findAll();
        for (AlunoTurma v : vinculos) {
            if (v.getAluno().getId().equals(alunoId) && "APROVADO".equals(v.getStatus())) {
                return v.getTurma();
            }
        }
        return null;
    }

    public Turma atualizarTurma(Long turmaId, Turma turmaAtualizada) {
        Turma turma = turmaRepository.findById(turmaId)
                .orElseThrow(() -> new RuntimeException("Turma não encontrada."));
        
        turma.setNome(turmaAtualizada.getNome());
        turma.setTurno(turmaAtualizada.getTurno());
        
        return turmaRepository.save(turma);
    }

    public void solicitarEntradaPorCodigo(Long alunoId, String codigo) {
        Turma turma = turmaRepository.findByCodigoConvite(codigo.toUpperCase())
                .orElseThrow(() -> new RuntimeException("Código de convite inválido."));
        
        Usuario aluno = usuarioRepository.findById(alunoId)
                .orElseThrow(() -> new RuntimeException("Aluno não encontrado."));

        AlunoTurma vinculo = new AlunoTurma();
        vinculo.setTurma(turma);
        vinculo.setAluno(aluno);
        vinculo.setStatus("PENDENTE");
        
        alunoTurmaRepository.save(vinculo);
    }

    public List<AlunoTurma> listarAlunosPorStatus(Long turmaId, String status) {
        return alunoTurmaRepository.findByTurmaIdAndStatus(turmaId, status);
    }

    public void analisarSolicitacao(Long vinculoId, String status) {
        AlunoTurma vinculo = alunoTurmaRepository.findById(vinculoId)
                .orElseThrow(() -> new RuntimeException("Solicitação não encontrada."));
        
        vinculo.setStatus(status);
        alunoTurmaRepository.save(vinculo);
    }

    public Turma regenerarCodigoConvite(Long turmaId) {
        Turma turma = turmaRepository.findById(turmaId)
                .orElseThrow(() -> new RuntimeException("Turma não encontrada."));
        
        turma.setCodigoConvite(UUID.randomUUID().toString().substring(0, 6).toUpperCase());
        return turmaRepository.save(turma);
    }

    public void deletarTurma(Long turmaId) {
        Turma turma = turmaRepository.findById(turmaId)
                .orElseThrow(() -> new RuntimeException("Turma não encontrada."));
        
        List<AlunoTurma> todosVinculos = alunoTurmaRepository.findAll().stream()
                .filter(v -> v.getTurma() != null && v.getTurma().getId().equals(turmaId))
                .toList();
        
        if (!todosVinculos.isEmpty()) {
            alunoTurmaRepository.deleteAll(todosVinculos);
        }

        List<Viagem> viagensDaTurma = viagemRepository.findAll().stream()
                .filter(v -> v.getTurma() != null && v.getTurma().getId().equals(turmaId))
                .toList();

        if (!viagensDaTurma.isEmpty()) {
            viagemRepository.deleteAll(viagensDaTurma);
        }

        turmaRepository.delete(turma);
    }
}