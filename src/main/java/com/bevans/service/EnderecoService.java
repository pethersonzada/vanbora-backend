package com.bevans.service;

import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import org.springframework.stereotype.Service;
import com.bevans.model.Endereco;
import com.bevans.model.Usuario;
import com.bevans.repository.EnderecoRepository;
import com.bevans.repository.UsuarioRepository;

@Service
public class EnderecoService {

    private final EnderecoRepository enderecoRepository;
    private final UsuarioRepository usuarioRepository;

    public EnderecoService(EnderecoRepository enderecoRepository, UsuarioRepository usuarioRepository) {
        this.enderecoRepository = enderecoRepository;
        this.usuarioRepository = usuarioRepository;
    }

    public Endereco cadastrarEndereco(Long usuarioId, Endereco enderecoRequest) {
        Usuario usuario = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new RuntimeException("Usuário não encontrado"));

        enderecoRequest.setUsuario(usuario);
        Endereco salvo = enderecoRepository.save(enderecoRequest);

        if (enderecoRequest.getLatitude() != null && enderecoRequest.getLongitude() != null) {
            usuario.setLatitude(enderecoRequest.getLatitude());
            usuario.setLongitude(enderecoRequest.getLongitude());
            usuarioRepository.save(usuario);
        }

        return salvo;
    }

    public List<Endereco> listarPorUsuario(Long usuarioId) {
        return enderecoRepository.findByUsuarioId(usuarioId);
    }

    @Transactional
    public Endereco atualizarEndereco(Long id, Endereco enderecoRequest) {
        Endereco endereco = enderecoRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Endereço não encontrado"));

        if (enderecoRequest.getApelido() != null) {
            endereco.setApelido(enderecoRequest.getApelido());
        }
        if (enderecoRequest.getRua() != null) {
            endereco.setRua(enderecoRequest.getRua());
        }
        if (enderecoRequest.getNumero() != null) {
            endereco.setNumero(enderecoRequest.getNumero());
        }
        if (enderecoRequest.getBairro() != null) {
            endereco.setBairro(enderecoRequest.getBairro());
        }


if (enderecoRequest.getLatitude() != null
        || enderecoRequest.getLongitude() != null) {

    Double latitude = enderecoRequest.getLatitude();
    Double longitude = enderecoRequest.getLongitude();

    if (latitude == null || longitude == null
            || !Double.isFinite(latitude)
            || !Double.isFinite(longitude)
            || latitude < -90 || latitude > 90
            || longitude < -180 || longitude > 180
            || (latitude == 0.0 && longitude == 0.0)) {
        throw new IllegalArgumentException(
                "Coordenadas do endereço inválidas."
        );
    }

    endereco.setLatitude(latitude);
    endereco.setLongitude(longitude);
}


        return enderecoRepository.save(endereco);
    }

    public void excluirEndereco(Long id) {
        if (!enderecoRepository.existsById(id)) {
            throw new RuntimeException("Endereço não encontrado");
        }
        enderecoRepository.deleteById(id);
    }
}