package cuidar.mais.api.controller;

import cuidar.mais.api.dto.PacienteDTO;
import cuidar.mais.api.service.PacienteService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/pacientes")
@CrossOrigin(origins = {"http://localhost:4200", "http://localhost:3000"})
public class PacienteController {

    @Autowired
    private PacienteService pacienteService;

    /**
     * Busca todos os pacientes de um psicólogo
     */
    @GetMapping("/psicologo/{psicologoId}")
    public ResponseEntity<List<PacienteDTO>> buscarPorPsicologo(@PathVariable Long psicologoId) {
        try {
            List<PacienteDTO> pacientes = pacienteService.buscarPorPsicologo(psicologoId);
            return ResponseEntity.ok(pacientes);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Busca todos os pacientes (incluindo inativos) de um psicólogo
     */
    @GetMapping("/psicologo/{psicologoId}/todos")
    public ResponseEntity<List<PacienteDTO>> buscarTodosPorPsicologo(@PathVariable Long psicologoId) {
        try {
            List<PacienteDTO> pacientes = pacienteService.buscarTodosPorPsicologo(psicologoId);
            return ResponseEntity.ok(pacientes);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Busca um paciente por ID
     */
    @GetMapping("/{id}")
    public ResponseEntity<PacienteDTO> buscarPorId(@PathVariable Long id) {
        try {
            PacienteDTO paciente = pacienteService.buscarPorId(id);
            return ResponseEntity.ok(paciente);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * Cadastra um novo paciente
     */
    @PostMapping
    public ResponseEntity<PacienteDTO> cadastrar(@RequestBody PacienteDTO pacienteDTO) {
        try {
            PacienteDTO novoPaciente = pacienteService.cadastrar(pacienteDTO);
            return ResponseEntity.ok(novoPaciente);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Atualiza um paciente existente
     */
    @PutMapping("/{id}")
    public ResponseEntity<PacienteDTO> atualizar(@PathVariable Long id, @RequestBody PacienteDTO pacienteDTO) {
        try {
            PacienteDTO pacienteAtualizado = pacienteService.atualizar(id, pacienteDTO);
            return ResponseEntity.ok(pacienteAtualizado);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Inativa um paciente
     */
    @PostMapping("/{id}/inativar")
    public ResponseEntity<Void> inativar(@PathVariable Long id) {
        try {
            pacienteService.inativar(id);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Reativa um paciente
     */
    @PostMapping("/{id}/reativar")
    public ResponseEntity<Void> reativar(@PathVariable Long id) {
        try {
            pacienteService.reativar(id);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Cria sessões adicionais para um paciente
     */
    @PostMapping("/{id}/criar-sessoes-adicionais")
    public ResponseEntity<Void> criarSessoesAdicionais(@PathVariable Long id) {
        try {
            pacienteService.criarSessoesAdicionais(id);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Adiciona sessões extras ao pacote do paciente
     */
    @PostMapping("/{id}/adicionar-sessoes")
    public ResponseEntity<?> adicionarSessoes(@PathVariable Long id, @RequestBody Map<String, Integer> dados) {
        try {
            Integer quantidadeSessoes = dados.get("quantidadeSessoes");
            if (quantidadeSessoes == null || quantidadeSessoes <= 0) {
                return ResponseEntity.badRequest().body(Map.of("erro", "Quantidade de sessões deve ser maior que zero"));
            }
            
            pacienteService.adicionarSessoesExtras(id, quantidadeSessoes);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.badRequest().body(Map.of("erro", e.getMessage()));
        }
    }
}