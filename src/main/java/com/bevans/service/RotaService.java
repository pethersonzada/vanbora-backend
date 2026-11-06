package com.bevans.service;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import com.bevans.dto.PassageiroRotaDTO;
import com.bevans.model.Endereco;
import com.bevans.model.Presenca;
import com.bevans.model.Turma;
import com.bevans.model.Usuario;
import com.bevans.repository.EnderecoRepository;
import com.bevans.repository.PresencaRepository;
import com.bevans.repository.TurmaRepository;
import com.google.ortools.Loader;
import com.google.ortools.constraintsolver.Assignment;
import com.google.ortools.constraintsolver.FirstSolutionStrategy;
import com.google.ortools.constraintsolver.RoutingIndexManager;
import com.google.ortools.constraintsolver.RoutingModel;
import com.google.ortools.constraintsolver.main;

@Service
public class RotaService {

    private final PresencaRepository presencaRepository;
    private final TurmaRepository turmaRepository;
    private final EnderecoRepository enderecoRepository;

    static {
        Loader.loadNativeLibraries();
    }

    public RotaService(
            PresencaRepository presencaRepository,
            TurmaRepository turmaRepository,
            EnderecoRepository enderecoRepository
    ) {
        this.presencaRepository = presencaRepository;
        this.turmaRepository = turmaRepository;
        this.enderecoRepository = enderecoRepository;
    }

