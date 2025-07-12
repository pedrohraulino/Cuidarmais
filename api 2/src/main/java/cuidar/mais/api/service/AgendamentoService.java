package cuidar.mais.api.service;

import cuidar.mais.api.models.Agendamento;
import cuidar.mais.api.models.Usuario;
import cuidar.mais.api.repository.AgendamentoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

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
     * Busca todos os agendamentos de um psicólogo em uma data específica
     */
    public List<Agendamento> buscarPorPsicologoEData(Long psicologoId, LocalDate data) {
        Usuario psicologo = usuarioService.buscarPorId(psicologoId);
        return agendamentoRepository.findByPsicologoAndData(psicologo, data);
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
     */
    @Transactional
    public Agendamento atualizarStatus(Long agendamentoId, Agendamento.StatusAgendamento novoStatus) {
        Agendamento agendamento = agendamentoRepository.findById(agendamentoId)
                .orElseThrow(() -> new RuntimeException("Agendamento não encontrado"));

        agendamento.setStatus(novoStatus);
        return agendamentoRepository.save(agendamento);
    }
}
