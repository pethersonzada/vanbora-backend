package com.vanapp.service;

import java.time.LocalDate;
import org.springframework.stereotype.Service;
import com.vanapp.model.Endereco;
import com.vanapp.model.Presenca;
import com.vanapp.repository.EnderecoRepository;
import com.vanapp.repository.PresencaRepository;

@Service
public class PresencaService {

    private final PresencaRepository presencaRepository;
    private final EnderecoRepository enderecoRepository;

    public PresencaService(PresencaRepository presencaRepository, EnderecoRepository enderecoRepository) {
        this.presencaRepository = presencaRepository;
        this.enderecoRepository = enderecoRepository;
    }

    public void registrarPresenca(Presenca presenca, Long enderecoId) {
        if (enderecoId != null) {
            Endereco endereco = enderecoRepository.findById(enderecoId)
                    .orElseThrow(() -> new RuntimeException("Endereço não encontrado"));
            presenca.setEndereco(endereco);
        }
        
        presenca.setData(LocalDate.now());
        presencaRepository.save(presenca);
    }
}