package cuidar.mais.api.service;

import cuidar.mais.api.models.Agendamento;
import cuidar.mais.api.models.Usuario;
import cuidar.mais.api.repository.AgendamentoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class AgendamentoService {

    @Autowired
    private AgendamentoRepository agendamentoRepository;

    @Autowired
    private UsuarioService usuarioService;

    /**
     * Busca todos os agendamentos de um psicólogo
     */
    public List<Agendamento> buscarPorPsicologo(Long psicologoId) {
        Usuario psicologo = usuarioService.buscarPorId(psicologoId);
        return agendamentoRepository.findByPsicologo(psicologo);
    }

    /**
     * Busca todos os agendamentos ativos de um psicólogo
     */
    public List<Agendamento> buscarAtivosPorPsicologo(Long psicologoId) {
        Usuario psicologo = usuarioService.buscarPorId(psicologoId);
        return agendamentoRepository.findByPsicologoAndAtivo(psicologo, true);
    }

    /**
     * Busca todos os agendamentos de um psicólogo em uma data específica
     */
    public List<Agendamento> buscarPorPsicologoEData(Long psicologoId, LocalDate data) {
        Usuario psicologo = usuarioService.buscarPorId(psicologoId);
        return agendamentoRepository.findByPsicologoAndData(psicologo, data);
    }

    /**
     * Busca todos os agendamentos ativos de um psicólogo em uma data específica
     */
    public List<Agendamento> buscarAtivosPorPsicologoEData(Long psicologoId, LocalDate data) {
        Usuario psicologo = usuarioService.buscarPorId(psicologoId);
        return agendamentoRepository.findByPsicologoAndDataAndAtivo(psicologo, data, true);
    }

    /**
     * Verifica se um horário está disponível para agendamento
     * @param psicologoId ID do psicólogo
     * @param data Data do agendamento
     * @param horaInicio Hora de início do agendamento
     * @param horaFim Hora de fim do agendamento
     * @return true se o horário estiver disponível, false caso contrário
     */
    public boolean isHorarioDisponivel(Long psicologoId, LocalDate data, LocalTime horaInicio, LocalTime horaFim) {
        // Se não houver agendamentos na tabela, todos os horários estão disponíveis
        long totalAgendamentos = agendamentoRepository.count();
        System.out.println("Total de agendamentos na tabela: " + totalAgendamentos);

        if (totalAgendamentos == 0) {
            System.out.println("Não há agendamentos na tabela, horário disponível: " + data + " " + horaInicio + "-" + horaFim);
            return true;
        }

        Usuario psicologo = usuarioService.buscarPorId(psicologoId);
        boolean temConflito = agendamentoRepository.existsAgendamentoConflitante(psicologo, data, horaInicio, horaFim);
        System.out.println("Verificando disponibilidade para: " + data + " " + horaInicio + "-" + horaFim + " - Tem conflito: " + temConflito);

        return !temConflito;
    }

    /**
     * Cria um novo agendamento
     */
    @Transactional
    public Agendamento criar(Agendamento agendamento) {
        // Verificar se o horário está disponível
        if (!isHorarioDisponivel(
                agendamento.getPsicologo().getId(),
                agendamento.getData(),
                agendamento.getHoraInicio(),
                agendamento.getHoraFim())) {
            throw new RuntimeException("Horário não disponível para agendamento");
        }

        return agendamentoRepository.save(agendamento);
    }

    /**
     * Atualiza o status de um agendamento
     * Se o status for CANCELADO ou CONCLUIDO, o agendamento é marcado como inativo
     */
    @Transactional
    public Agendamento atualizarStatus(Long agendamentoId, Agendamento.StatusAgendamento novoStatus) {
        Agendamento agendamento = agendamentoRepository.findById(agendamentoId)
                .orElseThrow(() -> new RuntimeException("Agendamento não encontrado"));

        agendamento.setStatus(novoStatus);

        // Se o status for CANCELADO ou CONCLUIDO, marca o agendamento como inativo
        if (novoStatus == Agendamento.StatusAgendamento.CANCELADO || 
            novoStatus == Agendamento.StatusAgendamento.CONCLUIDO) {
            agendamento.setAtivo(false);
        }

        return agendamentoRepository.save(agendamento);
    }

    /**
     * Busca todos os agendamentos de um paciente
     */
    public List<Agendamento> buscarPorPaciente(Long pacienteId) {
        Usuario paciente = usuarioService.buscarPorId(pacienteId);
        return agendamentoRepository.findByPaciente(paciente);
    }

    /**
     * Busca todos os agendamentos ativos de um paciente
     */
    public List<Agendamento> buscarAtivosPorPaciente(Long pacienteId) {
        Usuario paciente = usuarioService.buscarPorId(pacienteId);
        return agendamentoRepository.findByPacienteAndAtivo(paciente, true);
    }

    /**
     * Busca todos os agendamentos agendados de um paciente
     */
    public List<Agendamento> buscarAgendadosPorPaciente(Long pacienteId) {
        Usuario paciente = usuarioService.buscarPorId(pacienteId);
        return agendamentoRepository.findByPacienteAndStatus(paciente, Agendamento.StatusAgendamento.AGENDADO);
    }

    /**
     * Busca todos os agendamentos ativos e agendados de um paciente
     */
    public List<Agendamento> buscarAtivosEAgendadosPorPaciente(Long pacienteId) {
        try {
            System.out.println("[AgendamentoService] Buscando usuário com ID: " + pacienteId);
            Usuario paciente = usuarioService.buscarPorId(pacienteId);
            System.out.println("[AgendamentoService] Usuário encontrado: " + paciente.getEmail() + " (ID: " + paciente.getId() + ")");

            System.out.println("[AgendamentoService] Buscando agendamentos ativos e agendados para o paciente: " + paciente.getEmail());
            List<Agendamento> agendamentos = agendamentoRepository.findByPacienteAndStatusAndAtivo(paciente, Agendamento.StatusAgendamento.AGENDADO, true);
            System.out.println("[AgendamentoService] Encontrados " + agendamentos.size() + " agendamentos ativos e agendados");

            return agendamentos;
        } catch (Exception e) {
            System.err.println("[AgendamentoService] Erro ao buscar agendamentos ativos e agendados para o paciente com ID: " + pacienteId);
            e.printStackTrace();
            throw e;
        }
    }

    /**
     * Busca todos os agendamentos de uma série
     */
    public List<Agendamento> buscarPorSerieId(UUID serieId) {
        return agendamentoRepository.findBySerieId(serieId);
    }

    /**
     * Busca todos os agendamentos ativos de uma série
     */
    public List<Agendamento> buscarAtivosPorSerieId(UUID serieId) {
        return agendamentoRepository.findBySerieIdAndAtivo(serieId, true);
    }

    /**
     * Busca todos os agendamentos ativos e agendados de uma série
     */
    public List<Agendamento> buscarAtivosEAgendadosPorSerieId(UUID serieId) {
        return agendamentoRepository.findBySerieIdAndStatusAndAtivo(serieId, Agendamento.StatusAgendamento.AGENDADO, true);
    }

    /**
     * Atualiza o dia e horário de todos os agendamentos ativos e agendados de uma série
     * @param serieId ID da série de agendamentos
     * @param diaSemana Novo dia da semana
     * @param horaInicio Nova hora de início
     * @param horaFim Nova hora de fim
     * @return Lista de agendamentos atualizados
     */
    @Transactional
    public List<Agendamento> atualizarDiaEHorarioDaSerie(UUID serieId, DayOfWeek diaSemana, LocalTime horaInicio, LocalTime horaFim) {
        List<Agendamento> agendamentos = buscarAtivosEAgendadosPorSerieId(serieId);
        List<Agendamento> agendamentosAtualizados = new ArrayList<>();

        for (Agendamento agendamento : agendamentos) {
            // Calcula a nova data mantendo o mesmo intervalo de semanas
            LocalDate novaData = agendamento.getData();
            while (novaData.getDayOfWeek() != diaSemana) {
                novaData = novaData.plusDays(1);
            }

            // Verifica se o horário está disponível
            if (!isHorarioDisponivel(agendamento.getPsicologo().getId(), novaData, horaInicio, horaFim)) {
                throw new RuntimeException("Horário não disponível para agendamento: " + novaData + " " + horaInicio + "-" + horaFim);
            }

            // Atualiza o agendamento
            agendamento.setData(novaData);
            agendamento.setHoraInicio(horaInicio);
            agendamento.setHoraFim(horaFim);

            agendamentosAtualizados.add(agendamentoRepository.save(agendamento));
        }

        return agendamentosAtualizados;
    }
}
