package com.bevans.service;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import com.bevans.model.AlunoTurma;
import com.bevans.model.Turma;
import com.bevans.model.Usuario;
import com.bevans.model.Viagem;
import com.bevans.repository.AlunoTurmaRepository;
import com.bevans.repository.TurmaRepository;
import com.bevans.repository.UsuarioRepository;
import com.bevans.repository.ViagemRepository;

@Service
public class TurmaService {

    private final TurmaRepository turmaRepository;
    private final UsuarioRepository usuarioRepository;
    private final AlunoTurmaRepository alunoTurmaRepository;
    private final ViagemRepository viagemRepository;
    private final GeocodingService geocodingService;

    public TurmaService(
            TurmaRepository turmaRepository, 
            UsuarioRepository usuarioRepository, 
            AlunoTurmaRepository alunoTurmaRepository,
            ViagemRepository viagemRepository,
            GeocodingService geocodingService
    ) {
        this.turmaRepository = turmaRepository;
        this.usuarioRepository = usuarioRepository;
        this.alunoTurmaRepository = alunoTurmaRepository;
        this.viagemRepository = viagemRepository;
        this.geocodingService = geocodingService;
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

        if (turma.getDestinoNome() != null && !turma.getDestinoNome().isEmpty()) {
            try {
                double[] coords = geocodingService.geocodificarEndereco(turma.getDestinoNome(), "Pernambuco");
                turma.setDestinoLatitude(coords[0]);
                turma.setDestinoLongitude(coords[1]);
            } catch (Exception e) {
                turma.setDestinoLatitude(-8.302755);
                turma.setDestinoLongitude(-35.991248);
            }
        }

        if (turma.getOrigemNome() != null && !turma.getOrigemNome().isEmpty()) {
            try {
                double[] coords = geocodingService.geocodificarEndereco(turma.getOrigemNome(), "Pernambuco");
                turma.setOrigemLatitude(coords[0]);
                turma.setOrigemLongitude(coords[1]);
            } catch (Exception e) {
                turma.setOrigemLatitude(motorista.getLatitude());
                turma.setOrigemLongitude(motorista.getLongitude());
            }
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
            .orElseThrow(() ->
                    new IllegalArgumentException(
                            "Turma não encontrada."
                    )
            );

    Usuario aluno = usuarioRepository.findById(alunoId)
            .orElseThrow(() ->
                    new IllegalArgumentException(
                            "Aluno não encontrado."
                    )
            );

    AlunoTurma vinculo = alunoTurmaRepository
            .findByTurmaIdAndAlunoId(
                    turmaId, alunoId
            )
            .orElse(null);

    if (vinculo == null) {
        vinculo = new AlunoTurma();
        vinculo.setTurma(turma);
        vinculo.setAluno(aluno);
    }

    vinculo.setStatus("APROVADO");

    alunoTurmaRepository.save(vinculo);
}


    public void removerAluno(Long turmaId, Long alunoId) {
        AlunoTurma vinculo = alunoTurmaRepository.findByTurmaIdAndAlunoId(turmaId, alunoId)
                .orElseThrow(() -> new RuntimeException("Vínculo não encontrado."));
        alunoTurmaRepository.delete(vinculo);
    }

    
public List<Turma> listarTurmasDoAluno(Long alunoId) {
    return alunoTurmaRepository
            .findByAlunoIdAndStatus(
                    alunoId, "APROVADO"
            )
            .stream()
            .map(AlunoTurma::getTurma)
            .filter(turma -> turma != null)
            .distinct()
            .toList();
}

// Compatibilidade com o endpoint antigo.
public Turma buscarTurmaPorAlunoId(Long alunoId) {
    return listarTurmasDoAluno(alunoId)
            .stream()
            .findFirst()
            .orElse(null);
}


     public Turma atualizarTurma(Long turmaId, Turma turmaAtualizada) {
        Turma turma = turmaRepository.findById(turmaId)
                .orElseThrow(() -> new RuntimeException("Turma não encontrada."));

        if (turmaAtualizada.getNome() != null && !turmaAtualizada.getNome().trim().isEmpty()) {
            turma.setNome(turmaAtualizada.getNome().trim());
        }
        if (turmaAtualizada.getTurno() != null && !turmaAtualizada.getTurno().trim().isEmpty()) {
            turma.setTurno(turmaAtualizada.getTurno().trim());
        }

        boolean destinoNomeMudou = turmaAtualizada.getDestinoNome() != null
                && !turmaAtualizada.getDestinoNome().equals(turma.getDestinoNome());

        boolean origemNomeMudou = turmaAtualizada.getOrigemNome() != null
                && !turmaAtualizada.getOrigemNome().equals(turma.getOrigemNome());

        // O mapa envia coordenadas precisas; elas têm prioridade sobre geocodificação.
        if (turmaAtualizada.getDestinoLatitude() != null
                || turmaAtualizada.getDestinoLongitude() != null) {
            validarCoordenadas(
                    turmaAtualizada.getDestinoLatitude(),
                    turmaAtualizada.getDestinoLongitude(),
                    "destino");
            turma.setDestinoLatitude(turmaAtualizada.getDestinoLatitude());
            turma.setDestinoLongitude(turmaAtualizada.getDestinoLongitude());
        } else if (destinoNomeMudou) {
            double[] coords = geocodingService.geocodificarEndereco(
                    turmaAtualizada.getDestinoNome(), "Pernambuco");
            validarCoordenadas(coords[0], coords[1], "destino");
            turma.setDestinoLatitude(coords[0]);
            turma.setDestinoLongitude(coords[1]);
        }

        if (turmaAtualizada.getDestinoNome() != null) {
            turma.setDestinoNome(turmaAtualizada.getDestinoNome());
        }

        if (turmaAtualizada.getOrigemLatitude() != null
                || turmaAtualizada.getOrigemLongitude() != null) {
            validarCoordenadas(
                    turmaAtualizada.getOrigemLatitude(),
                    turmaAtualizada.getOrigemLongitude(),
                    "origem");
            turma.setOrigemLatitude(turmaAtualizada.getOrigemLatitude());
            turma.setOrigemLongitude(turmaAtualizada.getOrigemLongitude());
        } else if (origemNomeMudou) {
            double[] coords = geocodingService.geocodificarEndereco(
                    turmaAtualizada.getOrigemNome(), "Pernambuco");
            validarCoordenadas(coords[0], coords[1], "origem");
            turma.setOrigemLatitude(coords[0]);
            turma.setOrigemLongitude(coords[1]);
        }

        if (turmaAtualizada.getOrigemNome() != null) {
            turma.setOrigemNome(turmaAtualizada.getOrigemNome());
        }

        return turmaRepository.save(turma);
    }

    private void validarCoordenadas(Double latitude, Double longitude, String campo) {
        if (latitude == null || longitude == null
                || !Double.isFinite(latitude) || !Double.isFinite(longitude)
                || latitude < -90 || latitude > 90
                || longitude < -180 || longitude > 180
                || (latitude == 0 && longitude == 0)) {
            throw new IllegalArgumentException(
                    "Coordenadas de " + campo + " inválidas ou incompletas.");
        }
    }

    
public void solicitarEntradaPorCodigo(
        Long alunoId,
        String codigo
) {
    if (codigo == null || codigo.isBlank()) {
        throw new IllegalArgumentException(
                "Informe o código de convite."
        );
    }

    Turma turma = turmaRepository
            .findByCodigoConvite(
                    codigo.trim().toUpperCase()
            )
            .orElseThrow(() ->
                    new IllegalArgumentException(
                            "Código de convite inválido."
                    )
            );

    Usuario aluno = usuarioRepository
            .findById(alunoId)
            .orElseThrow(() ->
                    new IllegalArgumentException(
                            "Aluno não encontrado."
                    )
            );

    AlunoTurma vinculo = alunoTurmaRepository
            .findByTurmaIdAndAlunoId(
                    turma.getId(),
                    alunoId
            )
            .orElse(null);

    if (vinculo != null) {
        if ("APROVADO".equals(vinculo.getStatus())) {
            throw new IllegalArgumentException(
                    "Você já participa desta turma."
            );
        }

        if ("PENDENTE".equals(vinculo.getStatus())) {
            throw new IllegalArgumentException(
                    "Sua solicitação já está pendente."
            );
        }
    } else {
        vinculo = new AlunoTurma();
        vinculo.setTurma(turma);
        vinculo.setAluno(aluno);
    }

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