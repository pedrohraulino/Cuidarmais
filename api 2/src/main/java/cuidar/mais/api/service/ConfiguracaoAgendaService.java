package cuidar.mais.api.service;

import cuidar.mais.api.dto.ConfiguracaoAgendaDTO;
import cuidar.mais.api.models.ConfiguracaoAgenda;
import cuidar.mais.api.models.Usuario;
import cuidar.mais.api.repository.ConfiguracaoAgendaRepository;
import cuidar.mais.api.service.UsuarioService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class ConfiguracaoAgendaService {

    @Autowired
    private ConfiguracaoAgendaRepository configuracaoAgendaRepository;

    @Autowired
    private UsuarioService usuarioService;

    @Autowired
    private AgendamentoService agendamentoService;

    /**
     * Busca todas as configurações de agenda de um psicólogo
     */
    public List<ConfiguracaoAgendaDTO> buscarPorPsicologo(Long psicologoId) {
        Usuario psicologo = usuarioService.buscarPorId(psicologoId);

        return configuracaoAgendaRepository.findByPsicologo(psicologo).stream()
                .map(this::converterParaDTO)
                .collect(Collectors.toList());
    }

    /**
     * Busca uma configuração específica de agenda
     */
    public ConfiguracaoAgendaDTO buscarPorId(Long id) {
        ConfiguracaoAgenda configuracao = configuracaoAgendaRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Configuração de agenda não encontrada"));

        return converterParaDTO(configuracao);
    }

    /**
     * Verifica se todos os horários de uma configuração estão disponíveis
     * @param psicologoId ID do psicólogo
     * @param diaSemana Dia da semana
     * @param data Data para verificar disponibilidade
     * @param horarioInicio Hora de início
     * @param horarioFim Hora de fim
     * @param inicioPausa Hora de início da pausa
     * @param fimPausa Hora de fim da pausa
     * @param intervaloMinutos Intervalo entre atendimentos
     * @return true se todos os horários estiverem disponíveis, false caso contrário
     */
    public boolean verificarDisponibilidadeHorarios(
            Long psicologoId, 
            DayOfWeek diaSemana, 
            LocalDate data, 
            LocalTime horarioInicio, 
            LocalTime horarioFim, 
            LocalTime inicioPausa, 
            LocalTime fimPausa, 
            Integer intervaloMinutos) {

        // Duração de cada atendimento: 50 minutos
        int duracaoAtendimento = 50;

        // Horário atual para iteração
        LocalTime horarioAtual = horarioInicio;

        // Verifica todos os horários até o fim do expediente
        while (horarioAtual.plusMinutes(duracaoAtendimento).isBefore(horarioFim) ||
               horarioAtual.plusMinutes(duracaoAtendimento).equals(horarioFim)) {

            // Verifica se o horário está dentro do período de pausa
            boolean estaNaPausa = false;
            if (inicioPausa != null && fimPausa != null) {
                estaNaPausa = !horarioAtual.isBefore(inicioPausa) &&
                              horarioAtual.isBefore(fimPausa);
            }

            // Verifica disponibilidade se não estiver na pausa
            if (!estaNaPausa) {
                LocalTime horarioFimAtendimento = horarioAtual.plusMinutes(duracaoAtendimento);

                // Verifica se o horário está disponível
                if (data != null && !agendamentoService.isHorarioDisponivel(
                        psicologoId, data, horarioAtual, horarioFimAtendimento)) {
                    return false; // Encontrou um horário indisponível
                }
            }

            // Avança para o próximo horário considerando o intervalo
            horarioAtual = horarioAtual.plusMinutes(duracaoAtendimento + intervaloMinutos);
        }

        return true; // Todos os horários estão disponíveis
    }

    /**
     * Salva uma nova configuração de agenda ou atualiza uma existente
     */
    @Transactional
    public ConfiguracaoAgendaDTO salvar(ConfiguracaoAgendaDTO dto) {
        Usuario psicologo = usuarioService.buscarPorId(dto.getPsicologoId());

        // Verifica se já existe configuração para este dia
        Optional<ConfiguracaoAgenda> configuracaoExistente =
                configuracaoAgendaRepository.findByPsicologoAndDiaSemana(psicologo, dto.getDiaSemana());

        ConfiguracaoAgenda configuracao;
        boolean isUpdate = false;

        if (configuracaoExistente.isPresent() && (dto.getId() == null || !dto.getId().equals(configuracaoExistente.get().getId()))) {
            // Atualiza a configuração existente
            configuracao = configuracaoExistente.get();
            isUpdate = true;
        } else if (dto.getId() != null) {
            // Busca a configuração pelo ID para atualizar
            configuracao = configuracaoAgendaRepository.findById(dto.getId())
                    .orElseThrow(() -> new RuntimeException("Configuração de agenda não encontrada"));
            isUpdate = true;
        } else {
            // Cria uma nova configuração
            configuracao = new ConfiguracaoAgenda();
            configuracao.setPsicologo(psicologo);
            configuracao.setDiaSemana(dto.getDiaSemana());
        }

        // Se for uma atualização, verifica se todos os horários estão disponíveis
        if (isUpdate) {
            // Obtém a data atual para o dia da semana da configuração
            LocalDate dataAtual = LocalDate.now();
            while (dataAtual.getDayOfWeek() != dto.getDiaSemana()) {
                dataAtual = dataAtual.plusDays(1);
            }

            // Verifica se todos os horários estão disponíveis
            boolean todosHorariosDisponiveis = verificarDisponibilidadeHorarios(
                    dto.getPsicologoId(),
                    dto.getDiaSemana(),
                    dataAtual,
                    dto.getHorarioInicio(),
                    dto.getHorarioFim(),
                    dto.getInicioPausa(),
                    dto.getFimPausa(),
                    dto.getIntervaloMinutos()
            );

            if (!todosHorariosDisponiveis) {
                throw new RuntimeException("Não é possível atualizar a configuração porque existem horários indisponíveis");
            }
        }

        // Atualiza os campos
        configuracao.setAtivo(dto.getAtivo());
        configuracao.setHorarioInicio(dto.getHorarioInicio());
        configuracao.setIntervaloMinutos(dto.getIntervaloMinutos());
        configuracao.setHorarioFim(dto.getHorarioFim());
        configuracao.setInicioPausa(dto.getInicioPausa());
        configuracao.setFimPausa(dto.getFimPausa());

        // Define a data de atualização como a data e hora atual
        configuracao.setDataAtualizacao(LocalDateTime.now());

        // Salva a configuração
        configuracao = configuracaoAgendaRepository.save(configuracao);

        return converterParaDTO(configuracao);
    }

    /**
     * Exclui uma configuração de agenda
     */
    @Transactional
    public void excluir(Long id) {
        configuracaoAgendaRepository.deleteById(id);
    }

    /**
     * Gera os horários disponíveis para um psicólogo em um dia específico
     * @deprecated Use {@link #gerarHorariosDisponiveisComDisponibilidade(Long, DayOfWeek, LocalDate)} instead
     */
    @Deprecated
    public List<LocalTime> gerarHorariosDisponiveis(Long psicologoId, DayOfWeek diaSemana) {
        Usuario psicologo = usuarioService.buscarPorId(psicologoId);

        Optional<ConfiguracaoAgenda> configuracaoOpt =
                configuracaoAgendaRepository.findByPsicologoAndDiaSemana(psicologo, diaSemana);

        if (configuracaoOpt.isEmpty() || !configuracaoOpt.get().getAtivo()) {
            return new ArrayList<>(); // Não há configuração ou está inativa
        }

        ConfiguracaoAgenda config = configuracaoOpt.get();
        List<LocalTime> horarios = new ArrayList<>();

        // Duração de cada atendimento: 50 minutos
        int duracaoAtendimento = 50;

        // Horário atual para iteração
        LocalTime horarioAtual = config.getHorarioInicio();

        // Gera os horários até o fim do expediente
        while (horarioAtual.plusMinutes(duracaoAtendimento).isBefore(config.getHorarioFim()) ||
               horarioAtual.plusMinutes(duracaoAtendimento).equals(config.getHorarioFim())) {

            // Verifica se o horário está dentro do período de pausa
            boolean estaNaPausa = false;
            if (config.getInicioPausa() != null && config.getFimPausa() != null) {
                estaNaPausa = !horarioAtual.isBefore(config.getInicioPausa()) &&
                              horarioAtual.isBefore(config.getFimPausa());
            }

            // Adiciona o horário se não estiver na pausa
            if (!estaNaPausa) {
                horarios.add(horarioAtual);
            }

            // Avança para o próximo horário considerando o intervalo
            horarioAtual = horarioAtual.plusMinutes(duracaoAtendimento + config.getIntervaloMinutos());
        }

        return horarios;
    }

    /**
     * Gera os horários disponíveis para um psicólogo em um dia específico, incluindo informações de disponibilidade
     * @param psicologoId ID do psicólogo
     * @param diaSemana Dia da semana
     * @param data Data para verificar disponibilidade (se null, apenas gera os horários sem verificar disponibilidade)
     * @return Lista de mapas com informações de cada horário
     */
    public List<Map<String, Object>> gerarHorariosDisponiveisComDisponibilidade(Long psicologoId, DayOfWeek diaSemana, LocalDate data) {
        Usuario psicologo = usuarioService.buscarPorId(psicologoId);

        Optional<ConfiguracaoAgenda> configuracaoOpt =
                configuracaoAgendaRepository.findByPsicologoAndDiaSemana(psicologo, diaSemana);

        if (configuracaoOpt.isEmpty() || !configuracaoOpt.get().getAtivo()) {
            return new ArrayList<>(); // Não há configuração ou está inativa
        }

        ConfiguracaoAgenda config = configuracaoOpt.get();
        List<Map<String, Object>> horariosInfo = new ArrayList<>();

        // Duração de cada atendimento: 50 minutos
        int duracaoAtendimento = 50;

        // Horário atual para iteração
        LocalTime horarioAtual = config.getHorarioInicio();

        // Gera os horários até o fim do expediente
        while (horarioAtual.plusMinutes(duracaoAtendimento).isBefore(config.getHorarioFim()) ||
               horarioAtual.plusMinutes(duracaoAtendimento).equals(config.getHorarioFim())) {

            // Verifica se o horário está dentro do período de pausa
            boolean estaNaPausa = false;
            if (config.getInicioPausa() != null && config.getFimPausa() != null) {
                estaNaPausa = !horarioAtual.isBefore(config.getInicioPausa()) &&
                              horarioAtual.isBefore(config.getFimPausa());
            }

            // Adiciona o horário se não estiver na pausa
            if (!estaNaPausa) {
                LocalTime horarioFim = horarioAtual.plusMinutes(duracaoAtendimento);

                Map<String, Object> horarioInfo = new HashMap<>();
                horarioInfo.put("inicio", horarioAtual);
                horarioInfo.put("fim", horarioFim);

                // Verifica disponibilidade se a data for fornecida
                if (data != null) {
                    System.out.println("Service: Verificando disponibilidade para " + data + " " + horarioAtual + "-" + horarioFim);
                    boolean disponivel = agendamentoService.isHorarioDisponivel(
                            psicologoId, data, horarioAtual, horarioFim);
                    System.out.println("Service: Resultado da verificação: " + disponivel);
                    horarioInfo.put("disponivel", disponivel);
                } else {
                    System.out.println("Service: Data não fornecida, assumindo disponível para " + horarioAtual + "-" + horarioFim);
                    horarioInfo.put("disponivel", true); // Assume disponível se não verificar
                }

                horariosInfo.add(horarioInfo);
            }

            // Avança para o próximo horário considerando o intervalo
            horarioAtual = horarioAtual.plusMinutes(duracaoAtendimento + config.getIntervaloMinutos());
        }

        return horariosInfo;
    }

    /**
     * Converte uma entidade ConfiguracaoAgenda para DTO
     */
    private ConfiguracaoAgendaDTO converterParaDTO(ConfiguracaoAgenda configuracao) {
        ConfiguracaoAgendaDTO dto = new ConfiguracaoAgendaDTO();
        dto.setId(configuracao.getId());
        dto.setPsicologoId(configuracao.getPsicologo().getId());
        dto.setDiaSemana(configuracao.getDiaSemana());
        dto.setAtivo(configuracao.getAtivo());
        dto.setHorarioInicio(configuracao.getHorarioInicio());
        dto.setIntervaloMinutos(configuracao.getIntervaloMinutos());
        dto.setHorarioFim(configuracao.getHorarioFim());
        dto.setInicioPausa(configuracao.getInicioPausa());
        dto.setFimPausa(configuracao.getFimPausa());
        dto.setDataAtualizacao(configuracao.getDataAtualizacao());
        return dto;
    }
}
