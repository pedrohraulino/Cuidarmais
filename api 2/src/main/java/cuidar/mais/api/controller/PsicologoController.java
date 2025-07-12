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
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/psicologo")
@CrossOrigin(origins = {"http://localhost:4200", "http://localhost:3000"})
public class PsicologoController {
    @Autowired
    private UsuarioService usuarioService;

    @Autowired
    private ImagemBase64Service imagemService;

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

    @PostMapping("/{id}/imagem")
    public ResponseEntity<?> uploadImagem(
            @PathVariable Long id,
            @RequestParam("imagem") MultipartFile arquivo) {

        try {
            String base64String = imagemService.converterParaBase64(arquivo);

            Usuario usuario = usuarioService.buscarPorId(id);
            usuario.setImagemBase64(base64String);
            usuario.setImagemTipo(arquivo.getContentType());
            usuarioService.salvar(usuario);

            return ResponseEntity.ok().body(Map.of(
                    "message", "Imagem salva com sucesso",
                    "dataUrl", usuario.getImagemDataUrl()
            ));

        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Erro ao processar imagem: " + e.getMessage());
        }
    }

    @PostMapping("/{id}/imagem-base64")
    public ResponseEntity<?> uploadImagemBase64(
            @PathVariable Long id,
            @RequestBody Map<String, String> dados,
            @AuthenticationPrincipal Usuario usuarioLogado) {

        if (!usuarioLogado.getId().equals(id)) {
            return ResponseEntity.status(403).body("Acesso negado");
        }

        try {
            String dataUrl = dados.get("imagem");

            if (dataUrl == null || dataUrl.isEmpty()) {
                return ResponseEntity.badRequest().body("Imagem não pode estar vazia");
            }

            String tipoMime = imagemService.extrairTipoMime(dataUrl);
            if (tipoMime == null) {
                return ResponseEntity.badRequest().body("Formato de imagem inválido");
            }

            String base64String = imagemService.processarBase64String(dataUrl);

            Usuario usuario = usuarioService.buscarPorId(id);
            usuario.setImagemBase64(base64String);
            usuario.setImagemTipo(tipoMime);
            usuarioService.salvar(usuario);

            return ResponseEntity.ok().body(Map.of(
                    "message", "Imagem salva com sucesso",
                    "dataUrl", usuario.getImagemDataUrl()
            ));

        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Erro ao processar imagem: " + e.getMessage());
        }
    }

    @GetMapping("/{id}/imagem")
    public ResponseEntity<?> obterImagem(@PathVariable Long id, @AuthenticationPrincipal Usuario usuarioLogado) {
        if (!usuarioLogado.getId().equals(id)) {
            return ResponseEntity.status(403).body("Acesso negado");
        }
        try {
            Usuario usuario = usuarioService.buscarPorId(id);

            if (usuario.getImagemBase64() == null) {
                return ResponseEntity.notFound().build();
            }

            return ResponseEntity.ok().body(Map.of(
                    "dataUrl", usuario.getImagemDataUrl(),
                    "tipo", usuario.getImagemTipo()
            ));

        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Erro ao obter imagem");
        }
    }
}
