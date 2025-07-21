package cuidar.mais.api.service;

import cuidar.mais.api.dto.SessaoDTO;
import cuidar.mais.api.models.Sessao;
import cuidar.mais.api.models.Paciente;
import cuidar.mais.api.models.HorarioDisponivel;
import cuidar.mais.api.repository.SessaoRepository;
import cuidar.mais.api.repository.HorarioDisponivelRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class SessaoService {

    @Autowired
    private SessaoRepository sessaoRepository;

    @Autowired
    private HorarioDisponivelRepository horarioDisponivelRepository;

    public List<SessaoDTO> listarSessoesPorPsicologo(Long psicologoId) {
        List<Sessao> sessoes = sessaoRepository.findByPsicologoIdAndAtivoTrueOrderByDataSessao(psicologoId);
        return sessoes.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    public List<SessaoDTO> listarSessoesPorPaciente(Long pacienteId) {
        List<Sessao> sessoes = sessaoRepository.findByPacienteIdAndAtivoTrueOrderByDataSessao(pacienteId);
        return sessoes.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    public List<SessaoDTO> listarSessoesPorPsicologoEPeriodo(Long psicologoId, LocalDate dataInicio, LocalDate dataFim) {
        List<Sessao> sessoes = sessaoRepository.findByPsicologoIdAndDataSessaoBetweenAndAtivoTrue(
                psicologoId, dataInicio, dataFim);
        
        return sessoes.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    public List<SessaoDTO> listarSessoesPendentes(Long psicologoId) {
        List<Sessao> sessoes = sessaoRepository.findByPsicologoIdAndStatusAgendadaAndAtivoTrue(psicologoId);
        return sessoes.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    public List<SessaoDTO> listarSessoesRealizadas(Long psicologoId) {
        List<Sessao> sessoes = sessaoRepository.findByPsicologoIdAndStatusRealizadaAndAtivoTrue(psicologoId);
        return sessoes.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    @Transactional
    public SessaoDTO marcarSessaoComoRealizada(Long sessaoId, String observacoes) {
        Optional<Sessao> sessaoOpt = sessaoRepository.findById(sessaoId);
        if (sessaoOpt.isEmpty()) {
            throw new RuntimeException("Sessão não encontrada");
        }

        Sessao sessao = sessaoOpt.get();
        sessao.setStatus(Sessao.StatusSessao.REALIZADA);
        sessao.setDataAtualizacao(LocalDateTime.now());
        
        if (observacoes != null && !observacoes.trim().isEmpty()) {
            sessao.setObservacoes(observacoes);
        }

        Sessao sessaoSalva = sessaoRepository.save(sessao);
        return convertToDTO(sessaoSalva);
    }

    @Transactional
    public SessaoDTO remarcarSessao(Long sessaoId, LocalDate novaData) {
        Optional<Sessao> sessaoOpt = sessaoRepository.findById(sessaoId);
        if (sessaoOpt.isEmpty()) {
            throw new RuntimeException("Sessão não encontrada");
        }

        Sessao sessao = sessaoOpt.get();
        
        // Verificar se já foi realizada
        if (sessao.getStatus() == Sessao.StatusSessao.REALIZADA) {
            throw new RuntimeException("Não é possível remarcar uma sessão já realizada");
        }

        // Verificar conflito com outras sessões do mesmo psicólogo
        List<Sessao> sessoesConflitantes = sessaoRepository.findConflictingSessions(
                sessao.getPsicologo().getId(),
                novaData,
                sessao.getHoraInicio(),
                sessao.getHoraFim()
        );

        // Remover a própria sessão da verificação de conflito
        sessoesConflitantes = sessoesConflitantes.stream()
                .filter(s -> !s.getId().equals(sessaoId))
                .collect(Collectors.toList());

        if (!sessoesConflitantes.isEmpty()) {
            throw new RuntimeException("Já existe uma sessão agendada neste horário");
        }

        sessao.setDataSessao(novaData);
        Sessao sessaoSalva = sessaoRepository.save(sessao);
        return convertToDTO(sessaoSalva);
    }

    @Transactional
    public void cancelarSessao(Long sessaoId) {
        Optional<Sessao> sessaoOpt = sessaoRepository.findById(sessaoId);
        if (sessaoOpt.isEmpty()) {
            throw new RuntimeException("Sessão não encontrada");
        }

        Sessao sessao = sessaoOpt.get();
        
        // Verificar se já foi realizada
        if (sessao.getStatus() == Sessao.StatusSessao.REALIZADA) {
            throw new RuntimeException("Não é possível cancelar uma sessão já realizada");
        }

        sessao.setStatus(Sessao.StatusSessao.CANCELADA);
        sessao.setAtivo(false);
        sessaoRepository.save(sessao);
    }

    public SessaoDTO buscarSessaoPorId(Long sessaoId) {
        Optional<Sessao> sessao = sessaoRepository.findById(sessaoId);
        if (sessao.isEmpty()) {
            throw new RuntimeException("Sessão não encontrada");
        }
        return convertToDTO(sessao.get());
    }

    public long contarSessoesPendentes(Long pacienteId) {
        return sessaoRepository.countByPacienteIdAndStatusAgendadaAndAtivoTrue(pacienteId);
    }

    public long contarSessoesRealizadas(Long pacienteId) {
        return sessaoRepository.countByPacienteIdAndStatusRealizadaAndAtivoTrue(pacienteId);
    }

    @Transactional
    public SessaoDTO atualizarObservacoes(Long sessaoId, String observacoes) {
        Optional<Sessao> sessaoOpt = sessaoRepository.findById(sessaoId);
        if (sessaoOpt.isEmpty()) {
            throw new RuntimeException("Sessão não encontrada");
        }

        Sessao sessao = sessaoOpt.get();
        sessao.setObservacoes(observacoes);
        sessao.setDataAtualizacao(LocalDateTime.now());
        
        Sessao sessaoSalva = sessaoRepository.save(sessao);
        return convertToDTO(sessaoSalva);
    }

    private SessaoDTO convertToDTO(Sessao sessao) {
        SessaoDTO dto = new SessaoDTO();
        dto.setId(sessao.getId());
        dto.setPacienteId(sessao.getPaciente().getId());
        dto.setNomePaciente(sessao.getPaciente().getNome());
        dto.setPsicologoId(sessao.getPsicologo().getId());
        dto.setDataSessao(sessao.getDataSessao());
        dto.setHoraInicio(sessao.getHoraInicio());
        dto.setHoraFim(sessao.getHoraFim());
        dto.setNumeroSessao(sessao.getNumeroSessao());
        dto.setStatus(sessao.getStatus());
        dto.setObservacoes(sessao.getObservacoes());
        dto.setAtivo(sessao.getAtivo());
        dto.setDataCriacao(sessao.getDataCriacao());
        dto.setDataAtualizacao(sessao.getDataAtualizacao());
        
        return dto;
    }

    /**
     * Cancela todas as sessões futuras de um paciente
     * Remove fisicamente as sessões não realizadas para evitar constraint de FK
     */
    @Transactional
    public void cancelarSessoesFuturas(Paciente paciente) {
        LocalDate hoje = LocalDate.now();
        List<Sessao> sessoesFuturas = sessaoRepository.findSessoesFuturasPaciente(paciente, hoje);
        
        List<Sessao> sessoesParaDeletar = new ArrayList<>();
        List<Sessao> sessoesParaAtualizar = new ArrayList<>();
        
        for (Sessao sessao : sessoesFuturas) {
            if (sessao.getStatus() == Sessao.StatusSessao.REALIZADA) {
                // Sessões realizadas são mantidas, mas marcadas como inativas
                sessao.setAtivo(false);
                sessao.setDataAtualizacao(LocalDateTime.now());
                sessoesParaAtualizar.add(sessao);
            } else {
                // Sessões não realizadas são deletadas fisicamente
                sessoesParaDeletar.add(sessao);
            }
        }
        
        // Salva sessões realizadas como inativas
        if (!sessoesParaAtualizar.isEmpty()) {
            sessaoRepository.saveAll(sessoesParaAtualizar);
        }
        
        // Remove fisicamente sessões não realizadas
        if (!sessoesParaDeletar.isEmpty()) {
            sessaoRepository.deleteAll(sessoesParaDeletar);
        }
    }

    /**
     * Cria sessões para um paciente baseado no seu pacote
     */
    @Transactional
    public void criarSessoesParaPaciente(Paciente paciente) {
        if (paciente.getHorarioDisponivelId() == null) {
            throw new RuntimeException("Paciente não possui horário definido");
        }

        // Busca o horário disponível vinculado ao paciente
        Optional<HorarioDisponivel> horarioOpt = horarioDisponivelRepository.findById(paciente.getHorarioDisponivelId());
        
        HorarioDisponivel horarioDisponivel;
        if (horarioOpt.isPresent()) {
            // Horário encontrado
            horarioDisponivel = horarioOpt.get();
        } else {
            throw new RuntimeException("Horário disponível não encontrado para o paciente");
        }

        // Calcula quantas sessões já existem
        List<Sessao> sessoesExistentes = sessaoRepository.findByPacienteAndAtivoTrue(paciente);
        
        // Calcula quantas sessões precisa criar
        int sessoesPorPacote = paciente.getSessoesPorPacote();
        int sessoesPraCriar = sessoesPorPacote - sessoesExistentes.size();
        
        if (sessoesPraCriar <= 0) {
            return; // Já tem todas as sessões
        }

        // Encontra a próxima data disponível
        LocalDate proximaData = encontrarProximaDataDisponivel(horarioDisponivel);
        
        List<Sessao> novasSessoes = new ArrayList<>();
        int numeroSessao = sessoesExistentes.size() + 1;
        
        for (int i = 0; i < sessoesPraCriar; i++) {
            Sessao sessao = new Sessao();
            sessao.setPaciente(paciente);
            sessao.setPsicologo(paciente.getPsicologo());
            sessao.setHorarioDisponivel(horarioDisponivel); // CRITICAL: Set the horario_disponivel_id
            sessao.setDataSessao(proximaData);
            sessao.setHoraInicio(horarioDisponivel.getHoraInicio());
            sessao.setHoraFim(horarioDisponivel.getHoraFim());
            sessao.setNumeroSessao(numeroSessao++);
            sessao.setStatus(Sessao.StatusSessao.AGENDADA);
            sessao.setAtivo(true);
            sessao.setDataCriacao(LocalDateTime.now());
            
            novasSessoes.add(sessao);
            
            // Próxima semana
            proximaData = proximaData.plusWeeks(1);
        }
        
        sessaoRepository.saveAll(novasSessoes);
        
        // Marca o horário como ocupado (ativo = false) após criar as sessões
        if (!novasSessoes.isEmpty()) {
            horarioDisponivel.setAtivo(false);
            horarioDisponivelRepository.save(horarioDisponivel);
        }
    }

    /**
     * Conta sessões agendadas de um paciente
     */
    public long contarSessoesAgendadas(Paciente paciente) {
        return sessaoRepository.countByPacienteIdAndStatusAgendadaAndAtivoTrue(paciente.getId());
    }

    /**
     * Cancela todas as sessões relacionadas a uma configuração de agenda
     * Remove fisicamente as sessões não realizadas para evitar constraint de FK
     */
    @Transactional
    public void cancelarSessoesPorConfiguracaoAgenda(List<HorarioDisponivel> horariosDisponiveis) {
        for (HorarioDisponivel horario : horariosDisponiveis) {
            List<Sessao> sessoesDoHorario = sessaoRepository.findByHorarioDisponivelAndAtivoTrue(horario);
            
            List<Sessao> sessoesParaDeletar = new ArrayList<>();
            List<Sessao> sessoesParaAtualizar = new ArrayList<>();
            
            for (Sessao sessao : sessoesDoHorario) {
                if (sessao.getStatus() == Sessao.StatusSessao.REALIZADA) {
                    // Sessões realizadas são mantidas, mas marcadas como inativas
                    sessao.setAtivo(false);
                    sessao.setDataAtualizacao(LocalDateTime.now());
                    sessoesParaAtualizar.add(sessao);
                } else {
                    // Sessões não realizadas são deletadas fisicamente
                    sessoesParaDeletar.add(sessao);
                }
            }
            
            // Salva sessões realizadas como inativas
            if (!sessoesParaAtualizar.isEmpty()) {
                sessaoRepository.saveAll(sessoesParaAtualizar);
            }
            
            // Remove fisicamente sessões não realizadas
            if (!sessoesParaDeletar.isEmpty()) {
                sessaoRepository.deleteAll(sessoesParaDeletar);
            }
        }
    }

    /**
     * Encontra a próxima data disponível para criar sessões
     */
    private LocalDate encontrarProximaDataDisponivel(HorarioDisponivel horarioDisponivel) {
        LocalDate hoje = LocalDate.now();
        LocalDate proximaData = hoje;
        
        // Encontra o próximo dia da semana correspondente
        while (proximaData.getDayOfWeek() != horarioDisponivel.getDiaSemana()) {
            proximaData = proximaData.plusDays(1);
        }
        
        // Se a data é hoje mas já passou do horário, vai para a próxima semana
        if (proximaData.equals(hoje)) {
            LocalTime agora = LocalTime.now();
            if (agora.isAfter(horarioDisponivel.getHoraInicio())) {
                proximaData = proximaData.plusWeeks(1);
            }
        }
        
        return proximaData;
    }
}
