package cuidar.mais.api.service;

import cuidar.mais.api.dto.HorarioDisponivelDTO;
import cuidar.mais.api.models.HorarioDisponivel;
import cuidar.mais.api.models.Usuario;
import cuidar.mais.api.models.ConfiguracaoAgenda;
import cuidar.mais.api.repository.HorarioDisponivelRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class HorarioDisponivelService {

    @Autowired
    private HorarioDisponivelRepository horarioDisponivelRepository;

    @Autowired
    private UsuarioService usuarioService;

    /**
     * Busca horário por ID do paciente
     */
    public Optional<HorarioDisponivel> buscarHorarioPorPaciente(Long pacienteId) {
        return horarioDisponivelRepository.findByPacienteId(pacienteId);
    }

    /**
     * Desvincula paciente de todos os horários
     */
    @Transactional
    public void desvincularPaciente(Long pacienteId) {
        horarioDisponivelRepository.desvincularTodosHorariosDoPaciente(pacienteId);
    }

    /**
     * Busca todos os horários de um psicólogo
     */
    public List<HorarioDisponivelDTO> buscarPorPsicologo(Long psicologoId) {
        Usuario psicologo = usuarioService.buscarPorId(psicologoId);
        List<HorarioDisponivel> horarios = horarioDisponivelRepository.findByPsicologoAndAtivoTrue(psicologo);
        return horarios.stream()
                .map(this::converterParaDTO)
                .collect(Collectors.toList());
    }

    /**
     * Busca horários de um psicólogo em um dia específico
     */
    public List<HorarioDisponivelDTO> buscarPorPsicologoEDiaSemana(Long psicologoId, DayOfWeek diaSemana) {
        Usuario psicologo = usuarioService.buscarPorId(psicologoId);
        List<HorarioDisponivel> horarios = horarioDisponivelRepository
                .findByPsicologoAndDiaSemanaAndAtivoTrue(psicologo, diaSemana);
        return horarios.stream()
                .map(this::converterParaDTO)
                .collect(Collectors.toList());
    }

    /**
     * Busca horários com informação de disponibilidade para um dia específico
     */
    public List<HorarioDisponivelDTO> buscarHorariosDisponiveisComDisponibilidade(Long psicologoId, DayOfWeek diaSemana, java.time.LocalDate data) {
        Usuario psicologo = usuarioService.buscarPorId(psicologoId);
        List<HorarioDisponivel> horarios = horarioDisponivelRepository
                .findHorariosAtivosOrdenados(psicologo, diaSemana);
        return horarios.stream()
                .map(this::converterParaDTO)
                .collect(Collectors.toList());
    }

    /**
     * Busca horários livres (sem paciente vinculado) de um psicólogo
     */
    public List<HorarioDisponivelDTO> buscarHorariosLivres(Long psicologoId) {
        Usuario psicologo = usuarioService.buscarPorId(psicologoId);
        List<HorarioDisponivel> horarios = horarioDisponivelRepository
                .findByPsicologoAndPacienteIsNullAndAtivoTrue(psicologo);
        return horarios.stream()
                .map(this::converterParaDTO)
                .collect(Collectors.toList());
    }

    /**
     * Busca horários ocupados (com paciente vinculado) de um psicólogo
     */
    public List<HorarioDisponivelDTO> buscarHorariosOcupados(Long psicologoId) {
        List<HorarioDisponivel> horarios = horarioDisponivelRepository.findByPacienteIdIsNotNull()
                .stream()
                .filter(h -> h.getPsicologo().getId().equals(psicologoId))
                .collect(Collectors.toList());
        return horarios.stream()
                .map(this::converterParaDTO)
                .collect(Collectors.toList());
    }

    /**
     * Busca horário por ID
     */
    public HorarioDisponivelDTO buscarPorId(Long id) {
        Optional<HorarioDisponivel> horario = horarioDisponivelRepository.findById(id);
        if (horario.isPresent()) {
            return converterParaDTO(horario.get());
        } else {
            throw new RuntimeException("Horário não encontrado");
        }
    }

    /**
     * Gera horários disponíveis para uma configuração de agenda
     */
    @Transactional
    public void gerarHorariosParaConfiguracao(Object configuracao) {
        if (!(configuracao instanceof ConfiguracaoAgenda)) {
            return;
        }
        
        ConfiguracaoAgenda config = (ConfiguracaoAgenda) configuracao;
        
        // Remove horários existentes desta configuração
        excluirPorConfiguracao(config);
        
        // Gera novos horários baseados na configuração
        LocalTime horarioAtual = config.getHorarioInicio();
        
        // intervaloMinutos representa o tempo total entre o início de um atendimento e o início do próximo
        // Por exemplo: 60 minutos = 50 min de atendimento + 10 min de intervalo
        int intervaloTotalMinutos = config.getIntervaloMinutos();
        
        // Duração padrão do atendimento (50 minutos é comum na psicologia)
        int duracaoAtendimento = Math.max(45, intervaloTotalMinutos - 10); // Mínimo 45 min, deixando pelo menos 10 min de intervalo
        if (intervaloTotalMinutos <= 50) {
            duracaoAtendimento = intervaloTotalMinutos - 10; // Para intervalos menores, ajusta proporcionalmente
        }
        
        while (horarioAtual.isBefore(config.getHorarioFim())) {
            LocalTime horarioFimAtendimento = horarioAtual.plusMinutes(duracaoAtendimento);
            
            // Verifica se o horário de fim do atendimento não ultrapassa o limite geral
            if (horarioFimAtendimento.isAfter(config.getHorarioFim())) {
                break;
            }
            
            // Verifica se conflita com o período de pausa (almoço)
            boolean conflitaComPausa = false;
            if (config.getInicioPausa() != null && config.getVoltaPausa() != null) {
                LocalTime inicioPausa = config.getInicioPausa();
                LocalTime fimPausa = config.getVoltaPausa();
                
                // Verifica se o atendimento completo (início + duração) conflita com a pausa
                boolean inicioNaPausa = (horarioAtual.equals(inicioPausa) || horarioAtual.isAfter(inicioPausa)) && horarioAtual.isBefore(fimPausa);
                boolean fimNaPausa = (horarioFimAtendimento.equals(inicioPausa) || horarioFimAtendimento.isAfter(inicioPausa)) && horarioFimAtendimento.isBefore(fimPausa);
                boolean atravessaPausa = horarioAtual.isBefore(inicioPausa) && horarioFimAtendimento.isAfter(inicioPausa);
                
                conflitaComPausa = inicioNaPausa || fimNaPausa || atravessaPausa;
            }
            
            // Se não conflita com a pausa, cria o horário
            if (!conflitaComPausa) {
                HorarioDisponivel horario = new HorarioDisponivel();
                horario.setConfiguracaoAgenda(config);
                horario.setPsicologo(config.getPsicologo());
                horario.setDiaSemana(config.getDiaSemana());
                horario.setHoraInicio(horarioAtual);
                horario.setHoraFim(horarioFimAtendimento);
                horario.setAtivo(true);
                horario.setPacienteId(null); // Inicialmente disponível
                
                horarioDisponivelRepository.save(horario);
            }
            
            // Avança para o próximo horário
            horarioAtual = horarioAtual.plusMinutes(intervaloTotalMinutos);
            
            // Se o próximo horário está dentro da pausa, pula para depois da pausa
            if (config.getInicioPausa() != null && config.getVoltaPausa() != null) {
                LocalTime inicioPausa = config.getInicioPausa();
                LocalTime fimPausa = config.getVoltaPausa();
                
                if ((horarioAtual.equals(inicioPausa) || horarioAtual.isAfter(inicioPausa)) && horarioAtual.isBefore(fimPausa)) {
                    horarioAtual = fimPausa;
                }
            }
        }
    }

    /**
     * Verifica se uma configuração pode ser excluída
     */
    public boolean podeExcluirConfiguracao(Object configuracao) {
        if (!(configuracao instanceof ConfiguracaoAgenda)) {
            return true;
        }
        
        ConfiguracaoAgenda config = (ConfiguracaoAgenda) configuracao;
        // Verifica se existem horários ocupados (com pacientes) nesta configuração
        return !horarioDisponivelRepository.existsByConfiguracaoAgendaAndPacienteIsNotNull(config);
    }

    /**
     * Busca horários de uma configuração específica
     */
    public List<HorarioDisponivel> buscarPorConfiguracao(Object configuracao) {
        if (!(configuracao instanceof ConfiguracaoAgenda)) {
            return List.of();
        }
        
        ConfiguracaoAgenda config = (ConfiguracaoAgenda) configuracao;
        return horarioDisponivelRepository.findByConfiguracaoAgenda(config);
    }

    /**
     * Exclui todos os horários de uma configuração
     */
    @Transactional
    public void excluirPorConfiguracao(Object configuracao) {
        if (!(configuracao instanceof ConfiguracaoAgenda)) {
            return;
        }
        
        ConfiguracaoAgenda config = (ConfiguracaoAgenda) configuracao;
        horarioDisponivelRepository.deleteByConfiguracaoAgenda(config);
    }

    /**
     * Busca horários com informação de disponibilidade para uma data específica
     */
    public List<java.util.Map<String, Object>> buscarHorariosComDisponibilidade(Object configuracao, java.time.LocalDate data) {
        if (!(configuracao instanceof ConfiguracaoAgenda)) {
            return List.of();
        }
        
        ConfiguracaoAgenda config = (ConfiguracaoAgenda) configuracao;
        List<HorarioDisponivel> horarios = horarioDisponivelRepository.findByConfiguracaoAgenda(config);
        
        return horarios.stream()
                .map(h -> {
                    java.util.Map<String, Object> map = new java.util.HashMap<>();
                    map.put("id", h.getId());
                    map.put("inicio", h.getHoraInicio());
                    map.put("fim", h.getHoraFim());
                    map.put("disponivel", h.getPacienteId() == null);
                    map.put("pacienteId", h.getPacienteId());
                    return map;
                })
                .collect(Collectors.toList());
    }

    /**
     * Converte entidade para DTO
     */
    private HorarioDisponivelDTO converterParaDTO(HorarioDisponivel horario) {
        HorarioDisponivelDTO dto = new HorarioDisponivelDTO();
        dto.setId(horario.getId());
        dto.setConfiguracaoAgendaId(horario.getConfiguracaoAgenda().getId());
        dto.setPsicologoId(horario.getPsicologo().getId());
        dto.setDiaSemana(horario.getDiaSemana());
        dto.setHoraInicio(horario.getHoraInicio());
        dto.setHoraFim(horario.getHoraFim());
        dto.setAtivo(horario.getAtivo());
        dto.setDisponivel(horario.isDisponivel());

        if (horario.getPacienteId() != null) {
            dto.setPacienteId(horario.getPacienteId());
        }

        return dto;
    }
}
