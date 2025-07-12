package cuidar.mais.api.service;

import cuidar.mais.api.models.Usuario;
import cuidar.mais.api.repository.UsuarioRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class UsuarioService {

    @Autowired
    private UsuarioRepository usuarioRepository;

    /**
     * Busca um usuário pelo ID
     * @param id ID do usuário
     * @return Usuário encontrado
     * @throws RuntimeException se o usuário não for encontrado
     */
    public Usuario buscarPorId(Long id) {
        return usuarioRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Usuário não encontrado com ID: " + id));
    }

    /**
     * Busca um usuário pelo email
     * @param email Email do usuário
     * @return Optional contendo o usuário, se encontrado
     */
    public Optional<Usuario> buscarPorEmail(String email) {
        return usuarioRepository.findByEmail(email);
    }

    /**
     * Lista todos os usuários
     * @return Lista de usuários
     */
    public List<Usuario> listarTodos() {
        return usuarioRepository.findAll();
    }

    /**
     * Salva um usuário
     * @param usuario Usuário a ser salvo
     * @return Usuário salvo
     */
    @Transactional
    public Usuario salvar(Usuario usuario) {
        return usuarioRepository.save(usuario);
    }

    /**
     * Exclui um usuário pelo ID
     * @param id ID do usuário a ser excluído
     */
    @Transactional
    public void excluir(Long id) {
        usuarioRepository.deleteById(id);
    }
}