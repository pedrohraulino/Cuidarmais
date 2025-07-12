package cuidar.mais.api.controller;

import cuidar.mais.api.models.Usuario;
import cuidar.mais.api.service.ImagemBase64Service;
import cuidar.mais.api.service.UsuarioService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/usuarios")
@CrossOrigin(origins = {"http://localhost:4200", "http://localhost:3000"})
public class UsuarioController {
    @Autowired
    private UsuarioService usuarioService;

    @Autowired
    private ImagemBase64Service imagemService;
    private static final Logger logger = LoggerFactory.getLogger(UsuarioController.class);

    @GetMapping("/me")
    public ResponseEntity<?> dadosUsuarioLogado(@AuthenticationPrincipal Usuario usuario) {
        if (usuario == null) {
            logger.warn("Tentativa de acesso a /usuarios/me sem autenticação");
            Map<String, String> erro = new HashMap<>();
            erro.put("erro", "Usuário não autenticado");
            return ResponseEntity.status(401).body(erro);
        }

        logger.info("Dados do usuário {} solicitados", usuario.getEmail());

        Map<String, Object> userData = new HashMap<>();
        userData.put("id", usuario.getId());
        userData.put("nome", usuario.getNome());
        userData.put("email", usuario.getEmail());
        userData.put("perfis", usuario.getAuthorities().stream().map(GrantedAuthority::getAuthority).toList());

        return ResponseEntity.ok(userData);
    }



}
