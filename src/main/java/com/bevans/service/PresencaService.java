package com.bevans.service;

import java.time.LocalDate;
import java.time.ZoneId;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.bevans.model.Endereco;
import com.bevans.model.Presenca;
import com.bevans.repository.EnderecoRepository;
import com.bevans.repository.PresencaRepository;

@Service
public class PresencaService {

    private final PresencaRepository presencaRepository;
    private final EnderecoRepository enderecoRepository;

    public PresencaService(PresencaRepository presencaRepository, EnderecoRepository enderecoRepository) {
        this.presencaRepository = presencaRepository;
        this.enderecoRepository = enderecoRepository;
    }

    @Transactional
    public void registrarPresenca(Presenca presenca, Long enderecoId) {
        if (enderecoId != null) {
            Endereco endereco = enderecoRepository.findById(enderecoId)
                    .orElseThrow(() -> new RuntimeException("Endereço não encontrado"));
            presenca.setEndereco(endereco);
        }
        
        presenca.setData(LocalDate.now(ZoneId.of("America/Recife")));
        presencaRepository.save(presenca);
    }
}