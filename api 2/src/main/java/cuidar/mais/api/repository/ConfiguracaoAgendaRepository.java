package cuidar.mais.api.repository;

import cuidar.mais.api.models.ConfiguracaoAgenda;
import cuidar.mais.api.models.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.DayOfWeek;
import java.util.List;
import java.util.Optional;

@Repository
public interface ConfiguracaoAgendaRepository extends JpaRepository<ConfiguracaoAgenda, Long> {
    
    /**
     * Busca todas as configurações de agenda de um psicólogo
     */
    List<ConfiguracaoAgenda> findByPsicologo(Usuario psicologo);
    
    /**
     * Busca todas as configurações de agenda ativas de um psicólogo
     */
    List<ConfiguracaoAgenda> findByPsicologoAndAtivoTrue(Usuario psicologo);
    
    /**
     * Busca a configuração de agenda de um psicólogo para um dia específico
     */
    Optional<ConfiguracaoAgenda> findByPsicologoAndDiaSemana(Usuario psicologo, DayOfWeek diaSemana);
}