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
    private LocalTime horarioInicio;
    private Integer intervaloMinutos;
    private LocalTime horarioFim;
    private LocalTime inicioPausa;
    private LocalTime voltaPausa;
    private Boolean ativo;
    private LocalDateTime dataCriacao;
    private LocalDateTime dataAtualizacao;
    
    // Nome do dia da semana em português (para exibição)
    private String nomeDiaSemana;

    public String getNomeDiaSemana() {
        if (diaSemana == null) return null;
        
        return switch (diaSemana) {
            case MONDAY -> "Segunda-feira";
            case TUESDAY -> "Terça-feira";
            case WEDNESDAY -> "Quarta-feira";
            case THURSDAY -> "Quinta-feira";
            case FRIDAY -> "Sexta-feira";
            case SATURDAY -> "Sábado";
            case SUNDAY -> "Domingo";
        };
    }
}
