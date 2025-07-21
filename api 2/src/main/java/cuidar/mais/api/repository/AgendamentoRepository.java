package cuidar.mais.api.repository;

import cuidar.mais.api.models.Agendamento;
import cuidar.mais.api.models.HorarioDisponivel;
import cuidar.mais.api.models.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface AgendamentoRepository extends JpaRepository<Agendamento, Long> {

    /**
     * Busca todos os agendamentos de um psicólogo
     */
    List<Agendamento> findByPsicologo(Usuario psicologo);

    /**
     * Busca todos os agendamentos ativos de um psicólogo
     */
    List<Agendamento> findByPsicologoAndAtivo(Usuario psicologo, Boolean ativo);

    /**
     * Busca todos os agendamentos de um psicólogo em uma data específica
     */
    List<Agendamento> findByPsicologoAndData(Usuario psicologo, LocalDate data);

    /**
     * Busca todos os agendamentos ativos de um psicólogo em uma data específica
     */
    List<Agendamento> findByPsicologoAndDataAndAtivo(Usuario psicologo, LocalDate data, Boolean ativo);

    /**
     * Busca todos os agendamentos de um psicólogo em uma data e horário específicos
     */
    List<Agendamento> findByPsicologoAndDataAndHoraInicio(Usuario psicologo, LocalDate data, LocalTime horaInicio);

    /**
     * Busca todos os agendamentos ativos de um psicólogo em uma data e horário específicos
     */
    List<Agendamento> findByPsicologoAndDataAndHoraInicioAndAtivo(Usuario psicologo, LocalDate data, LocalTime horaInicio, Boolean ativo);

    /**
     * Busca todos os agendamentos de um paciente
     */
    List<Agendamento> findByPaciente(Usuario paciente);

    /**
     * Busca todos os agendamentos ativos de um paciente
     */
    List<Agendamento> findByPacienteAndAtivo(Usuario paciente, Boolean ativo);

    /**
     * Busca todos os agendamentos de um paciente com status AGENDADO
     */
    List<Agendamento> findByPacienteAndStatus(Usuario paciente, Agendamento.StatusAgendamento status);

    /**
     * Busca todos os agendamentos ativos de um paciente com status específico
     */
    List<Agendamento> findByPacienteAndStatusAndAtivo(Usuario paciente, Agendamento.StatusAgendamento status, Boolean ativo);

    /**
     * Busca todos os agendamentos de uma série
     */
    List<Agendamento> findBySerieId(UUID serieId);

    /**
     * Busca todos os agendamentos ativos de uma série
     */
    List<Agendamento> findBySerieIdAndAtivo(UUID serieId, Boolean ativo);

    /**
     * Busca todos os agendamentos ativos e agendados de uma série
     */
    List<Agendamento> findBySerieIdAndStatusAndAtivo(UUID serieId, Agendamento.StatusAgendamento status, Boolean ativo);

    /**
     * Verifica se existe algum agendamento para um psicólogo em uma data e intervalo de horário específicos
     */
    @Query("SELECT COUNT(a) > 0 FROM Agendamento a WHERE a.psicologo = ?1 AND a.data = ?2 AND " +
           "a.status = cuidar.mais.api.models.Agendamento.StatusAgendamento.AGENDADO AND " +
            "a.ativo = true AND " +
           "((a.horaInicio <= ?3 AND a.horaFim > ?3) OR " +
           "(a.horaInicio < ?4 AND a.horaFim >= ?4) OR " +
           "(a.horaInicio >= ?3 AND a.horaFim <= ?4))")
    boolean existsAgendamentoConflitante(Usuario psicologo, LocalDate data, LocalTime horaInicio, LocalTime horaFim);

    /**
     * Busca todos os agendamentos para um psicólogo em uma data e intervalo de horário específicos
     * que possam conflitar com um novo agendamento
     */
    @Query("SELECT a FROM Agendamento a WHERE a.psicologo = ?1 AND a.data = ?2 AND " +
           "a.status = cuidar.mais.api.models.Agendamento.StatusAgendamento.AGENDADO AND " +
            "a.ativo = true AND " +
           "((a.horaInicio <= ?3 AND a.horaFim > ?3) OR " +
           "(a.horaInicio < ?4 AND a.horaFim >= ?4) OR " +
           "(a.horaInicio >= ?3 AND a.horaFim <= ?4))")
    List<Agendamento> findAgendamentosConflitantes(Usuario psicologo, LocalDate data, LocalTime horaInicio, LocalTime horaFim);

    List<Agendamento> findByPacienteIdAndDataGreaterThanEqual(Long pacienteId, LocalDate data);

    List<Agendamento> findByPacienteIdAndAtivo(Long pacienteId, boolean ativo);

    List<Agendamento> findByPacienteIdAndStatus(Long pacienteId, Agendamento.StatusAgendamento status);
}
