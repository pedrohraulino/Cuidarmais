package cuidar.mais.api.controller;

import cuidar.mais.api.dto.PacienteDTO;
import cuidar.mais.api.models.Agendamento;
import cuidar.mais.api.service.AgendamentoService;
import cuidar.mais.api.service.PacienteService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/pacientes")
public class PacienteController {

    @Autowired
    private PacienteService pacienteService;

    @Autowired
    private AgendamentoService agendamentoService;

    /**
     * Busca todos os pacientes de um psicólogo
     */
    @GetMapping("/psicologo/{psicologoId}")
    public ResponseEntity<List<PacienteDTO>> buscarPorPsicologo(@PathVariable Long psicologoId) {
        List<PacienteDTO> pacientes = pacienteService.buscarPorPsicologo(psicologoId);
        return ResponseEntity.ok(pacientes);
    }

    /**
     * Busca um paciente pelo ID
     */
    @GetMapping("/{id}")
    public ResponseEntity<PacienteDTO> buscarPorId(@PathVariable Long id) {
        PacienteDTO paciente = pacienteService.buscarPorId(id);
        return ResponseEntity.ok(paciente);
    }

    /**
     * Cria um novo paciente com agendamentos recorrentes
     */
    @PostMapping
    public ResponseEntity<?> criar(@RequestBody PacienteDTO pacienteDTO) {
        try {
            PacienteDTO pacienteCriado = pacienteService.criar(pacienteDTO);
            return ResponseEntity.ok(pacienteCriado);
        } catch (Exception e) {
            // Log the error
            System.err.println("Erro ao criar paciente: " + e.getMessage());
            e.printStackTrace();

            // Return a structured error response
            Map<String, String> erro = new HashMap<>();
            erro.put("erro", e.getMessage());
            return ResponseEntity.badRequest().body(erro);
        }
    }

    /**
     * Atualiza um paciente existente
     */
    @PutMapping("/{id}")
    public ResponseEntity<PacienteDTO> atualizar(@PathVariable Long id, @RequestBody PacienteDTO pacienteDTO) {
        PacienteDTO pacienteAtualizado = pacienteService.atualizar(id, pacienteDTO);
        return ResponseEntity.ok(pacienteAtualizado);
    }

    /**
     * Busca todos os agendamentos de um paciente
     */
    @GetMapping("/{id}/agendamentos")
    public ResponseEntity<List<Agendamento>> buscarAgendamentos(@PathVariable Long id) {
        List<Agendamento> agendamentos = agendamentoService.buscarPorPaciente(id);
        return ResponseEntity.ok(agendamentos);
    }

    /**
     * Busca todos os agendamentos agendados de um paciente
     */
    @GetMapping("/{id}/agendamentos/agendados")
    public ResponseEntity<List<Agendamento>> buscarAgendamentosAgendados(@PathVariable Long id) {
        List<Agendamento> agendamentos = agendamentoService.buscarAgendadosPorPaciente(id);
        return ResponseEntity.ok(agendamentos);
    }

    /**
     * Busca todos os agendamentos ativos e agendados de um paciente
     */
    @GetMapping("/{id}/agendamentos/ativos-agendados")
    public ResponseEntity<List<Agendamento>> buscarAgendamentosAtivosEAgendados(@PathVariable Long id) {
        try {
            System.out.println("Buscando agendamentos ativos e agendados para o paciente com ID: " + id);
            List<Agendamento> agendamentos = agendamentoService.buscarAtivosEAgendadosPorPaciente(id);
            System.out.println("Encontrados " + agendamentos.size() + " agendamentos ativos e agendados para o paciente com ID: " + id);
            return ResponseEntity.ok(agendamentos);
        } catch (Exception e) {
            System.err.println("Erro ao buscar agendamentos ativos e agendados para o paciente com ID: " + id);
            e.printStackTrace();
            throw e;
        }
    }

    /**
     * Endpoint de teste para verificar se a configuração de segurança está funcionando corretamente
     */
    @GetMapping("/{id}/agendamentos/teste-acesso")
    public ResponseEntity<String> testeAcesso(@PathVariable Long id) {
        return ResponseEntity.ok("Acesso permitido ao endpoint de teste para o paciente com ID: " + id);
    }

    /**
     * Busca o horário atual de um paciente
     * Este endpoint retorna o horário disponível associado ao agendamento mais recente do paciente
     */
    @GetMapping("/{id}/horario-atual")
    public ResponseEntity<?> buscarHorarioAtual(@PathVariable Long id) {
        try {
            System.out.println("Buscando horário atual para o paciente com ID: " + id);
            Map<String, Object> horarioAtual = pacienteService.buscarHorarioAtual(id);
            System.out.println("Horário atual encontrado para o paciente com ID: " + id + ": " + horarioAtual);
            return ResponseEntity.ok(horarioAtual);
        } catch (Exception e) {
            System.err.println("Erro ao buscar horário atual para o paciente com ID: " + id);
            e.printStackTrace();

            Map<String, String> erro = new HashMap<>();
            erro.put("erro", e.getMessage());
            return ResponseEntity.badRequest().body(erro);
        }
    }

    /**
     * Inativa um paciente e todos os seus agendamentos ativos
     * Também reativa o horário que estava vinculado aos agendamentos
     */
    @PostMapping("/{id}/inativar")
    public ResponseEntity<?> inativarPaciente(@PathVariable Long id) {
        try {
            System.out.println("Inativando paciente com ID: " + id);
            PacienteDTO paciente = pacienteService.inativarPaciente(id);
            System.out.println("Paciente inativado com sucesso: " + paciente.getId());
            return ResponseEntity.ok(paciente);
        } catch (Exception e) {
            System.err.println("Erro ao inativar paciente com ID: " + id);
            e.printStackTrace();

            Map<String, String> erro = new HashMap<>();
            erro.put("erro", e.getMessage());
            return ResponseEntity.badRequest().body(erro);
        }
    }

    /**
     * Cria sessões adicionais para um paciente
     * Este endpoint cria novas sessões baseadas no total de sessões configurado para o paciente
     */
    @PostMapping("/{id}/criar-sessoes-adicionais")
    public ResponseEntity<?> criarSessoesAdicionais(@PathVariable Long id) {
        try {
            System.out.println("Criando sessões adicionais para o paciente com ID: " + id);
            PacienteDTO paciente = pacienteService.criarSessoesAdicionais(id);
            System.out.println("Sessões adicionais criadas com sucesso para o paciente com ID: " + id);
            return ResponseEntity.ok(paciente);
        } catch (Exception e) {
            System.err.println("Erro ao criar sessões adicionais para o paciente com ID: " + id);
            e.printStackTrace();

            Map<String, String> erro = new HashMap<>();
            erro.put("erro", e.getMessage());
            return ResponseEntity.badRequest().body(erro);
        }
    }

    /**
     * Reativa um paciente que estava inativo
     */
    @PostMapping("/{id}/reativar")
    public ResponseEntity<?> reativarPaciente(@PathVariable Long id) {
        try {
            System.out.println("Reativando paciente com ID: " + id);
            PacienteDTO paciente = pacienteService.reativarPaciente(id);
            System.out.println("Paciente reativado com sucesso: " + paciente.getId());
            return ResponseEntity.ok(paciente);
        } catch (Exception e) {
            System.err.println("Erro ao reativar paciente com ID: " + id);
            e.printStackTrace();

            Map<String, String> erro = new HashMap<>();
            erro.put("erro", e.getMessage());
            return ResponseEntity.badRequest().body(erro);
        }
    }
}
