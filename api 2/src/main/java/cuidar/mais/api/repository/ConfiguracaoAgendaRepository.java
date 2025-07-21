package cuidar.mais.api.repository;

import cuidar.mais.api.models.ConfiguracaoAgenda;
import cuidar.mais.api.models.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.DayOfWeek;
import java.util.List;
import java.util.Optional;

@Repository
public interface ConfiguracaoAgendaRepository extends JpaRepository<ConfiguracaoAgenda, Long> {
    
    List<ConfiguracaoAgenda> findByPsicologoAndAtivoTrue(Usuario psicologo);
    
    Optional<ConfiguracaoAgenda> findByPsicologoAndDiaSemanaAndAtivoTrue(Usuario psicologo, DayOfWeek diaSemana);
    
    @Query("SELECT c FROM ConfiguracaoAgenda c WHERE c.psicologo.id = ?1 AND c.ativo = true ORDER BY c.diaSemana")
    List<ConfiguracaoAgenda> findByPsicologoIdAndAtivoTrueOrderByDiaSemana(Long psicologoId);
    
    boolean existsByPsicologoAndDiaSemanaAndAtivoTrue(Usuario psicologo, DayOfWeek diaSemana);
}
