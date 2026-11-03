package com.vanapp.service;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import org.springframework.stereotype.Service;
import com.vanapp.model.Turma;
import com.vanapp.model.Viagem;
import com.vanapp.repository.TurmaRepository;
import com.vanapp.repository.ViagemRepository;

@Service
public class ViagemService {

    private final ViagemRepository viagemRepository;
    private final TurmaRepository turmaRepository;

    public ViagemService(ViagemRepository viagemRepository, TurmaRepository turmaRepository) {
        this.viagemRepository = viagemRepository;
        this.turmaRepository = turmaRepository;
    }

    public void iniciarRota(Long turmaId, String sentido) {
        Turma turma = turmaRepository.findById(turmaId)
                .orElseThrow(() -> new RuntimeException("Turma não encontrada no sistema."));

        Optional<Viagem> viagemAtiva = viagemRepository.findFirstByTurmaIdAndStatus(turmaId, "EM_ANDAMENTO");
        
        if (viagemAtiva.isPresent()) {
            throw new RuntimeException("Já existe uma rota em andamento exclusivamente para esta turma.");
        }

        Viagem novaViagem = new Viagem();
        novaViagem.setTurma(turma);
        novaViagem.setSentido(sentido);
        novaViagem.setStatus("EM_ANDAMENTO");

        viagemRepository.save(novaViagem);
    }

    public void encerrarRota(Long turmaId) {
        Optional<Viagem> viagemAtiva = viagemRepository.findFirstByTurmaIdAndStatus(turmaId, "EM_ANDAMENTO");
        
        if (viagemAtiva.isPresent()) {
            Viagem viagem = viagemAtiva.get();
            viagem.setStatus("FINALIZADA");
            viagemRepository.save(viagem);
        } else {
            throw new RuntimeException("Tentativa de encerrar uma rota que não está em andamento para esta turma.");
        }
    }

    public Map<String, Object> verificarStatusAtual(Long turmaId) {
        Optional<Viagem> viagemAtiva = viagemRepository.findFirstByTurmaIdAndStatus(turmaId, "EM_ANDAMENTO");
        Map<String, Object> resposta = new HashMap<>();

        if (viagemAtiva.isPresent()) {
            Viagem v = viagemAtiva.get();
            resposta.put("status", "ATIVA");
            resposta.put("sentido", v.getSentido());
            resposta.put("turmaId", v.getTurma().getId());
            resposta.put("turmaNome", v.getTurma().getNome());
        } else {
            resposta.put("status", "INATIVA");
        }
        return resposta;
    }
}