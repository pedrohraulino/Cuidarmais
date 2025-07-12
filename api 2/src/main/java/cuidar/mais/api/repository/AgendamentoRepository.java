package cuidar.mais.api.repository;

import cuidar.mais.api.models.Agendamento;
import cuidar.mais.api.models.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@Repository
public interface AgendamentoRepository extends JpaRepository<Agendamento, Long> {

    /**
     * Busca todos os agendamentos de um psicólogo
     */
    List<Agendamento> findByPsicologo(Usuario psicologo);

    /**
     * Busca todos os agendamentos de um psicólogo em uma data específica
     */
    List<Agendamento> findByPsicologoAndData(Usuario psicologo, LocalDate data);

    /**
     * Busca todos os agendamentos de um psicólogo em uma data e horário específicos
     */
    List<Agendamento> findByPsicologoAndDataAndHoraInicio(Usuario psicologo, LocalDate data, LocalTime horaInicio);

    /**
     * Verifica se existe algum agendamento para um psicólogo em uma data e intervalo de horário específicos
     */
    @Query("SELECT COUNT(a) > 0 FROM Agendamento a WHERE a.psicologo = ?1 AND a.data = ?2 AND " +
           "a.status = cuidar.mais.api.models.Agendamento.StatusAgendamento.AGENDADO AND " +
           "((a.horaInicio <= ?3 AND a.horaFim > ?3) OR " +
           "(a.horaInicio < ?4 AND a.horaFim >= ?4) OR " +
           "(a.horaInicio >= ?3 AND a.horaFim <= ?4))")
    boolean existsAgendamentoConflitante(Usuario psicologo, LocalDate data, LocalTime horaInicio, LocalTime horaFim);
}
