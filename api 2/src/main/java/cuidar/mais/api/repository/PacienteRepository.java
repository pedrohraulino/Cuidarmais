package cuidar.mais.api.repository;

import cuidar.mais.api.models.Paciente;
import cuidar.mais.api.models.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PacienteRepository extends JpaRepository<Paciente, Long> {
    
    List<Paciente> findByPsicologo(Usuario psicologo);
    
    List<Paciente> findByPsicologoAndAtivoTrue(Usuario psicologo);
    
    @Query("SELECT p FROM Paciente p WHERE p.psicologo.id = ?1 AND p.ativo = true ORDER BY p.nome, p.sobrenome")
    List<Paciente> findByPsicologoIdAndAtivoTrueOrderByNome(Long psicologoId);
    
    @Query("SELECT p FROM Paciente p WHERE p.psicologo.id = ?1 ORDER BY p.nome, p.sobrenome")
    List<Paciente> findByPsicologoIdOrderByNome(Long psicologoId);
    
    boolean existsByEmailAndPsicologo(String email, Usuario psicologo);
    
    long countByPsicologoAndAtivoTrue(Usuario psicologo);
}
