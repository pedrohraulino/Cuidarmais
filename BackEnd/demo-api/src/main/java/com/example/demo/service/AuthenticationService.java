package com.example.demo.service;

import com.example.demo.models.Usuario;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import com.example.demo.repository.UsuarioRepository;

@Service
public class AuthenticationService {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder encoder;
    private final JwtService jwtService;

    public AuthenticationService(UsuarioRepository usuarioRepository, PasswordEncoder encoder, JwtService jwtService) {
        this.usuarioRepository = usuarioRepository;
        this.encoder = encoder;
        this.jwtService = jwtService;
    }

    public String autenticar(String email, String senha) {
        // Usuário de teste hardcoded
        if ("teste@teste.com".equals(email) && "12345".equals(senha)) {
            Usuario usuarioFake = new Usuario();
            usuarioFake.setId(0L);
            usuarioFake.setEmail(email);
            usuarioFake.setNome("Usuário de Teste");
            usuarioFake.setAtivo(true);
            return jwtService.gerarToken(usuarioFake);
        }

        // Autenticação real via banco de dados
        Usuario usuario = usuarioRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Usuário não encontrado"));

        if (!usuario.getAtivo()) {
            throw new RuntimeException("Usuário inativo");
        }

        if (!encoder.matches(senha, usuario.getSenha())) {
            throw new RuntimeException("Senha inválida");
        }

        return jwtService.gerarToken(usuario);
    }

}

