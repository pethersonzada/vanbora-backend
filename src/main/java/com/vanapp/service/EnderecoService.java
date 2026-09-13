package com.vanapp.service;

import java.util.List;
import org.springframework.stereotype.Service;
import com.vanapp.model.Endereco;
import com.vanapp.model.Usuario;
import com.vanapp.repository.EnderecoRepository;
import com.vanapp.repository.UsuarioRepository;

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
        return enderecoRepository.save(enderecoRequest);
    }

    public List<Endereco> listarPorUsuario(Long usuarioId) {
        return enderecoRepository.findByUsuarioId(usuarioId);
    }

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
        
        return enderecoRepository.save(endereco);
    }

    public void excluirEndereco(Long id) {
        enderecoRepository.deleteById(id);
    }
}