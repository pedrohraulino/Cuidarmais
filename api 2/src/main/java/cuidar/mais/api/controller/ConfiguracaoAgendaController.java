package cuidar.mais.api.controller;

import cuidar.mais.api.dto.ConfiguracaoAgendaDTO;
import cuidar.mais.api.models.Usuario;
import cuidar.mais.api.service.ConfiguracaoAgendaService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/configuracao-agenda")
@CrossOrigin(origins = {"http://localhost:4200", "http://localhost:4201", "http://localhost:3000"})
public class ConfiguracaoAgendaController {

    private static final Logger logger = LoggerFactory.getLogger(ConfiguracaoAgendaController.class);

    @Autowired
    private ConfiguracaoAgendaService configuracaoAgendaService;

    /**
     * Busca todas as configurações de agenda do psicólogo logado
     */
    @GetMapping
    public ResponseEntity<?> listarConfiguracoes(@AuthenticationPrincipal Usuario usuario) {
        if (usuario == null) {
            logger.warn("Tentativa de acesso sem autenticação");
            return ResponseEntity.status(401).body(Map.of("erro", "Usuário não autenticado"));
        }

        logger.info("Listando configurações de agenda para o psicólogo {}", usuario.getId());
        List<ConfiguracaoAgendaDTO> configuracoes = configuracaoAgendaService.buscarPorPsicologo(usuario.getId());
        return ResponseEntity.ok(configuracoes);
    }

