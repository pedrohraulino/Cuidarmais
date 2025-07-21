package cuidar.mais.api.repository;

import cuidar.mais.api.models.ConfiguracaoAgenda;
import cuidar.mais.api.models.HorarioDisponivel;
import cuidar.mais.api.models.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface HorarioDisponivelRepository extends JpaRepository<HorarioDisponivel, Long> {
    
    List<HorarioDisponivel> findByPsicologoAndAtivoTrue(Usuario psicologo);
    
    List<HorarioDisponivel> findByPsicologoAndDiaSemanaAndAtivoTrue(Usuario psicologo, DayOfWeek diaSemana);
    
    List<HorarioDisponivel> findByConfiguracaoAgenda(ConfiguracaoAgenda configuracaoAgenda);
    
    @Query("SELECT h FROM HorarioDisponivel h WHERE h.psicologo = ?1 AND h.diaSemana = ?2 AND h.ativo = true ORDER BY h.horaInicio")
    List<HorarioDisponivel> findHorariosAtivosOrdenados(Usuario psicologo, DayOfWeek diaSemana);
    
    @Query("SELECT h FROM HorarioDisponivel h WHERE h.psicologo = ?1 AND h.diaSemana = ?2 AND h.pacienteId IS NULL AND h.ativo = true ORDER BY h.horaInicio")
    List<HorarioDisponivel> findHorariosDisponiveisOrdenados(Usuario psicologo, DayOfWeek diaSemana);
    
    Optional<HorarioDisponivel> findByPsicologoAndDiaSemanaAndHoraInicioAndHoraFimAndAtivoTrue(
            Usuario psicologo, DayOfWeek diaSemana, LocalTime horaInicio, LocalTime horaFim);
    
    Optional<HorarioDisponivel> findByPsicologoAndDiaSemanaAndHoraInicioAndHoraFim(
            Usuario psicologo, DayOfWeek diaSemana, LocalTime horaInicio, LocalTime horaFim);
    
    // Métodos atualizados para usar pacienteId ao invés de relacionamento FK
    Optional<HorarioDisponivel> findByPacienteId(Long pacienteId);
    
    List<HorarioDisponivel> findByPacienteIdIsNull();
    
    List<HorarioDisponivel> findByPacienteIdIsNotNull();
    
    @Modifying
    @Transactional
    @Query("UPDATE HorarioDisponivel h SET h.pacienteId = null, h.ativo = true WHERE h.pacienteId = ?1")
    void desvincularTodosHorariosDoPaciente(Long pacienteId);
    
    @Query("SELECT h FROM HorarioDisponivel h WHERE h.psicologo = ?1 AND h.pacienteId IS NULL AND h.ativo = true")
    List<HorarioDisponivel> findByPsicologoAndPacienteIsNullAndAtivoTrue(Usuario psicologo);
    
    @Modifying
    @Transactional
    void deleteByConfiguracaoAgenda(ConfiguracaoAgenda configuracaoAgenda);
    
    @Query("SELECT CASE WHEN COUNT(h) > 0 THEN true ELSE false END FROM HorarioDisponivel h WHERE h.configuracaoAgenda = ?1 AND h.pacienteId IS NOT NULL")
    boolean existsByConfiguracaoAgendaAndPacienteIsNotNull(ConfiguracaoAgenda configuracaoAgenda);
}