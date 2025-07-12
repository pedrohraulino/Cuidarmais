package cuidar.mais.api.controller;

import jakarta.validation.Valid;
import cuidar.mais.api.dto.LoginRequest;
import cuidar.mais.api.dto.TokenResponse;
import cuidar.mais.api.models.Perfil;
import cuidar.mais.api.models.Usuario;
import cuidar.mais.api.repository.UsuarioRepository;
import cuidar.mais.api.security.TokenService;
import cuidar.mais.api.service.AutenticacaoService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/auth")
@CrossOrigin(origins = {"http://localhost:4200", "http://localhost:3000"})
public class AutenticacaoController {
    private static final Logger logger = LoggerFactory.getLogger(AutenticacaoController.class);

    @Autowired
    private AuthenticationManager authManager;

    @Autowired
    private TokenService tokenService;

    @Autowired
    private AutenticacaoService autenticacaoService;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private org.springframework.security.crypto.password.PasswordEncoder passwordEncoder;

    @PostMapping("/login")
    @Transactional
    public ResponseEntity<?> autenticar(@RequestBody @Valid LoginRequest login) {
        logger.info("Tentativa de login recebida: {}", login);
        logger.info("Tentativa de login para o email: {}", login.getEmail());

        if (login.getEmail() == null || login.getEmail().isEmpty()) {
            logger.warn("Email não fornecido na requisição de login");
            Map<String, String> erro = new HashMap<>();
            erro.put("erro", "Email não fornecido");
            return ResponseEntity.status(400).body(erro);
        }

        if (login.getSenha() == null || login.getSenha().isEmpty()) {
            logger.warn("Senha não fornecida na requisição de login para o email: {}", login.getEmail());
            Map<String, String> erro = new HashMap<>();
            erro.put("erro", "Senha não fornecida");
            return ResponseEntity.status(400).body(erro);
        }

        try {
            // Verificar se o usuário existe
            logger.debug("Verificando se o usuário existe: {}", login.getEmail());
            boolean userExists = usuarioRepository.findByEmail(login.getEmail()).isPresent();
            logger.debug("Usuário existe: {}", userExists);

            if (!userExists) {
                logger.warn("Email não encontrado: {}", login.getEmail());
                Map<String, String> erro = new HashMap<>();
                erro.put("erro", "Email não encontrado");
                return ResponseEntity.status(401).body(erro);
            }

            UsernamePasswordAuthenticationToken dadosLogin =
                    new UsernamePasswordAuthenticationToken(login.getEmail(), login.getSenha());

            logger.debug("Tentando autenticar usuário com email: {}", login.getEmail());
            Authentication autenticacao;
            try {
                autenticacao = authManager.authenticate(dadosLogin);
            } catch (BadCredentialsException e) {
                logger.warn("Senha incorreta para o email: {}", login.getEmail());
                Map<String, String> erro = new HashMap<>();
                erro.put("erro", "Senha incorreta");
                return ResponseEntity.status(401).body(erro);
            }

            if (autenticacao == null) {
                logger.error("Autenticação retornou null para o usuário: {}", login.getEmail());
                Map<String, String> erro = new HashMap<>();
                erro.put("erro", "Falha na autenticação");
                return ResponseEntity.status(401).body(erro);
            }

            if (!autenticacao.isAuthenticated()) {
                logger.error("Usuário não autenticado: {}", login.getEmail());
                Map<String, String> erro = new HashMap<>();
                erro.put("erro", "Usuário não autenticado");
                return ResponseEntity.status(401).body(erro);
            }

            logger.debug("Autenticação bem-sucedida, obtendo principal");
            Object principal = autenticacao.getPrincipal();

            if (principal == null) {
                logger.error("Principal é null para o usuário: {}", login.getEmail());
                Map<String, String> erro = new HashMap<>();
                erro.put("erro", "Erro ao obter dados do usuário");
                return ResponseEntity.status(500).body(erro);
            }

            if (!(principal instanceof Usuario)) {
                logger.error("Principal não é uma instância de Usuario: {}", principal.getClass().getName());
                Map<String, String> erro = new HashMap<>();
                erro.put("erro", "Tipo de usuário inválido");
                return ResponseEntity.status(500).body(erro);
            }

            Usuario usuario = (Usuario) principal;
            logger.info("Usuário autenticado com sucesso: {}", usuario.getEmail());

            try {
                logger.debug("Gerando token para o usuário: {}", usuario.getEmail());
                String token = tokenService.gerarToken(usuario);
                logger.debug("Token gerado com sucesso");

                // Use TokenResponse class
                return ResponseEntity.ok(new TokenResponse(token));
            } catch (Exception e) {
                logger.error("Erro ao gerar token: {}", e.getMessage(), e);
                Map<String, String> erro = new HashMap<>();
                erro.put("erro", "Erro ao gerar token: " + e.getMessage());
                return ResponseEntity.status(500).body(erro);
            }

        } catch (AuthenticationException e) {
            // This will catch other authentication exceptions, but not BadCredentialsException
            // since it's already handled above
            logger.error("Erro de autenticação: {}", e.getMessage(), e);
            Map<String, String> erro = new HashMap<>();
            erro.put("erro", "Erro de autenticação: " + e.getMessage());
            return ResponseEntity.status(401).body(erro);

        } catch (Exception e) {
            logger.error("Erro inesperado durante autenticação: {}", e.getMessage(), e);
            Map<String, String> erro = new HashMap<>();
            erro.put("erro", "Erro interno do servidor");
            return ResponseEntity.status(500).body(erro);
        }
    }

