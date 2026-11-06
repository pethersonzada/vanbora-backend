
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

    private void validarCoordenadas(
            Double latitude,
            Double longitude,
            String campo
    ) {
        if (latitude == null
                || longitude == null
                || !Double.isFinite(latitude)
                || !Double.isFinite(longitude)
                || latitude < -90
                || latitude > 90
                || longitude < -180
                || longitude > 180
                || (latitude == 0.0 && longitude == 0.0)) {

            throw new IllegalArgumentException(
                    "Coordenadas de " + campo
                    + " inválidas ou incompletas."
            );
        }
    }

    private double[] resolverCoordenadas(
            Double latitude,
            Double longitude,
            String enderecoNome,
            String campo
    ) {
        // Coordenadas explícitas têm prioridade.
        // Um par incompleto é inválido.

        if (latitude != null || longitude != null) {
            validarCoordenadas(
                    latitude,
                    longitude,
                    campo
            );

            return new double[]{
                    latitude,
                    longitude
            };
        }

        // Sem coordenadas, o nome é obrigatório
        // para tentar a geocodificação.

        if (enderecoNome == null
                || enderecoNome.isBlank()) {

            throw new IllegalArgumentException(
                    "Informe o endereço de " + campo
                    + " ou selecione um ponto no mapa."
            );
        }

        try {
            double[] coords =
                    geocodingService.geocodificarEndereco(
                            enderecoNome.trim(),
                            "Pernambuco"
                    );

            if (coords == null || coords.length < 2) {
                throw new IllegalArgumentException(
                        "Geocodificação sem coordenadas."
                );
            }

            validarCoordenadas(
                    coords[0],
                    coords[1],
                    campo
            );

            return coords;

        } catch (Exception e) {
            throw new IllegalArgumentException(
                    "Não foi possível localizar o endereço "
                    + "de " + campo
                    + ". Selecione um ponto válido no mapa.",
                    e
            );
        }
    }

    public Turma criarTurma(Turma turma) {

        if (turma == null) {
            throw new IllegalArgumentException(
                    "Informe os dados da turma."
            );
        }

        if (turma.getMotorista() == null
                || turma.getMotorista().getId() == null) {

            throw new IllegalArgumentException(
                    "O ID do motorista é obrigatório "
                    + "para criar uma turma."
            );
        }

        Usuario motorista = usuarioRepository
                .findById(turma.getMotorista().getId())
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Motorista não encontrado."
                        )
                );

        if (!"MOTORISTA".equalsIgnoreCase(
                motorista.getTipo())) {
            throw new IllegalArgumentException(
                    "O usuário informado não é motorista."
            );
        }

        turma.setMotorista(motorista);

        if (turma.getNome() == null
                || turma.getNome().isBlank()) {

            throw new IllegalArgumentException(
                    "Informe o nome da turma."
            );
        }

        turma.setNome(turma.getNome().trim());

        if (turma.getCodigoConvite() == null
                || turma.getCodigoConvite().isBlank()) {

            turma.setCodigoConvite(
                    UUID.randomUUID()
                            .toString()
                            .substring(0, 6)
                            .toUpperCase()
            );
        }

        // Validar destino antes de persistir.

        double[] destino = resolverCoordenadas(
                turma.getDestinoLatitude(),
                turma.getDestinoLongitude(),
                turma.getDestinoNome(),
                "destino"
        );

        // Validar origem antes de persistir.

        double[] origem = resolverCoordenadas(
                turma.getOrigemLatitude(),
                turma.getOrigemLongitude(),
                turma.getOrigemNome(),
                "origem"
        );

        turma.setDestinoLatitude(destino[0]);
        turma.setDestinoLongitude(destino[1]);

        turma.setOrigemLatitude(origem[0]);
        turma.setOrigemLongitude(origem[1]);

        // Nenhum endereço fictício é utilizado.
        return turmaRepository.save(turma);
    }

    public List<Turma> listarTurmasPorMotorista(
            Long motoristaId
    ) {
        return turmaRepository.findByMotoristaId(
                motoristaId
        );
    }

    public List<Usuario> listarPassageirosPorTurma(
            Long turmaId
    ) {
        List<AlunoTurma> vinculos =
                alunoTurmaRepository.findByTurmaIdAndStatus(
                        turmaId,
                        "APROVADO"
                );

        return vinculos.stream()
                .map(AlunoTurma::getAluno)
                .collect(Collectors.toList());
    }

    public void adicionarAluno(
            Long turmaId,
            Long alunoId
    ) {
        Turma turma = turmaRepository
                .findById(turmaId)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Turma não encontrada."
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
                        turmaId,
                        alunoId
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

    public void removerAluno(
            Long turmaId,
            Long alunoId
    ) {
        AlunoTurma vinculo = alunoTurmaRepository
                .findByTurmaIdAndAlunoId(
                        turmaId,
                        alunoId
                )
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Vínculo não encontrado."
                        )
                );

        alunoTurmaRepository.delete(vinculo);
    }

    public List<Turma> listarTurmasDoAluno(
            Long alunoId
    ) {
        return alunoTurmaRepository
                .findByAlunoIdAndStatus(
                        alunoId,
                        "APROVADO"
                )
                .stream()
                .map(AlunoTurma::getTurma)
                .filter(turma -> turma != null)
                .distinct()
                .toList();
    }

    // Compatibilidade com o endpoint antigo.
    public Turma buscarTurmaPorAlunoId(
            Long alunoId
    ) {
        return listarTurmasDoAluno(alunoId)
                .stream()
                .findFirst()
                .orElse(null);
    }

    public Turma atualizarTurma(
            Long turmaId,
            Turma turmaAtualizada
    ) {
        if (turmaAtualizada == null) {
            throw new IllegalArgumentException(
                    "Informe os dados da atualização."
            );
        }

        Turma turma = turmaRepository
                .findById(turmaId)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Turma não encontrada."
                        )
                );

        if (turmaAtualizada.getNome() != null
                && !turmaAtualizada.getNome().isBlank()) {

            turma.setNome(
                    turmaAtualizada.getNome().trim()
            );
        }

        if (turmaAtualizada.getTurno() != null
                && !turmaAtualizada.getTurno().isBlank()) {

            turma.setTurno(
                    turmaAtualizada.getTurno().trim()
            );
        }

        boolean destinoNomeMudou =
                turmaAtualizada.getDestinoNome() != null
                && !turmaAtualizada.getDestinoNome()
                        .equals(turma.getDestinoNome());

        boolean origemNomeMudou =
                turmaAtualizada.getOrigemNome() != null
                && !turmaAtualizada.getOrigemNome()
                        .equals(turma.getOrigemNome());

        // Atualizar destino somente se recebeu
        // coordenadas ou um nome diferente.

        if (turmaAtualizada.getDestinoLatitude() != null
                || turmaAtualizada.getDestinoLongitude() != null
                || destinoNomeMudou) {

            String nomeDestino =
                    turmaAtualizada.getDestinoNome() != null
                            ? turmaAtualizada.getDestinoNome()
                            : turma.getDestinoNome();

            double[] destino = resolverCoordenadas(
                    turmaAtualizada.getDestinoLatitude(),
                    turmaAtualizada.getDestinoLongitude(),
                    nomeDestino,
                    "destino"
            );

            turma.setDestinoLatitude(destino[0]);
            turma.setDestinoLongitude(destino[1]);
        }

        // Atualizar origem somente se recebeu
        // coordenadas ou um nome diferente.

        if (turmaAtualizada.getOrigemLatitude() != null
                || turmaAtualizada.getOrigemLongitude() != null
                || origemNomeMudou) {

            String nomeOrigem =
                    turmaAtualizada.getOrigemNome() != null
                            ? turmaAtualizada.getOrigemNome()
                            : turma.getOrigemNome();

            double[] origem = resolverCoordenadas(
                    turmaAtualizada.getOrigemLatitude(),
                    turmaAtualizada.getOrigemLongitude(),
                    nomeOrigem,
                    "origem"
            );

            turma.setOrigemLatitude(origem[0]);
            turma.setOrigemLongitude(origem[1]);
        }

        if (turmaAtualizada.getDestinoNome() != null) {
            turma.setDestinoNome(
                    turmaAtualizada.getDestinoNome()
            );
        }

        if (turmaAtualizada.getOrigemNome() != null) {
            turma.setOrigemNome(
                    turmaAtualizada.getOrigemNome()
            );
        }

        return turmaRepository.save(turma);
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

    public List<AlunoTurma> listarAlunosPorStatus(
            Long turmaId,
            String status
    ) {
        return alunoTurmaRepository
                .findByTurmaIdAndStatus(
                        turmaId,
                        status
                );
    }

    public void analisarSolicitacao(
            Long vinculoId,
            String status
    ) {
        AlunoTurma vinculo = alunoTurmaRepository
                .findById(vinculoId)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Solicitação não encontrada."
                        )
                );

        vinculo.setStatus(status);
        alunoTurmaRepository.save(vinculo);
    }

    public Turma regenerarCodigoConvite(
            Long turmaId
    ) {
        Turma turma = turmaRepository
                .findById(turmaId)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Turma não encontrada."
                        )
                );

        turma.setCodigoConvite(
                UUID.randomUUID()
                        .toString()
                        .substring(0, 6)
                        .toUpperCase()
        );

        return turmaRepository.save(turma);
    }

    public void deletarTurma(Long turmaId) {
        Turma turma = turmaRepository
                .findById(turmaId)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Turma não encontrada."
                        )
                );

        List<AlunoTurma> todosVinculos =
                alunoTurmaRepository.findAll()
                        .stream()
                        .filter(v ->
                                v.getTurma() != null
                                && v.getTurma()
                                        .getId()
                                        .equals(turmaId)
                        )
                        .toList();

        if (!todosVinculos.isEmpty()) {
            alunoTurmaRepository.deleteAll(
                    todosVinculos
            );
        }

        List<Viagem> viagensDaTurma =
                viagemRepository.findAll()
                        .stream()
                        .filter(v ->
                                v.getTurma() != null
                                && v.getTurma()
                                        .getId()
                                        .equals(turmaId)
                        )
                        .toList();

        if (!viagensDaTurma.isEmpty()) {
            viagemRepository.deleteAll(
                    viagensDaTurma
            );
        }

        turmaRepository.delete(turma);
    }
}
