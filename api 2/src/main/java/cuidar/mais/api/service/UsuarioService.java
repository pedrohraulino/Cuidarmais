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
     */
    public Usuario buscarPorId(Long id) {
        System.out.println("[UsuarioService] Buscando usuário com ID: " + id);
        try {
            Usuario usuario = usuarioRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("Usuário não encontrado"));
            System.out.println("[UsuarioService] Usuário encontrado: " + usuario.getEmail() + " (ID: " + usuario.getId() + ")");
            return usuario;
        } catch (Exception e) {
            System.err.println("[UsuarioService] Erro ao buscar usuário com ID: " + id);
            e.printStackTrace();
            throw e;
        }
    }

    /**
     * Busca um usuário pelo email
     */
    public Optional<Usuario> buscarPorEmail(String email) {
        return usuarioRepository.findByEmail(email);
    }

    /**
     * Lista todos os usuários
     */
    public List<Usuario> listarTodos() {
        return usuarioRepository.findAll();
    }

    /**
     * Salva um usuário
     */
    @Transactional
    public Usuario salvar(Usuario usuario) {
        return usuarioRepository.save(usuario);
    }

    /**
     * Exclui um usuário
     */
    @Transactional
    public void excluir(Long id) {
        usuarioRepository.deleteById(id);
    }
}
