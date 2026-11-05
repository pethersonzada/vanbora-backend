package com.bevans.service;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import org.springframework.cache.annotation.Cacheable;
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

    private double calcularDistancia(double lat1, double lon1, double lat2, double lon2) {
        final int R = 6371000;
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        return R * (2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a)));
    }

    private boolean presencaValidaParaSentido(String status, String sentido) {
        if (status == null || sentido == null) {
            return false;
        }
        String st = status.trim().toUpperCase();
        String sen = sentido.trim().toUpperCase();

        if ("AMBOS".equals(st) || "IDA_VOLTA".equals(st) || "CONFIRMADO".equals(st) || "VOU".equals(st) || "PRESENTE".equals(st)) {
            return true;
        }
        if ("IDA".equals(sen) && ("IDA".equals(st) || "SO_IDA".equals(st))) {
            return true;
        }
        if ("VOLTA".equals(sen) && ("VOLTA".equals(st) || "SO_VOLTA".equals(st))) {
            return true;
        }
        return st.equalsIgnoreCase(sen);
    }

    @Async("orToolsExecutor")
    @Cacheable(value = "rotasOtimizadas", key = "#turmaId + '-' + #sentido")
    public CompletableFuture<List<PassageiroRotaDTO>> otimizarRota(String sentido, Long turmaId) {
        Turma turma = turmaRepository.findById(turmaId)
                .orElseThrow(() -> new RuntimeException("Turma não encontrada."));
        Usuario motorista = turma.getMotorista();

        if (motorista == null) {
            throw new RuntimeException(
                    "Esta turma não possui motorista associado."
            );
        }
        LocalDate hoje = LocalDate.now(ZoneId.of("America/Recife"));
        List<Presenca> presencasDoDia
                = presencaRepository.findByDataAndTurmaId(hoje, turmaId);

        List<PassageiroRotaDTO> passageirosDTO = new ArrayList<>();
        List<Double> latsList = new ArrayList<>();
        List<Double> lonsList = new ArrayList<>();

        double origemLat = turma.getOrigemLatitude() != null ? turma.getOrigemLatitude() : (motorista.getLatitude() != null ? motorista.getLatitude() : 0.0);
        double origemLon = turma.getOrigemLongitude() != null ? turma.getOrigemLongitude() : (motorista.getLongitude() != null ? motorista.getLongitude() : 0.0);

        latsList.add(origemLat);
        lonsList.add(origemLon);

        for (Presenca p : presencasDoDia) {
            if (p.getUsuario() != null && p.getStatus() != null) {
                Usuario aluno = p.getUsuario();

                if (presencaValidaParaSentido(p.getStatus(), sentido)) {
                    double latFinal = aluno.getLatitude() != null ? aluno.getLatitude() : 0.0;
                    double lonFinal = aluno.getLongitude() != null ? aluno.getLongitude() : 0.0;

                    if (p.getEndereco() != null && p.getEndereco().getLatitude() != null && p.getEndereco().getLongitude() != null) {
                        latFinal = p.getEndereco().getLatitude();
                        lonFinal = p.getEndereco().getLongitude();
                    } else if (latFinal == 0.0 || lonFinal == 0.0) {
                        List<Endereco> enderecosAluno = enderecoRepository.findByUsuarioId(aluno.getId());
                        if (enderecosAluno != null && !enderecosAluno.isEmpty()) {
                            for (Endereco end : enderecosAluno) {
                                if (end.getLatitude() != null && end.getLongitude() != null && end.getLatitude() != 0.0 && end.getLongitude() != 0.0) {
                                    latFinal = end.getLatitude();
                                    lonFinal = end.getLongitude();
                                    break;
                                }
                            }
                        }
                    }

                    if (latFinal != 0.0 && lonFinal != 0.0) {
                        passageirosDTO.add(new PassageiroRotaDTO(aluno.getId(), aluno.getNome(), latFinal, lonFinal));
                        latsList.add(latFinal);
                        lonsList.add(lonFinal);
                    }
                }
            }
        }

        if (passageirosDTO.isEmpty()) {
            throw new RuntimeException("Nenhum passageiro confirmado com endereço válido para " + sentido);
        }

        double destinoLat = turma.getDestinoLatitude() != null ? turma.getDestinoLatitude() : -8.302755;
        double destinoLon = turma.getDestinoLongitude() != null ? turma.getDestinoLongitude() : -35.991248;

        latsList.add(destinoLat);
        lonsList.add(destinoLon);

        int n = latsList.size();
        double[] lats = new double[n];
        double[] lons = new double[n];
        for (int i = 0; i < n; i++) {
            lats[i] = latsList.get(i);
            lons[i] = lonsList.get(i);
        }

        long[][] distancias = new long[n][n];
        for (int i = 0; i < n; i++) {
            for (int j = 0; j < n; j++) {
                distancias[i][j] = (long) calcularDistancia(lats[i], lons[i], lats[j], lons[j]);
            }
        }

        RoutingIndexManager manager = new RoutingIndexManager(n, 1, 0);
        RoutingModel routing = new RoutingModel(manager);
        routing.setArcCostEvaluatorOfAllVehicles(routing.registerTransitCallback(
                (f, t) -> distancias[manager.indexToNode(f)][manager.indexToNode(t)]));

        Assignment solution = routing.solveWithParameters(
                main.defaultRoutingSearchParameters().toBuilder()
                        .setFirstSolutionStrategy(FirstSolutionStrategy.Value.PATH_CHEAPEST_ARC)
                        .build());

        if (solution == null) {
            throw new RuntimeException("Falha na otimização da rota. Nenhum caminho encontrado.");
        }

        List<PassageiroRotaDTO> rotaOtimizada = new ArrayList<>();
        long index = routing.start(0);
        while ((index = solution.value(routing.nextVar(index))) != routing.end(0)) {
            int nodeIndex = manager.indexToNode(index);
            if (nodeIndex > 0 && nodeIndex < latsList.size() - 1) {
                rotaOtimizada.add(passageirosDTO.get(nodeIndex - 1));
            }
        }

        if ("volta".equalsIgnoreCase(sentido)) {
            Collections.reverse(rotaOtimizada);
        }

        return CompletableFuture.completedFuture(rotaOtimizada);
    }
}
