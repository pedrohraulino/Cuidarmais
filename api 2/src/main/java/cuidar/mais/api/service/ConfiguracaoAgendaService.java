package cuidar.mais.api.service;

import cuidar.mais.api.dto.ConfiguracaoAgendaDTO;
import cuidar.mais.api.models.ConfiguracaoAgenda;
import cuidar.mais.api.models.HorarioDisponivel;
import cuidar.mais.api.models.Usuario;
import cuidar.mais.api.repository.ConfiguracaoAgendaRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class ConfiguracaoAgendaService {

    @Autowired
    private ConfiguracaoAgendaRepository configuracaoAgendaRepository;

    @Autowired
    private UsuarioService usuarioService;

    @Autowired
    private HorarioDisponivelService horarioDisponivelService;

    @Autowired
    private SessaoService sessaoService;

    /**
     * Busca todas as configurações de agenda de um psicólogo
     */
    public List<ConfiguracaoAgendaDTO> buscarPorPsicologo(Long psicologoId) {
        List<ConfiguracaoAgenda> configuracoes = configuracaoAgendaRepository
                .findByPsicologoIdAndAtivoTrueOrderByDiaSemana(psicologoId);
        
        return configuracoes.stream()
                .map(this::converterParaDTO)
                .collect(Collectors.toList());
    }

    /**
     * Busca uma configuração específica por ID
     */
    public ConfiguracaoAgendaDTO buscarPorId(Long id) {
        ConfiguracaoAgenda configuracao = configuracaoAgendaRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Configuração de agenda não encontrada"));
        
        return converterParaDTO(configuracao);
    }

    /**
     * Salva ou atualiza uma configuração de agenda
     */
    @Transactional
    public ConfiguracaoAgendaDTO salvar(ConfiguracaoAgendaDTO dto) {
        Usuario psicologo = usuarioService.buscarPorId(dto.getPsicologoId());
        
        ConfiguracaoAgenda configuracao;
        
        if (dto.getId() != null) {
            // Atualização
            configuracao = configuracaoAgendaRepository.findById(dto.getId())
                    .orElseThrow(() -> new RuntimeException("Configuração não encontrada"));
        } else {
            // Nova configuração - verifica se já existe para este dia
            var existente = configuracaoAgendaRepository
                    .findByPsicologoAndDiaSemanaAndAtivoTrue(psicologo, dto.getDiaSemana());
            
            if (existente.isPresent()) {
                configuracao = existente.get();
            } else {
                configuracao = new ConfiguracaoAgenda();
                configuracao.setPsicologo(psicologo);
                configuracao.setDiaSemana(dto.getDiaSemana());
            }
        }

        // Atualiza os dados
        configuracao.setHorarioInicio(dto.getHorarioInicio());
        configuracao.setIntervaloMinutos(dto.getIntervaloMinutos());
        configuracao.setHorarioFim(dto.getHorarioFim());
        configuracao.setInicioPausa(dto.getInicioPausa());
        configuracao.setVoltaPausa(dto.getVoltaPausa());
        configuracao.setAtivo(dto.getAtivo() != null ? dto.getAtivo() : true);

        configuracao = configuracaoAgendaRepository.save(configuracao);

        // Regenera os horários disponíveis para esta configuração
        if (configuracao.getAtivo()) {
            horarioDisponivelService.gerarHorariosParaConfiguracao(configuracao);
        }

        return converterParaDTO(configuracao);
    }

    /**
     * Verifica se uma configuração pode ser excluída
     */
    public boolean podeExcluir(Long id) {
        ConfiguracaoAgenda configuracao = configuracaoAgendaRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Configuração não encontrada"));
        
        return horarioDisponivelService.podeExcluirConfiguracao(configuracao);
    }

    /**
     * Exclui uma configuração de agenda
     */
    @Transactional
    public void excluir(Long id) {
        ConfiguracaoAgenda configuracao = configuracaoAgendaRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Configuração não encontrada"));
        
        // Busca todos os horários desta configuração
        List<HorarioDisponivel> horariosDisponiveis = horarioDisponivelService.buscarPorConfiguracao(configuracao);
        
        // Cancela todas as sessões que referenciam estes horários
        if (!horariosDisponiveis.isEmpty()) {
            sessaoService.cancelarSessoesPorConfiguracaoAgenda(horariosDisponiveis);
        }

        // Remove todos os horários disponíveis desta configuração
        horarioDisponivelService.excluirPorConfiguracao(configuracao);
        
        // Remove a configuração
        configuracaoAgendaRepository.delete(configuracao);
    }

    /**
     * Gera horários disponíveis com informação de disponibilidade
     */
    public List<Map<String, Object>> gerarHorariosDisponiveisComDisponibilidade(
            Long psicologoId, DayOfWeek diaSemana, LocalDate data) {
        
        Usuario psicologo = usuarioService.buscarPorId(psicologoId);
        
        var configuracao = configuracaoAgendaRepository
                .findByPsicologoAndDiaSemanaAndAtivoTrue(psicologo, diaSemana);
        
        if (configuracao.isEmpty()) {
            return new ArrayList<>();
        }

        return horarioDisponivelService.buscarHorariosComDisponibilidade(
                configuracao.get(), data);
    }

    /**
     * Converte entidade para DTO
     */
    private ConfiguracaoAgendaDTO converterParaDTO(ConfiguracaoAgenda configuracao) {
        ConfiguracaoAgendaDTO dto = new ConfiguracaoAgendaDTO();
        dto.setId(configuracao.getId());
        dto.setPsicologoId(configuracao.getPsicologo().getId());
        dto.setDiaSemana(configuracao.getDiaSemana());
        dto.setHorarioInicio(configuracao.getHorarioInicio());
        dto.setIntervaloMinutos(configuracao.getIntervaloMinutos());
        dto.setHorarioFim(configuracao.getHorarioFim());
        dto.setInicioPausa(configuracao.getInicioPausa());
        dto.setVoltaPausa(configuracao.getVoltaPausa());
        dto.setAtivo(configuracao.getAtivo());
        dto.setDataCriacao(configuracao.getDataCriacao());
        dto.setDataAtualizacao(configuracao.getDataAtualizacao());
        return dto;
    }
}
