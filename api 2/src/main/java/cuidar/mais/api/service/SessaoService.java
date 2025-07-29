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
        // Adiciona dados do paciente
        dto.setPacienteEmail(sessao.getPaciente().getEmail());
        dto.setPacienteTelefone(sessao.getPaciente().getTelefone());
        dto.setPacienteSobrenome(sessao.getPaciente().getSobrenome());
        dto.setPacienteImagem(sessao.getPaciente().getImagemBase64());
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
            sessao.setHorarioDisponivelId(horarioDisponivel.getId()); // CRITICAL: Set the horario_disponivel_id
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
     * Cria sessões adicionais para um paciente sem modificar o valor original do sessoesPorPacote
     */
    @Transactional
    public void criarSessoesAdicionais(Paciente paciente, Integer quantidadeSessoes) {
        if (paciente.getHorarioDisponivelId() == null) {
            throw new RuntimeException("Paciente não possui horário definido");
        }

        // Busca o horário disponível vinculado ao paciente
        Optional<HorarioDisponivel> horarioOpt = horarioDisponivelRepository.findById(paciente.getHorarioDisponivelId());
        
        HorarioDisponivel horarioDisponivel;
        if (horarioOpt.isPresent()) {
            horarioDisponivel = horarioOpt.get();
        } else {
            throw new RuntimeException("Horário disponível não encontrado para o paciente");
        }

        // Busca sessões existentes para calcular o próximo número
        List<Sessao> sessoesExistentes = sessaoRepository.findByPacienteAndAtivoTrue(paciente);
        
        // Encontra a próxima data disponível após a última sessão
        LocalDate proximaData = encontrarProximaDataDisponivel(horarioDisponivel);
        
        // Se já existem sessões, procura a data após a última sessão
        if (!sessoesExistentes.isEmpty()) {
            LocalDate ultimaDataSessao = sessoesExistentes.stream()
                    .map(Sessao::getDataSessao)
                    .max(LocalDate::compareTo)
                    .orElse(LocalDate.now());
            
            // Próxima semana após a última sessão
            proximaData = ultimaDataSessao.plusWeeks(1);
            
            // Garante que a data seja no dia correto da semana
            while (!proximaData.getDayOfWeek().equals(horarioDisponivel.getDiaSemana())) {
                proximaData = proximaData.plusDays(1);
            }
        }
        
        List<Sessao> novasSessoes = new ArrayList<>();
        int numeroSessao = sessoesExistentes.size() + 1;
        
        // Cria apenas as sessões extras solicitadas
        for (int i = 0; i < quantidadeSessoes; i++) {
            Sessao sessao = new Sessao();
            sessao.setPaciente(paciente);
            sessao.setPsicologo(paciente.getPsicologo());
            sessao.setHorarioDisponivelId(horarioDisponivel.getId());
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
            List<Sessao> sessoesDoHorario = sessaoRepository.findByHorarioDisponivelIdAndAtivoTrue(horario.getId());
            
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

    /**
     * Conta o número total de sessões existentes (agendadas + realizadas) para um paciente
     */
    public long contarSessoesExistentes(Paciente paciente) {
        return sessaoRepository.countByPacienteIdAndAtivoTrue(paciente.getId());
    }

    /**
     * Conta apenas sessões ativas (AGENDADA) para um paciente
     */
    public long contarSessoesAtivas(Paciente paciente) {
        return sessaoRepository.countByPacienteIdAndStatusAgendadaAndAtivoTrue(paciente.getId());
    }

    /**
     * Cria sessões adicionais para um paciente
     */
    @Transactional
    public void criarSessoesAdicionais(Paciente paciente, int quantidadeSessoes) {
        if (paciente.getHorarioDisponivelId() == null) {
            return;
        }

        Optional<HorarioDisponivel> horarioOpt = horarioDisponivelRepository.findById(paciente.getHorarioDisponivelId());
        if (horarioOpt.isEmpty()) {
            return;
        }

        HorarioDisponivel horarioDisponivel = horarioOpt.get();
        
        // Encontra a última sessão agendada para este paciente
        List<Sessao> sessoesExistentes = sessaoRepository.findByPacienteIdAndAtivoTrueOrderByDataSessao(paciente.getId());
        
        LocalDate proximaData;
        int proximoNumeroSessao;
        
        if (sessoesExistentes.isEmpty()) {
            proximaData = encontrarProximaDataDisponivel(horarioDisponivel);
            proximoNumeroSessao = 1;
        } else {
            // Começa a partir da última sessão agendada + 1 semana
            Sessao ultimaSessao = sessoesExistentes.get(sessoesExistentes.size() - 1);
            proximaData = ultimaSessao.getDataSessao().plusWeeks(1);
            proximoNumeroSessao = ultimaSessao.getNumeroSessao() + 1;
        }

        // Cria as sessões adicionais
        for (int i = 0; i < quantidadeSessoes; i++) {
            Sessao sessao = new Sessao();
            sessao.setPaciente(paciente);
            sessao.setPsicologo(paciente.getPsicologo());
            sessao.setDataSessao(proximaData);
            sessao.setHoraInicio(horarioDisponivel.getHoraInicio());
            sessao.setHoraFim(horarioDisponivel.getHoraFim());
            sessao.setNumeroSessao(proximoNumeroSessao++);
            sessao.setStatus(Sessao.StatusSessao.AGENDADA);
            sessao.setAtivo(true);
            sessao.setHorarioDisponivelId(horarioDisponivel.getId());

            sessaoRepository.save(sessao);

            // Próxima sessão na semana seguinte
            proximaData = proximaData.plusWeeks(1);
        }
    }

    /**
     * Remove sessões agendadas (não realizadas) para um paciente
     */
    @Transactional
    public void removerSessoesAgendadas(Paciente paciente, int quantidadeParaRemover) {
        System.out.println("=== DEBUG: Removendo sessões ===");
        System.out.println("Paciente ID: " + paciente.getId());
        System.out.println("Quantidade para remover: " + quantidadeParaRemover);
        
        List<Sessao> sessoesAgendadas = sessaoRepository.findByPacienteIdAndStatusAndAtivoTrueOrderByDataSessaoDesc(
            paciente.getId());
        
        System.out.println("Sessões AGENDADAS encontradas: " + sessoesAgendadas.size());
        
        int removidas = 0;
        for (Sessao sessao : sessoesAgendadas) {
            if (removidas >= quantidadeParaRemover) {
                break;
            }
            
            System.out.println("Analisando sessão ID: " + sessao.getId() + 
                              ", Data: " + sessao.getDataSessao() + 
                              ", Status: " + sessao.getStatus());
            
            // Remove apenas sessões futuras que ainda não foram realizadas
            if (sessao.getDataSessao().isAfter(LocalDate.now()) || 
                (sessao.getDataSessao().equals(LocalDate.now()) && 
                 LocalTime.now().isBefore(sessao.getHoraInicio()))) {
                
                System.out.println("Marcando sessão como inativa: " + sessao.getId());
                sessao.setAtivo(false);
                sessaoRepository.save(sessao);
                removidas++;
            } else {
                System.out.println("Sessão não pode ser removida (já passou): " + sessao.getId());
            }
        }
        
        System.out.println("Total de sessões removidas: " + removidas);
        System.out.println("=== FIM DEBUG REMOÇÃO ===");
    }

    /**
     * Remove fisicamente todas as sessões de um paciente (usado quando paciente é inativado)
     */
    @Transactional
    public void apagarTodasSessoesPaciente(Paciente paciente) {
        System.out.println("=== DEBUG: Apagando todas as sessões do paciente ===");
        System.out.println("Paciente ID: " + paciente.getId());
        
        // Busca TODAS as sessões do paciente (ativas e inativas)
        List<Sessao> todasSessoes = sessaoRepository.findByPaciente(paciente);
        System.out.println("Total de sessões encontradas: " + todasSessoes.size());
        
        if (!todasSessoes.isEmpty()) {
            // Lista as sessões que serão apagadas para debug
            for (Sessao sessao : todasSessoes) {
                System.out.println("Apagando sessão ID: " + sessao.getId() + 
                                  ", Data: " + sessao.getDataSessao() + 
                                  ", Status: " + sessao.getStatus() + 
                                  ", Ativo: " + sessao.getAtivo());
            }
            
            // Remove fisicamente todas as sessões do paciente
            sessaoRepository.deleteAll(todasSessoes);
            System.out.println("Todas as sessões foram apagadas do banco de dados");
        } else {
            System.out.println("Nenhuma sessão encontrada para apagar");
        }
        
        System.out.println("=== FIM DEBUG APAGAR SESSÕES ===");
    }

    public List<SessaoDTO> listarSessoesPorPsicologoEData(Long psicologoId, LocalDate data) {
        List<Sessao> sessoes = sessaoRepository.findByPsicologoIdAndDataSessaoAndAtivoTrueOrderByHoraInicio(psicologoId, data);
        return sessoes.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    // Confirma uma sessão
    @Transactional
    public void confirmarSessao(Long id) {
        Sessao sessao = sessaoRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Sessão não encontrada"));
        sessao.setStatus(Sessao.StatusSessao.REALIZADA);
        sessaoRepository.save(sessao);
    }

    // Marca falta em uma sessão
    @Transactional
    public void marcarFaltou(Long id) {
        Sessao sessao = sessaoRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Sessão não encontrada"));
        sessao.setStatus(Sessao.StatusSessao.FALTOU);
        sessaoRepository.save(sessao);
    }

    // Cancela uma sessão
    @Transactional
    public void cancelarSessao(Long id) {
        Sessao sessao = sessaoRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Sessão não encontrada"));
        sessao.setStatus(Sessao.StatusSessao.CANCELADA);
        sessaoRepository.save(sessao);
    }
}
