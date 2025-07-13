package cuidar.mais.api.repository;

import cuidar.mais.api.models.Paciente;
import cuidar.mais.api.models.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PacienteRepository extends JpaRepository<Paciente, Long> {
    
    List<Paciente> findByPsicologo(Usuario psicologo);
    
    List<Paciente> findByPsicologoAndAtivo(Usuario psicologo, Boolean ativo);
}