    private double calcularDistancia(
            double lat1,
            double lon1,
            double lat2,
            double lon2
    ) {
        final int R = 6371000;

        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);

        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1))
                * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLon / 2)
                * Math.sin(dLon / 2);

        return R * (2 * Math.atan2(
                Math.sqrt(a),
                Math.sqrt(1 - a)
        ));
    }

    private boolean coordenadasValidas(
            Double latitude,
            Double longitude
    ) {
        return latitude != null
                && longitude != null
                && Double.isFinite(latitude)
                && Double.isFinite(longitude)
                && latitude >= -90
                && latitude <= 90
                && longitude >= -180
                && longitude <= 180
                && !(latitude == 0.0 && longitude == 0.0);
    }

    private boolean presencaValidaParaSentido(
            String status,
            String sentido
    ) {
        if (status == null || sentido == null) {
            return false;
        }

        String st = status.trim().toUpperCase();
        String sen = sentido.trim().toUpperCase();

        if ("AMBOS".equals(st)
                || "IDA_VOLTA".equals(st)
                || "CONFIRMADO".equals(st)
                || "VOU".equals(st)
                || "PRESENTE".equals(st)) {
            return true;
        }

        if ("IDA".equals(sen)
                && ("IDA".equals(st) || "SO_IDA".equals(st))) {
            return true;
        }

        if ("VOLTA".equals(sen)
                && ("VOLTA".equals(st) || "SO_VOLTA".equals(st))) {
            return true;
        }

        return st.equals(sen);
    }

    @Async("orToolsExecutor")
    public CompletableFuture<List<PassageiroRotaDTO>> otimizarRota(
            String sentido,
            Long turmaId
    ) {

        if (sentido == null
                || (!sentido.equalsIgnoreCase("IDA")
                && !sentido.equalsIgnoreCase("VOLTA"))) {

            throw new IllegalArgumentException(
                    "Sentido inválido. Informe IDA ou VOLTA."
            );
        }

        boolean isVolta = sentido.equalsIgnoreCase("VOLTA");

        // 1. Buscar a turma

        Turma turma = turmaRepository.findById(turmaId)
                .orElseThrow(() ->
                        new RuntimeException("Turma não encontrada.")
                );

        // 2. Obter o motorista vinculado à turma

        Usuario motorista = turma.getMotorista();

        if (motorista == null) {
            throw new RuntimeException(
                    "Esta turma não possui motorista associado."
            );
        }

        // 3. Buscar presenças do dia somente da turma

        LocalDate hoje = LocalDate.now(
                ZoneId.of("America/Recife")
        );

        List<Presenca> presencasDoDia =
                presencaRepository.findByDataAndTurmaId(
                        hoje,
                        turmaId
                );

        // 4. Preparar listas de passageiros e coordenadas

        List<PassageiroRotaDTO> passageirosDTO =
                new ArrayList<>();

        List<Double> latsList = new ArrayList<>();
        List<Double> lonsList = new ArrayList<>();

        // 5. Validar coordenadas da origem

        Double origemLat = turma.getOrigemLatitude();
        Double origemLon = turma.getOrigemLongitude();

        if (!coordenadasValidas(origemLat, origemLon)) {
            origemLat = motorista.getLatitude();
            origemLon = motorista.getLongitude();
        }

        if (!coordenadasValidas(origemLat, origemLon)) {
            throw new RuntimeException(
                    "A turma não possui coordenadas de origem válidas "
                    + "e o motorista não possui localização disponível."
            );
        }

        latsList.add(origemLat);
        lonsList.add(origemLon);

        // 6. Filtrar e validar passageiros

        for (Presenca p : presencasDoDia) {

            if (p.getUsuario() == null || p.getStatus() == null) {
                continue;
            }

            Usuario aluno = p.getUsuario();

            if (!presencaValidaParaSentido(
                    p.getStatus(),
                    sentido
            )) {
                continue;
            }

            Double latFinal = aluno.getLatitude();
            Double lonFinal = aluno.getLongitude();

            // Priorizar o endereço informado na presença

            if (p.getEndereco() != null
                    && coordenadasValidas(
                            p.getEndereco().getLatitude(),
                            p.getEndereco().getLongitude()
                    )) {

                latFinal = p.getEndereco().getLatitude();
                lonFinal = p.getEndereco().getLongitude();

            } else if (!coordenadasValidas(
                    latFinal,
                    lonFinal
            )) {

                // Procurar endereço alternativo do aluno

                List<Endereco> enderecosAluno =
                        enderecoRepository.findByUsuarioId(
                                aluno.getId()
                        );

                if (enderecosAluno != null) {

                    for (Endereco end : enderecosAluno) {

                        if (coordenadasValidas(
                                end.getLatitude(),
                                end.getLongitude()
                        )) {

                            latFinal = end.getLatitude();
                            lonFinal = end.getLongitude();

                            break;
                        }
                    }
                }
            }

            // Adicionar somente passageiros com coordenadas válidas

            if (coordenadasValidas(
                    latFinal,
                    lonFinal
            )) {

                passageirosDTO.add(
                        new PassageiroRotaDTO(
                                aluno.getId(),
                                aluno.getNome(),
                                latFinal,
                                lonFinal
                        )
                );

                latsList.add(latFinal);
                lonsList.add(lonFinal);
            }
        }

        // 7. Verificar se existem passageiros válidos

        if (passageirosDTO.isEmpty()) {
                return CompletableFuture.completedFuture(
                        new ArrayList<PassageiroRotaDTO>()
                        );
        }

        // 8. Validar coordenadas do destino

        Double destinoLat = turma.getDestinoLatitude();
        Double destinoLon = turma.getDestinoLongitude();

        if (!coordenadasValidas(
                destinoLat,
                destinoLon
        )) {

            throw new RuntimeException(
                    "A turma não possui coordenadas de destino válidas. "
                    + "Configure o destino antes de otimizar a rota."
            );
        }

        latsList.add(destinoLat);
        lonsList.add(destinoLon);

        // 9. Converter coordenadas para arrays

        int n = latsList.size();

        double[] lats = new double[n];
        double[] lons = new double[n];

        for (int i = 0; i < n; i++) {
            lats[i] = latsList.get(i);
            lons[i] = lonsList.get(i);
        }

        // 10. Calcular matriz de distâncias

        long[][] distancias = new long[n][n];

        for (int i = 0; i < n; i++) {

            for (int j = 0; j < n; j++) {

                distancias[i][j] = (long) calcularDistancia(
                        lats[i],
                        lons[i],
                        lats[j],
                        lons[j]
                );
            }
        }

        // 11. Definir origem e destino conforme o sentido

        int[] inicios = {
            isVolta ? n - 1 : 0
        };

        int[] finais = {
            isVolta ? 0 : n - 1
        };

        RoutingIndexManager manager =
                new RoutingIndexManager(
                        n,
                        1,
                        inicios,
                        finais
                );

        // 12. Configurar modelo de otimização

        RoutingModel routing = new RoutingModel(manager);

        routing.setArcCostEvaluatorOfAllVehicles(
                routing.registerTransitCallback(
                        (f, t) -> distancias[
                                manager.indexToNode(f)
                        ][
                                manager.indexToNode(t)
                        ]
                )
        );

        // 13. Executar otimização

        Assignment solution = routing.solveWithParameters(
                main.defaultRoutingSearchParameters()
                        .toBuilder()
                        .setFirstSolutionStrategy(
                                FirstSolutionStrategy.Value.PATH_CHEAPEST_ARC
                        )
                        .build()
        );

        if (solution == null) {
            throw new RuntimeException(
                    "Falha na otimização da rota. "
                    + "Nenhum caminho encontrado."
            );
        }

        // 14. Montar rota otimizada

        List<PassageiroRotaDTO> rotaOtimizada =
                new ArrayList<>();

        long index = routing.start(0);

        while (!routing.isEnd(index)) {

            int nodeIndex = manager.indexToNode(index);

            if (nodeIndex > 0 && nodeIndex < n - 1) {

                rotaOtimizada.add(
                        passageirosDTO.get(nodeIndex - 1)
                );
            }

            index = solution.value(
                    routing.nextVar(index)
            );
        }

        // 15. Retornar resultado

        return CompletableFuture.completedFuture(
                rotaOtimizada
        );
    }
}