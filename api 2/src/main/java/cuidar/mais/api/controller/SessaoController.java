package cuidar.mais.api.controller;

import cuidar.mais.api.dto.SessaoDTO;
import cuidar.mais.api.service.SessaoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/sessoes")
@CrossOrigin(origins = {"http://localhost:4200", "http://localhost:4201", "http://localhost:3000"})
public class SessaoController {

    @Autowired
    private SessaoService sessaoService;

    /**
     * Lista todas as sessões de um psicólogo
     */
    @GetMapping("/psicologo/{psicologoId}")
    public ResponseEntity<List<SessaoDTO>> listarPorPsicologo(@PathVariable Long psicologoId) {
        List<SessaoDTO> sessoes = sessaoService.listarSessoesPorPsicologo(psicologoId);
        return ResponseEntity.ok(sessoes);
    }

    /**
     * Lista sessões de um psicólogo em um período específico
     */
    @GetMapping("/psicologo/{psicologoId}/periodo")
    public ResponseEntity<List<SessaoDTO>> listarPorPsicologoEPeriodo(
            @PathVariable Long psicologoId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dataInicio,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dataFim) {
        
        List<SessaoDTO> sessoes = sessaoService.listarSessoesPorPsicologoEPeriodo(psicologoId, dataInicio, dataFim);
        return ResponseEntity.ok(sessoes);
    }

    /**
     * Lista sessões pendentes de um psicólogo
     */
    @GetMapping("/psicologo/{psicologoId}/pendentes")
    public ResponseEntity<List<SessaoDTO>> listarPendentesPorPsicologo(@PathVariable Long psicologoId) {
        List<SessaoDTO> sessoes = sessaoService.listarSessoesPendentes(psicologoId);
        return ResponseEntity.ok(sessoes);
    }

    /**
     * Lista sessões realizadas de um psicólogo
     */
    @GetMapping("/psicologo/{psicologoId}/realizadas")
    public ResponseEntity<List<SessaoDTO>> listarRealizadasPorPsicologo(@PathVariable Long psicologoId) {
        List<SessaoDTO> sessoes = sessaoService.listarSessoesRealizadas(psicologoId);
        return ResponseEntity.ok(sessoes);
    }

    /**
     * Lista todas as sessões de um paciente
     */
    @GetMapping("/paciente/{pacienteId}")
    public ResponseEntity<List<SessaoDTO>> listarPorPaciente(@PathVariable Long pacienteId) {
        List<SessaoDTO> sessoes = sessaoService.listarSessoesPorPaciente(pacienteId);
        return ResponseEntity.ok(sessoes);
    }

    /**
     * Busca uma sessão específica pelo ID
     */
    @GetMapping("/{id}")
    public ResponseEntity<SessaoDTO> buscarPorId(@PathVariable Long id) {
        try {
            SessaoDTO sessao = sessaoService.buscarSessaoPorId(id);
            return ResponseEntity.ok(sessao);
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * Marca uma sessão como realizada
     */
    @PostMapping("/{id}/realizar")
    public ResponseEntity<?> marcarComoRealizada(@PathVariable Long id, @RequestBody(required = false) Map<String, String> dados) {
        try {
            String observacoes = dados != null ? dados.get("observacoes") : null;
            SessaoDTO sessao = sessaoService.marcarSessaoComoRealizada(id, observacoes);
            return ResponseEntity.ok(sessao);
        } catch (RuntimeException e) {
            Map<String, String> erro = new HashMap<>();
            erro.put("erro", e.getMessage());
            return ResponseEntity.badRequest().body(erro);
        }
    }

    /**
     * Remarca uma sessão para nova data/hora
     */
    @PostMapping("/{id}/remarcar")
    public ResponseEntity<?> remarcar(@PathVariable Long id, @RequestBody Map<String, String> dados) {
        try {
            LocalDate novaData = LocalDate.parse(dados.get("novaData"));
            SessaoDTO sessao = sessaoService.remarcarSessao(id, novaData);
            return ResponseEntity.ok(sessao);
        } catch (RuntimeException e) {
            Map<String, String> erro = new HashMap<>();
            erro.put("erro", e.getMessage());
            return ResponseEntity.badRequest().body(erro);
        } catch (Exception e) {
            Map<String, String> erro = new HashMap<>();
            erro.put("erro", "Erro ao processar data: " + e.getMessage());
            return ResponseEntity.badRequest().body(erro);
        }
    }

    /**
     * Cancela uma sessão
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<?> cancelar(@PathVariable Long id) {
        try {
            sessaoService.cancelarSessao(id);
            Map<String, String> resposta = new HashMap<>();
            resposta.put("mensagem", "Sessão cancelada com sucesso");
            return ResponseEntity.ok(resposta);
        } catch (RuntimeException e) {
            Map<String, String> erro = new HashMap<>();
            erro.put("erro", e.getMessage());
            return ResponseEntity.badRequest().body(erro);
        }
    }

    /**
     * Atualiza observações de uma sessão
     */
    @PutMapping("/{id}/observacoes")
    public ResponseEntity<?> atualizarObservacoes(@PathVariable Long id, @RequestBody Map<String, String> dados) {
        try {
            String observacoes = dados.get("observacoes");
            SessaoDTO sessao = sessaoService.atualizarObservacoes(id, observacoes);
            return ResponseEntity.ok(sessao);
        } catch (RuntimeException e) {
            Map<String, String> erro = new HashMap<>();
            erro.put("erro", e.getMessage());
            return ResponseEntity.badRequest().body(erro);
        }
    }

    /**
     * Obtém contadores de sessões de um paciente
     */
    @GetMapping("/paciente/{pacienteId}/contadores")
    public ResponseEntity<Map<String, Long>> obterContadores(@PathVariable Long pacienteId) {
        long pendentes = sessaoService.contarSessoesPendentes(pacienteId);
        long realizadas = sessaoService.contarSessoesRealizadas(pacienteId);
        
        Map<String, Long> contadores = new HashMap<>();
        contadores.put("pendentes", pendentes);
        contadores.put("realizadas", realizadas);
        contadores.put("total", pendentes + realizadas);
        
        return ResponseEntity.ok(contadores);
    }
}
