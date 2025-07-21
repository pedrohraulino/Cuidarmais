package cuidar.mais.api.controller;

import cuidar.mais.api.dto.PsicologoDTO;
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

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/psicologo")
@CrossOrigin(origins = {"http://localhost:4200", "http://localhost:4201", "http://localhost:3000"})
public class PsicologoController {
    private static final Logger logger = LoggerFactory.getLogger(PsicologoController.class);

    @Autowired
    private UsuarioService usuarioService;

    @Autowired
    private ImagemBase64Service imagemService;

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
            usuario.getCrp(),
            usuario.getImagemDataUrl(),
            usuario.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toList())
        );

        return ResponseEntity.ok(psicologoDTO);
    }

    @PostMapping("/{id}/imagem-base64")
    public ResponseEntity<?> uploadImagemBase64(@PathVariable Long id, @RequestBody Map<String, String> payload) {
        try {
            String imagemBase64 = payload.get("imagem");
            if (imagemBase64 == null || imagemBase64.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("erro", "Imagem não fornecida"));
            }

            // Extrair o tipo MIME e a string base64 da data URL
            String tipoMime = imagemService.extrairTipoMime(imagemBase64);
            String base64 = imagemService.processarBase64String(imagemBase64);

            // Buscar o usuário
            Usuario usuario = usuarioService.buscarPorId(id);

            // Atualizar a imagem do usuário
            usuario.setImagemBase64(base64);
            usuario.setImagemTipo(tipoMime);

            // Salvar o usuário
            usuarioService.salvar(usuario);

            return ResponseEntity.ok(Map.of("mensagem", "Imagem atualizada com sucesso"));
        } catch (Exception e) {
            logger.error("Erro ao fazer upload da imagem: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("erro", "Erro ao processar imagem: " + e.getMessage()));
        }
    }

    @GetMapping("/{id}/imagem")
    public ResponseEntity<?> obterImagem(@PathVariable Long id) {
        try {
            // Buscar o usuário
            Usuario usuario = usuarioService.buscarPorId(id);

            // Verificar se o usuário tem imagem
            if (usuario.getImagemBase64() == null || usuario.getImagemTipo() == null) {
                return ResponseEntity.ok(Map.of("dataUrl", null));
            }

            // Retornar a imagem como data URL
            String dataUrl = usuario.getImagemDataUrl();
            return ResponseEntity.ok(Map.of("dataUrl", dataUrl));
        } catch (Exception e) {
            logger.error("Erro ao obter imagem: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("erro", "Erro ao obter imagem: " + e.getMessage()));
        }
    }
}