    /**
     * Busca uma configuração específica de agenda
     */
    @GetMapping("/{id}")
    public ResponseEntity<?> buscarConfiguracao(@PathVariable Long id, @AuthenticationPrincipal Usuario usuario) {
        if (usuario == null) {
            logger.warn("Tentativa de acesso sem autenticação");
            return ResponseEntity.status(401).body(Map.of("erro", "Usuário não autenticado"));
        }

        try {
            logger.info("Buscando configuração de agenda {}", id);
            ConfiguracaoAgendaDTO configuracao = configuracaoAgendaService.buscarPorId(id);

            // Verifica se a configuração pertence ao psicólogo logado
            if (!configuracao.getPsicologoId().equals(usuario.getId())) {
                logger.warn("Tentativa de acesso a configuração de outro psicólogo");
                return ResponseEntity.status(403).body(Map.of("erro", "Acesso negado"));
            }

            return ResponseEntity.ok(configuracao);
        } catch (RuntimeException e) {
            logger.error("Erro ao buscar configuração: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("erro", e.getMessage()));
        }
    }

    /**
     * Salva uma nova configuração de agenda ou atualiza uma existente
     */
    @PostMapping
    public ResponseEntity<?> salvarConfiguracao(@RequestBody ConfiguracaoAgendaDTO dto, 
                                               @AuthenticationPrincipal Usuario usuario) {
        if (usuario == null) {
            logger.warn("Tentativa de acesso sem autenticação");
            return ResponseEntity.status(401).body(Map.of("erro", "Usuário não autenticado"));
        }

        // Garante que a configuração seja salva para o psicólogo logado
        dto.setPsicologoId(usuario.getId());

        try {
            logger.info("Salvando configuração de agenda para o dia {}", dto.getDiaSemana());
            ConfiguracaoAgendaDTO configuracaoSalva = configuracaoAgendaService.salvar(dto);
            return ResponseEntity.ok(configuracaoSalva);
        } catch (RuntimeException e) {
            logger.error("Erro ao salvar configuração: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("erro", e.getMessage()));
        }
    }

    /**
     * Atualiza uma configuração de agenda existente
     */
    @PutMapping("/{id}")
    public ResponseEntity<?> atualizarConfiguracao(@PathVariable Long id, 
                                                 @RequestBody ConfiguracaoAgendaDTO dto,
                                                 @AuthenticationPrincipal Usuario usuario) {
        if (usuario == null) {
            logger.warn("Tentativa de acesso sem autenticação");
            return ResponseEntity.status(401).body(Map.of("erro", "Usuário não autenticado"));
        }

        try {
            // Verifica se a configuração pertence ao psicólogo logado
            ConfiguracaoAgendaDTO configuracaoExistente = configuracaoAgendaService.buscarPorId(id);
            if (!configuracaoExistente.getPsicologoId().equals(usuario.getId())) {
                logger.warn("Tentativa de atualizar configuração de outro psicólogo");
                return ResponseEntity.status(403).body(Map.of("erro", "Acesso negado"));
            }

            // Garante que o ID na URL e no corpo da requisição sejam os mesmos
            dto.setId(id);

            // Garante que a configuração seja atualizada para o psicólogo logado
            dto.setPsicologoId(usuario.getId());

            logger.info("Atualizando configuração de agenda {} para o dia {}", id, dto.getDiaSemana());
            ConfiguracaoAgendaDTO configuracaoAtualizada = configuracaoAgendaService.salvar(dto);
            return ResponseEntity.ok(configuracaoAtualizada);
        } catch (RuntimeException e) {
            logger.error("Erro ao atualizar configuração: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("erro", e.getMessage()));
        }
    }

    /**
     * Verifica se uma configuração de agenda pode ser excluída
     */
    @GetMapping("/{id}/pode-excluir")
    public ResponseEntity<?> podeExcluirConfiguracao(@PathVariable Long id, @AuthenticationPrincipal Usuario usuario) {
        if (usuario == null) {
            logger.warn("Tentativa de acesso sem autenticação");
            return ResponseEntity.status(401).body(Map.of("erro", "Usuário não autenticado"));
        }

        try {
            // Verifica se a configuração pertence ao psicólogo logado
            ConfiguracaoAgendaDTO configuracao = configuracaoAgendaService.buscarPorId(id);
            if (!configuracao.getPsicologoId().equals(usuario.getId())) {
                logger.warn("Tentativa de verificar configuração de outro psicólogo");
                return ResponseEntity.status(403).body(Map.of("erro", "Acesso negado"));
            }

            logger.info("Verificando se configuração de agenda {} pode ser excluída", id);
            boolean podeExcluir = configuracaoAgendaService.podeExcluir(id);
            return ResponseEntity.ok(Map.of("podeExcluir", podeExcluir));
        } catch (RuntimeException e) {
            logger.error("Erro ao verificar se configuração pode ser excluída: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("erro", e.getMessage()));
        }
    }

    /**
     * Exclui uma configuração de agenda
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<?> excluirConfiguracao(@PathVariable Long id, @AuthenticationPrincipal Usuario usuario) {
        if (usuario == null) {
            logger.warn("Tentativa de acesso sem autenticação");
            return ResponseEntity.status(401).body(Map.of("erro", "Usuário não autenticado"));
        }

        try {
            // Verifica se a configuração pertence ao psicólogo logado
            ConfiguracaoAgendaDTO configuracao = configuracaoAgendaService.buscarPorId(id);
            if (!configuracao.getPsicologoId().equals(usuario.getId())) {
                logger.warn("Tentativa de excluir configuração de outro psicólogo");
                return ResponseEntity.status(403).body(Map.of("erro", "Acesso negado"));
            }

            logger.info("Excluindo configuração de agenda {}", id);
            configuracaoAgendaService.excluir(id);
            return ResponseEntity.ok(Map.of("mensagem", "Configuração excluída com sucesso"));
        } catch (RuntimeException e) {
            logger.error("Erro ao excluir configuração: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("erro", e.getMessage()));
        }
    }

    /**
     * Gera os horários disponíveis para um dia específico
     */
    @GetMapping("/horarios-disponiveis")
    public ResponseEntity<?> gerarHorariosDisponiveis(
            @RequestParam("diaSemana") DayOfWeek diaSemana,
            @RequestParam(value = "data", required = false) LocalDate data,
            @AuthenticationPrincipal Usuario usuario) {

        if (usuario == null) {
            logger.warn("Tentativa de acesso sem autenticação");
            return ResponseEntity.status(401).body(Map.of("erro", "Usuário não autenticado"));
        }

        try {
            logger.info("Gerando horários disponíveis para o dia {} e data {}", diaSemana, data);
            System.out.println("Controller: Gerando horários disponíveis para o dia " + diaSemana + " e data " + data);

            List<Map<String, Object>> horariosInfo = configuracaoAgendaService.gerarHorariosDisponiveisComDisponibilidade(
                    usuario.getId(), diaSemana, data);

            System.out.println("Controller: Recebidos " + horariosInfo.size() + " horários do serviço");

            // Formata os horários para exibição
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm");
            List<Map<String, Object>> horariosFormatados = new ArrayList<>();

            for (Map<String, Object> horarioInfo : horariosInfo) {
                LocalTime horarioInicio = (LocalTime) horarioInfo.get("inicio");
                LocalTime horarioFim = (LocalTime) horarioInfo.get("fim");
                Boolean disponivel = (Boolean) horarioInfo.get("disponivel");

                System.out.println("Controller: Processando horário " + horarioInicio + "-" + horarioFim + ", disponível: " + disponivel);

                Map<String, Object> slot = new HashMap<>();
                slot.put("inicio", horarioInicio.format(formatter));
                slot.put("fim", horarioFim.format(formatter));
                slot.put("inicioFormatado", horarioInicio.format(formatter));
                slot.put("fimFormatado", horarioFim.format(formatter));
                slot.put("disponivel", disponivel);
                horariosFormatados.add(slot);
            }

            return ResponseEntity.ok(horariosFormatados);
        } catch (RuntimeException e) {
            logger.error("Erro ao gerar horários disponíveis: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("erro", e.getMessage()));
        }
    }

    /**
     * Busca todas as configurações de agenda de um psicólogo específico
     */
    @GetMapping("/psicologo/{psicologoId}")
    public ResponseEntity<?> buscarPorPsicologo(@PathVariable Long psicologoId) {
        try {
            logger.info("Buscando configurações de agenda para o psicólogo {}", psicologoId);
            List<ConfiguracaoAgendaDTO> configuracoes = configuracaoAgendaService.buscarPorPsicologo(psicologoId);
            return ResponseEntity.ok(configuracoes);
        } catch (RuntimeException e) {
            logger.error("Erro ao buscar configurações de agenda: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("erro", e.getMessage()));
        }
    }

    /**
     * Inicializa as configurações padrão para todos os dias da semana
     */
    @PostMapping("/inicializar")
    public ResponseEntity<?> inicializarConfiguracoes(@AuthenticationPrincipal Usuario usuario) {
        if (usuario == null) {
            logger.warn("Tentativa de acesso sem autenticação");
            return ResponseEntity.status(401).body(Map.of("erro", "Usuário não autenticado"));
        }

        try {
            logger.info("Inicializando configurações padrão para o psicólogo {}", usuario.getId());

            // Horários padrão
            LocalTime horarioInicio = LocalTime.of(8, 0);
            LocalTime horarioFim = LocalTime.of(18, 0);
            LocalTime inicioPausa = LocalTime.of(12, 0);
            LocalTime fimPausa = LocalTime.of(13, 0);
            Integer intervaloMinutos = 10;

            // Cria configuração para cada dia útil da semana
            for (DayOfWeek dia : new DayOfWeek[]{
                    DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY, 
                    DayOfWeek.THURSDAY, DayOfWeek.FRIDAY}) {

                ConfiguracaoAgendaDTO dto = new ConfiguracaoAgendaDTO();
                dto.setPsicologoId(usuario.getId());
                dto.setDiaSemana(dia);
                dto.setAtivo(true);
                dto.setHorarioInicio(horarioInicio);
                dto.setHorarioFim(horarioFim);
                dto.setInicioPausa(inicioPausa);
                dto.setVoltaPausa(fimPausa);
                dto.setIntervaloMinutos(intervaloMinutos);

                configuracaoAgendaService.salvar(dto);
            }

            return ResponseEntity.ok(Map.of("mensagem", "Configurações inicializadas com sucesso"));
        } catch (RuntimeException e) {
            logger.error("Erro ao inicializar configurações: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("erro", e.getMessage()));
        }
    }
}