    @GetMapping("/status")
    public ResponseEntity<Map<String, String>> status() {
        Map<String, String> response = new HashMap<>();
        response.put("status", "Serviço de autenticação está funcionando");
        return ResponseEntity.ok(response);
    }

    @GetMapping("/db-status")
    @Transactional
    public ResponseEntity<?> dbStatus() {
        try {
            logger.info("Verificando conexão com o banco de dados");
            long count = usuarioRepository.count();
            Map<String, Object> response = new HashMap<>();
            response.put("status", "Conexão com o banco de dados está funcionando");
            response.put("userCount", count);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Erro ao verificar conexão com o banco de dados: {}", e.getMessage(), e);
            Map<String, String> erro = new HashMap<>();
            erro.put("erro", "Erro ao verificar conexão com o banco de dados: " + e.getMessage());
            return ResponseEntity.status(500).body(erro);
        }
    }

    @GetMapping("/check-password")
    public ResponseEntity<?> checkPassword(@RequestParam String raw, @RequestParam String encoded) {
        try {
            logger.info("Verificando senha");
            boolean matches = passwordEncoder.matches(raw, encoded);
            Map<String, Object> response = new HashMap<>();
            response.put("matches", matches);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Erro ao verificar senha: {}", e.getMessage(), e);
            Map<String, String> erro = new HashMap<>();
            erro.put("erro", "Erro ao verificar senha: " + e.getMessage());
            return ResponseEntity.status(500).body(erro);
        }
    }

    @GetMapping("/encode-password")
    public ResponseEntity<?> encodePassword(@RequestParam String raw) {
        try {
            logger.info("Codificando senha");
            String encoded = passwordEncoder.encode(raw);
            Map<String, Object> response = new HashMap<>();
            response.put("raw", raw);
            response.put("encoded", encoded);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Erro ao codificar senha: {}", e.getMessage(), e);
            Map<String, String> erro = new HashMap<>();
            erro.put("erro", "Erro ao codificar senha: " + e.getMessage());
            return ResponseEntity.status(500).body(erro);
        }
    }

    @GetMapping("/test-auth")
    @Transactional
    public ResponseEntity<?> testAuth(@RequestParam String email, @RequestParam String senha) {
        try {
            logger.info("Testando autenticação direta para: {}", email);
            UsernamePasswordAuthenticationToken authToken = 
                new UsernamePasswordAuthenticationToken(email, senha);

            Authentication auth = authManager.authenticate(authToken);

            Map<String, Object> response = new HashMap<>();
            response.put("authenticated", auth.isAuthenticated());
            response.put("principal", auth.getPrincipal().toString());
            response.put("authorities", auth.getAuthorities().toString());
            return ResponseEntity.ok(response);
        } catch (BadCredentialsException e) {
            logger.warn("Credenciais inválidas para: {}", email);
            Map<String, String> erro = new HashMap<>();
            erro.put("erro", "Credenciais inválidas");
            return ResponseEntity.status(401).body(erro);
        } catch (Exception e) {
            logger.error("Erro ao testar autenticação: {}", e.getMessage(), e);
            Map<String, String> erro = new HashMap<>();
            erro.put("erro", "Erro ao testar autenticação: " + e.getMessage());
            return ResponseEntity.status(500).body(erro);
        }
    }

    @GetMapping("/check-user")
    @Transactional
    public ResponseEntity<?> checkUser(@RequestParam String email) {
        try {
            logger.info("Verificando se usuário existe: {}", email);
            UserDetails userDetails = autenticacaoService.loadUserByUsername(email);
            Map<String, Object> response = new HashMap<>();
            response.put("exists", true);
            response.put("enabled", userDetails.isEnabled());
            response.put("username", userDetails.getUsername());
            return ResponseEntity.ok(response);
        } catch (UsernameNotFoundException e) {
            logger.warn("Usuário não encontrado: {}", email);
            Map<String, Object> response = new HashMap<>();
            response.put("exists", false);
            response.put("message", "Usuário não encontrado");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Erro ao verificar usuário: {}", e.getMessage(), e);
            Map<String, String> erro = new HashMap<>();
            erro.put("erro", "Erro ao verificar usuário: " + e.getMessage());
            return ResponseEntity.status(500).body(erro);
        }
    }
    @PostMapping("/create-test-user")
    @Transactional
    public ResponseEntity<?> createTestUser(@RequestParam String email, @RequestParam String senha, @RequestParam String nome) {
        try {
            logger.info("Tentando criar usuário de teste: {}", email);

            // Verificar se o usuário já existe
            if (usuarioRepository.findByEmail(email).isPresent()) {
                logger.warn("Usuário já existe: {}", email);
                Map<String, String> response = new HashMap<>();
                response.put("message", "Usuário já existe");
                return ResponseEntity.badRequest().body(response);
            }

            // Criar novo usuário
            Usuario usuario = new Usuario();
            usuario.setEmail(email);
            usuario.setSenha(passwordEncoder.encode(senha));
            usuario.setNome(nome);
            usuario.setAtivo(true);
            usuario.setDataCriacao(LocalDateTime.now());
            usuario.setPerfis(new ArrayList<>());

            // Salvar usuário
            usuarioRepository.save(usuario);

            logger.info("Usuário de teste criado com sucesso: {}", email);
            Map<String, String> response = new HashMap<>();
            response.put("message", "Usuário criado com sucesso");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Erro ao criar usuário de teste: {}", e.getMessage(), e);
            Map<String, String> erro = new HashMap<>();
            erro.put("erro", "Erro ao criar usuário: " + e.getMessage());
            return ResponseEntity.status(500).body(erro);
        }
    }
}
