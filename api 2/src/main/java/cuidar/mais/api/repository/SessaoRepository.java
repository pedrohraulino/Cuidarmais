package cuidar.mais.api.repository;

import cuidar.mais.api.models.HorarioDisponivel;
import cuidar.mais.api.models.Paciente;
import cuidar.mais.api.models.Sessao;
import cuidar.mais.api.models.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@Repository
public interface SessaoRepository extends JpaRepository<Sessao, Long> {
    
    List<Sessao> findByPacienteAndAtivoTrue(Paciente paciente);
    
    // Busca todas as sessões de um paciente (ativas e inativas)
    List<Sessao> findByPaciente(Paciente paciente);
    
    // Busca todas as sessões de um paciente por ID (ativas e inativas) - alternativa mais confiável
    @Query("SELECT s FROM Sessao s WHERE s.paciente.id = ?1")
    List<Sessao> findAllByPacienteId(Long pacienteId);
    
    List<Sessao> findByPsicologoAndAtivoTrue(Usuario psicologo);
    
    List<Sessao> findByPacienteAndStatusAndAtivoTrue(Paciente paciente, Sessao.StatusSessao status);
    
    @Query("SELECT s FROM Sessao s WHERE s.horarioDisponivelId = ?1 AND s.ativo = true")
    List<Sessao> findByHorarioDisponivelIdAndAtivoTrue(Long horarioDisponivelId);
    
    @Query("SELECT s FROM Sessao s WHERE s.psicologo.id = ?1 AND s.dataSessao = ?2 AND s.ativo = true ORDER BY s.horaInicio")
    List<Sessao> findByPsicologoIdAndDataSessaoAndAtivoTrueOrderByHoraInicio(Long psicologoId, LocalDate dataSessao);
    
    @Query("SELECT s FROM Sessao s WHERE s.paciente.id = ?1 AND s.ativo = true ORDER BY s.numeroSessao")
    List<Sessao> findByPacienteIdAndAtivoTrueOrderByNumeroSessao(Long pacienteId);
    
    @Query("SELECT COUNT(s) FROM Sessao s WHERE s.paciente = ?1 AND s.status = ?2 AND s.ativo = true")
    long countByPacienteAndStatusAndAtivoTrue(Paciente paciente, Sessao.StatusSessao status);
    
    @Query("SELECT s FROM Sessao s WHERE s.paciente = ?1 AND s.dataSessao >= ?2 AND s.status = cuidar.mais.api.models.Sessao$StatusSessao.AGENDADA AND s.ativo = true ORDER BY s.dataSessao, s.horaInicio")
    List<Sessao> findSessoesFuturasPaciente(Paciente paciente, LocalDate dataAtual);
    
    @Query("SELECT s FROM Sessao s WHERE s.psicologo.id = ?1 AND s.ativo = true ORDER BY s.dataSessao, s.horaInicio")
    List<Sessao> findByPsicologoIdAndAtivoTrueOrderByDataSessao(Long psicologoId);
    
    @Query("SELECT s FROM Sessao s WHERE s.paciente.id = ?1 AND s.ativo = true ORDER BY s.dataSessao, s.horaInicio")
    List<Sessao> findByPacienteIdAndAtivoTrueOrderByDataSessao(Long pacienteId);
    
    @Query("SELECT s FROM Sessao s WHERE s.psicologo.id = ?1 AND s.dataSessao BETWEEN ?2 AND ?3 AND s.ativo = true ORDER BY s.dataSessao, s.horaInicio")
    List<Sessao> findByPsicologoIdAndDataSessaoBetweenAndAtivoTrue(Long psicologoId, LocalDate dataInicio, LocalDate dataFim);
    
    @Query("SELECT s FROM Sessao s WHERE s.psicologo.id = ?1 AND s.status = cuidar.mais.api.models.Sessao$StatusSessao.AGENDADA AND s.ativo = true ORDER BY s.dataSessao, s.horaInicio")
    List<Sessao> findByPsicologoIdAndStatusAgendadaAndAtivoTrue(Long psicologoId);
    
    @Query("SELECT s FROM Sessao s WHERE s.psicologo.id = ?1 AND s.status = cuidar.mais.api.models.Sessao$StatusSessao.REALIZADA AND s.ativo = true ORDER BY s.dataSessao DESC, s.horaInicio DESC")
    List<Sessao> findByPsicologoIdAndStatusRealizadaAndAtivoTrue(Long psicologoId);
    
    @Query("SELECT s FROM Sessao s WHERE s.psicologo.id = ?1 AND s.dataSessao = ?2 AND s.horaInicio BETWEEN ?3 AND ?4 AND s.ativo = true")
    List<Sessao> findConflictingSessions(Long psicologoId, LocalDate dataSessao, LocalTime horaInicio, LocalTime horaFim);
    
    @Query("SELECT COUNT(s) FROM Sessao s WHERE s.paciente.id = ?1 AND s.status = cuidar.mais.api.models.Sessao$StatusSessao.AGENDADA AND s.ativo = true")
    long countByPacienteIdAndStatusAgendadaAndAtivoTrue(Long pacienteId);
    
    @Query("SELECT COUNT(s) FROM Sessao s WHERE s.paciente.id = ?1 AND s.status = cuidar.mais.api.models.Sessao$StatusSessao.REALIZADA AND s.ativo = true")
    long countByPacienteIdAndStatusRealizadaAndAtivoTrue(Long pacienteId);

    // Novos métodos para gerenciamento de sessões
    @Query("SELECT COUNT(s) FROM Sessao s WHERE s.paciente.id = ?1 AND s.ativo = true")
    long countByPacienteIdAndAtivoTrue(Long pacienteId);

    @Query("SELECT s FROM Sessao s WHERE s.paciente.id = ?1 AND s.status = cuidar.mais.api.models.Sessao$StatusSessao.AGENDADA AND s.ativo = true ORDER BY s.dataSessao DESC, s.horaInicio DESC")
    List<Sessao> findByPacienteIdAndStatusAndAtivoTrueOrderByDataSessaoDesc(Long pacienteId);
}
