package cuidar.mais.api.controller;

import cuidar.mais.api.dto.HorarioDisponivelDTO;
import cuidar.mais.api.service.HorarioDisponivelService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/horarios-disponiveis")
public class HorarioDisponivelController {

    @Autowired
    private HorarioDisponivelService horarioDisponivelService;

    /**
     * Busca todos os horários disponíveis de um psicólogo
     */
    @GetMapping("/psicologo/{psicologoId}")
    public ResponseEntity<List<HorarioDisponivelDTO>> buscarPorPsicologo(@PathVariable Long psicologoId) {
        List<HorarioDisponivelDTO> horarios = horarioDisponivelService.buscarPorPsicologo(psicologoId);
        return ResponseEntity.ok(horarios);
    }

    /**
     * Busca todos os horários disponíveis de um psicólogo em um dia específico
     */
    @GetMapping("/psicologo/{psicologoId}/dia-semana/{diaSemana}")
    public ResponseEntity<List<HorarioDisponivelDTO>> buscarPorPsicologoEDiaSemana(
            @PathVariable Long psicologoId,
            @PathVariable DayOfWeek diaSemana) {
        List<HorarioDisponivelDTO> horarios = horarioDisponivelService.buscarPorPsicologoEDiaSemana(psicologoId, diaSemana);
        return ResponseEntity.ok(horarios);
    }

    /**
     * Busca todos os horários disponíveis de um psicólogo em um dia específico com informação de disponibilidade
     */
    @GetMapping("/psicologo/{psicologoId}/dia-semana/{diaSemana}/data/{data}")
    public ResponseEntity<List<HorarioDisponivelDTO>> buscarHorariosDisponiveisComDisponibilidade(
            @PathVariable Long psicologoId,
            @PathVariable DayOfWeek diaSemana,
            @PathVariable LocalDate data) {
        List<HorarioDisponivelDTO> horarios = horarioDisponivelService.buscarHorariosDisponiveisComDisponibilidade(psicologoId, diaSemana, data);
        return ResponseEntity.ok(horarios);
    }
}