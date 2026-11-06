
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

        return R * (
                2 * Math.atan2(
                        Math.sqrt(a),
                        Math.sqrt(1 - a)
                )
        );
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
                && ("IDA".equals(st)
                || "SO_IDA".equals(st))) {
            return true;
        }

        if ("VOLTA".equals(sen)
                && ("VOLTA".equals(st)
                || "SO_VOLTA".equals(st))) {
            return true;
        }

        return st.equals(sen);
    }

    private Endereco obterEnderecoPorSentido(
            Presenca presenca,
            boolean isVolta
    ) {
        // Cada sentido utiliza seu próprio endereço.
        Endereco endereco = isVolta
                ? presenca.getEnderecoVolta()
                : presenca.getEnderecoIda();

        if (endereco != null) {
            return endereco;
        }

        // Fallback somente para registros históricos
        // que ainda não possuem endereços por sentido.
        if (presenca.getTurma() == null) {
            return presenca.getEndereco();
        }

        return null;
    }

    @Async("orToolsExecutor")
    public CompletableFuture<List<PassageiroRotaDTO>>
    otimizarRota(
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

        if (turmaId == null) {
            throw new IllegalArgumentException(
                    "O ID da turma é obrigatório."
            );
        }

        boolean isVolta =
                sentido.equalsIgnoreCase("VOLTA");

        // 1. Buscar a turma.

        Turma turma = turmaRepository.findById(turmaId)
                .orElseThrow(() ->
                        new RuntimeException(
                                "Turma não encontrada."
                        )
                );

        Usuario motorista = turma.getMotorista();

        if (motorista == null) {
            throw new RuntimeException(
                    "Esta turma não possui motorista associado."
            );
        }

        // 2. Buscar presenças apenas da turma selecionada.

        LocalDate hoje = LocalDate.now(
                ZoneId.of("America/Recife")
        );

        List<Presenca> presencasDoDia =
                presencaRepository.findByDataAndTurmaId(
                        hoje,
                        turmaId
                );

        List<PassageiroRotaDTO> passageirosDTO =
                new ArrayList<>();

        List<Double> latsList = new ArrayList<>();
        List<Double> lonsList = new ArrayList<>();

        // 3. Definir a origem cadastrada para a turma.

        Double origemLat = turma.getOrigemLatitude();
        Double origemLon = turma.getOrigemLongitude();

        if (!coordenadasValidas(origemLat, origemLon)) {
            origemLat = motorista.getLatitude();
            origemLon = motorista.getLongitude();
        }

        if (!coordenadasValidas(origemLat, origemLon)) {
            throw new RuntimeException(
                    "A turma não possui coordenadas de origem "
                    + "válidas e o motorista não possui "
                    + "localização disponível."
            );
        }

        latsList.add(origemLat);
        lonsList.add(origemLon);

        // 4. Filtrar passageiros pelo sentido da viagem.

        for (Presenca presenca : presencasDoDia) {

            if (presenca.getUsuario() == null
                    || presenca.getStatus() == null) {
                continue;
            }

            if (!presencaValidaParaSentido(
                    presenca.getStatus(),
                    sentido
            )) {
                continue;
            }

            Usuario aluno = presenca.getUsuario();

            // 5. Selecionar o endereço correspondente.
            // IDA: embarque.
            // VOLTA: desembarque.

            Endereco enderecoEscolhido =
                    obterEnderecoPorSentido(
                            presenca,
                            isVolta
                    );

            if (enderecoEscolhido == null) {
                continue;
            }

            Double latFinal =
                    enderecoEscolhido.getLatitude();

            Double lonFinal =
                    enderecoEscolhido.getLongitude();

            // Endereços inválidos não entram na rota.
            // Nunca substituir pelo endereço de outro sentido.

            if (!coordenadasValidas(
                    latFinal,
                    lonFinal
            )) {
                continue;
            }

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

        // 6. Nenhum passageiro confirmado para este sentido.

        if (passageirosDTO.isEmpty()) {
            return CompletableFuture.completedFuture(
                    new ArrayList<>()
            );
        }

        // 7. Validar destino da turma.

        Double destinoLat = turma.getDestinoLatitude();
        Double destinoLon = turma.getDestinoLongitude();

        if (!coordenadasValidas(
                destinoLat,
                destinoLon
        )) {
            throw new RuntimeException(
                    "A turma não possui coordenadas de "
                    + "destino válidas."
            );
        }

        latsList.add(destinoLat);
        lonsList.add(destinoLon);

        // 8. Converter coordenadas em arrays.

        int n = latsList.size();

        double[] lats = new double[n];
        double[] lons = new double[n];

        for (int i = 0; i < n; i++) {
            lats[i] = latsList.get(i);
            lons[i] = lonsList.get(i);
        }

        // 9. Calcular matriz de distâncias.

        long[][] distancias = new long[n][n];

        for (int i = 0; i < n; i++) {
            for (int j = 0; j < n; j++) {
                distancias[i][j] =
                        (long) calcularDistancia(
                                lats[i],
                                lons[i],
                                lats[j],
                                lons[j]
                        );
            }
        }

        // 10. Sentido da viagem.
        //
        // IDA:
        // origem -> passageiros -> destino
        //
        // VOLTA:
        // destino -> passageiros -> origem

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

        RoutingModel routing =
                new RoutingModel(manager);

        // 11. Configurar custo de deslocamento.

        int transitCallbackIndex =
                routing.registerTransitCallback(
                        (fromIndex, toIndex) -> {
                            int fromNode =
                                    manager.indexToNode(
                                            fromIndex
                                    );

                            int toNode =
                                    manager.indexToNode(
                                            toIndex
                                    );

                            return distancias[fromNode][toNode];
                        }
                );

        routing.setArcCostEvaluatorOfAllVehicles(
                transitCallbackIndex
        );

        // 12. Executar algoritmo OR-Tools.

        Assignment solution =
                routing.solveWithParameters(
                        main.defaultRoutingSearchParameters()
                                .toBuilder()
                                .setFirstSolutionStrategy(
                                        FirstSolutionStrategy.Value
                                                .PATH_CHEAPEST_ARC
                                )
                                .build()
                );

        if (solution == null) {
            throw new RuntimeException(
                    "Falha na otimização da rota. "
                    + "Nenhum caminho encontrado."
            );
        }

        // 13. Montar sequência de passageiros otimizada.

        List<PassageiroRotaDTO> rotaOtimizada =
                new ArrayList<>();

        long index = routing.start(0);

        while (!routing.isEnd(index)) {
            int nodeIndex = manager.indexToNode(index);

            // Ignorar nós de origem e destino.
            if (nodeIndex > 0 && nodeIndex < n - 1) {
                rotaOtimizada.add(
                        passageirosDTO.get(nodeIndex - 1)
                );
            }

            index = solution.value(
                    routing.nextVar(index)
            );
        }

        // 14. Retornar ordem de paradas.

        return CompletableFuture.completedFuture(
                rotaOtimizada
        );
    }
}
