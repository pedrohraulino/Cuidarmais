package cuidar.mais.api.repository;

import cuidar.mais.api.models.ConfiguracaoAgenda;
import cuidar.mais.api.models.HorarioDisponivel;
import cuidar.mais.api.models.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.List;

@Repository
public interface HorarioDisponivelRepository extends JpaRepository<HorarioDisponivel, Long> {
    
    List<HorarioDisponivel> findByPsicologo(Usuario psicologo);
    
    List<HorarioDisponivel> findByPsicologoAndDiaSemana(Usuario psicologo, DayOfWeek diaSemana);
    
    List<HorarioDisponivel> findByConfiguracaoAgenda(ConfiguracaoAgenda configuracaoAgenda);
    
    @Query("SELECT h FROM HorarioDisponivel h WHERE h.psicologo = ?1 AND h.diaSemana = ?2 AND h.ativo = true")
    List<HorarioDisponivel> findHorariosAtivos(Usuario psicologo, DayOfWeek diaSemana);
    
    void deleteByConfiguracaoAgenda(ConfiguracaoAgenda configuracaoAgenda);
}