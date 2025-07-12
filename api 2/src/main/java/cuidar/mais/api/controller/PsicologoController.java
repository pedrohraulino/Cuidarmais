package cuidar.mais.api.controller;

import cuidar.mais.api.dto.PsicologoDTO;
import cuidar.mais.api.models.Usuario;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/psicologo")
@CrossOrigin(origins = {"http://localhost:4200", "http://localhost:3000"})
public class PsicologoController {
    private static final Logger logger = LoggerFactory.getLogger(PsicologoController.class);

    @GetMapping("/me")
    public ResponseEntity<?> dadosPsicologoLogado(@AuthenticationPrincipal Usuario usuario) {
        if (usuario == null) {
            logger.warn("Tentativa de acesso a /psicologo/me sem autenticação");
            Map<String, String> erro = new HashMap<>();
            erro.put("erro", "Usuário não autenticado");
            return ResponseEntity.status(401).body(erro);
        }

        logger.info("Dados do psicólogo {} solicitados", usuario.getEmail());

        PsicologoDTO psicologoDTO = new PsicologoDTO(
            usuario.getId(),
            usuario.getNome(),
            usuario.getEmail(),
            usuario.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toList())
        );

        return ResponseEntity.ok(psicologoDTO);
    }
}
