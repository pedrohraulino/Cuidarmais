package cuidar.mais.api.service;

import cuidar.mais.api.dto.HorarioDisponivelDTO;
import cuidar.mais.api.models.ConfiguracaoAgenda;
import cuidar.mais.api.models.HorarioDisponivel;
import cuidar.mais.api.models.Usuario;
import cuidar.mais.api.repository.HorarioDisponivelRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class HorarioDisponivelService {

    @Autowired
    private HorarioDisponivelRepository horarioDisponivelRepository;

    @Autowired
    private UsuarioService usuarioService;

    @Autowired
    private AgendamentoService agendamentoService;

    /**
     * Busca todos os horários disponíveis de um psicólogo
     */
    public List<HorarioDisponivelDTO> buscarPorPsicologo(Long psicologoId) {
        Usuario psicologo = usuarioService.buscarPorId(psicologoId);
        return horarioDisponivelRepository.findByPsicologo(psicologo).stream()
                .map(this::converterParaDTO)
                .collect(Collectors.toList());
    }

    /**
     * Busca todos os horários disponíveis de um psicólogo em um dia específico
     * Inclui os horários indisponíveis que pertencem ao usuário atual
     */
    public List<HorarioDisponivelDTO> buscarPorPsicologoEDiaSemana(Long psicologoId, DayOfWeek diaSemana) {
        Usuario psicologo = usuarioService.buscarPorId(psicologoId);
        List<HorarioDisponivel> horarios = horarioDisponivelRepository.findHorariosAtivos(psicologo, diaSemana);

        // Obtém a data atual para o dia da semana especificado
        LocalDate dataAtual = LocalDate.now();
        while (dataAtual.getDayOfWeek() != diaSemana) {
            dataAtual = dataAtual.plusDays(1);
        }

        // Data final para verificação de disponibilidade
        final LocalDate dataVerificacao = dataAtual;

        return horarios.stream()
                .map(horario -> {
                    HorarioDisponivelDTO dto = converterParaDTO(horario);

                    // Verifica disponibilidade
                    boolean disponivel = agendamentoService.isHorarioDisponivel(
                            psicologoId, dataVerificacao, horario.getHoraInicio(), horario.getHoraFim());
                    dto.setDisponivel(disponivel);

                    return dto;
                })
                // Não filtra os horários indisponíveis, para que os horários do usuário atual sejam incluídos
                .collect(Collectors.toList());
    }

    /**
     * Busca todos os horários disponíveis de um psicólogo em um dia específico com informação de disponibilidade
     */
    public List<HorarioDisponivelDTO> buscarHorariosDisponiveisComDisponibilidade(Long psicologoId, DayOfWeek diaSemana, LocalDate data) {
        Usuario psicologo = usuarioService.buscarPorId(psicologoId);
        List<HorarioDisponivel> horarios = horarioDisponivelRepository.findHorariosAtivos(psicologo, diaSemana);

        return horarios.stream()
                .map(horario -> {
                    HorarioDisponivelDTO dto = converterParaDTO(horario);

                    // Verifica disponibilidade se a data for fornecida
                    if (data != null) {
                        boolean disponivel = agendamentoService.isHorarioDisponivel(
                                psicologoId, data, horario.getHoraInicio(), horario.getHoraFim());
                        dto.setDisponivel(disponivel);
                    } else {
                        dto.setDisponivel(true); // Assume disponível se não verificar
                    }

                    return dto;
                })
                .collect(Collectors.toList());
    }

    /**
     * Gera e salva os horários disponíveis para uma configuração de agenda
     */
    @Transactional
    public List<HorarioDisponivelDTO> gerarESalvarHorarios(ConfiguracaoAgenda configuracao) {
        // Primeiro, exclui os horários existentes para esta configuração
        horarioDisponivelRepository.deleteByConfiguracaoAgenda(configuracao);

        List<HorarioDisponivel> horarios = new ArrayList<>();

        // Duração de cada atendimento: 50 minutos
        int duracaoAtendimento = 50;

        // Horário atual para iteração
        LocalTime horarioAtual = configuracao.getHorarioInicio();

        // Gera os horários até o fim do expediente
        while (horarioAtual.plusMinutes(duracaoAtendimento).isBefore(configuracao.getHorarioFim()) ||
               horarioAtual.plusMinutes(duracaoAtendimento).equals(configuracao.getHorarioFim())) {

            // Verifica se o horário está dentro do período de pausa
            boolean estaNaPausa = false;
            if (configuracao.getInicioPausa() != null && configuracao.getFimPausa() != null) {
                estaNaPausa = !horarioAtual.isBefore(configuracao.getInicioPausa()) &&
                              horarioAtual.isBefore(configuracao.getFimPausa());
            }

            // Adiciona o horário se não estiver na pausa
            if (!estaNaPausa) {
                LocalTime horarioFim = horarioAtual.plusMinutes(duracaoAtendimento);

                HorarioDisponivel horario = new HorarioDisponivel();
                horario.setConfiguracaoAgenda(configuracao);
                horario.setPsicologo(configuracao.getPsicologo());
                horario.setDiaSemana(configuracao.getDiaSemana());
                horario.setHoraInicio(horarioAtual);
                horario.setHoraFim(horarioFim);
                horario.setAtivo(true);

                horarios.add(horario);
            }

            // Avança para o próximo horário considerando o intervalo
            horarioAtual = horarioAtual.plusMinutes(duracaoAtendimento + configuracao.getIntervaloMinutos());
        }

        // Salva todos os horários gerados
        horarios = horarioDisponivelRepository.saveAll(horarios);

        // Converte para DTOs
        return horarios.stream()
                .map(this::converterParaDTO)
                .collect(Collectors.toList());
    }

    /**
     * Exclui todos os horários disponíveis associados a uma configuração de agenda
     */
    @Transactional
    public void excluirPorConfiguracao(ConfiguracaoAgenda configuracao) {
        horarioDisponivelRepository.deleteByConfiguracaoAgenda(configuracao);
    }

    /**
     * Desativa um horário disponível (seta o campo ativo para false)
     * @param id ID do horário disponível
     * @return O horário disponível desativado
     */
    @Transactional
    public HorarioDisponivel desativarHorario(Long id) {
        HorarioDisponivel horario = horarioDisponivelRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Horário disponível não encontrado"));

        horario.setAtivo(false);
        return horarioDisponivelRepository.save(horario);
    }

    /**
     * Reativa um horário disponível (seta o campo ativo para true)
     * @param id ID do horário disponível
     * @return O horário disponível reativado
     */
    @Transactional
    public HorarioDisponivel reativarHorario(Long id) {
        HorarioDisponivel horario = horarioDisponivelRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Horário disponível não encontrado"));

        horario.setAtivo(true);
        return horarioDisponivelRepository.save(horario);
    }

    /**
     * Converte uma entidade HorarioDisponivel para DTO
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
        dto.setDisponivel(true); // Por padrão, assume que está disponível
        return dto;
    }
}
