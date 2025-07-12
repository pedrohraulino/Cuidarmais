package cuidar.mais.api.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ConfiguracaoAgendaDTO {
    private Long id;
    private Long psicologoId;
    private DayOfWeek diaSemana;
    private Boolean ativo;
    private LocalTime horarioInicio;
    private Integer intervaloMinutos;
    private LocalTime horarioFim;
    private LocalTime inicioPausa;
    private LocalTime fimPausa;
    private LocalDateTime dataAtualizacao;

    // Campos adicionais para facilitar a exibição no frontend
    private String nomeDiaSemana; // Ex: "Segunda-feira"

    // Construtor que recebe apenas os campos obrigatórios
    public ConfiguracaoAgendaDTO(Long psicologoId, DayOfWeek diaSemana, Boolean ativo, 
                                LocalTime horarioInicio, Integer intervaloMinutos, 
                                LocalTime horarioFim) {
        this.psicologoId = psicologoId;
        this.diaSemana = diaSemana;
        this.ativo = ativo;
        this.horarioInicio = horarioInicio;
        this.intervaloMinutos = intervaloMinutos;
        this.horarioFim = horarioFim;
    }

    // Método para converter o DayOfWeek para nome em português
    public String getNomeDiaSemana() {
        if (diaSemana == null) {
            return null;
        }

        switch (diaSemana) {
            case MONDAY: return "Segunda-feira";
            case TUESDAY: return "Terça-feira";
            case WEDNESDAY: return "Quarta-feira";
            case THURSDAY: return "Quinta-feira";
            case FRIDAY: return "Sexta-feira";
            case SATURDAY: return "Sábado";
            case SUNDAY: return "Domingo";
            default: return diaSemana.toString();
        }
    }
}
