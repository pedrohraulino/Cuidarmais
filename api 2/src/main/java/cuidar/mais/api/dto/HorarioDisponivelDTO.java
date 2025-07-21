package cuidar.mais.api.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.DayOfWeek;
import java.time.LocalTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class HorarioDisponivelDTO {

    private Long id;
    private Long configuracaoAgendaId;
    private Long psicologoId;
    private DayOfWeek diaSemana;
    private LocalTime horaInicio;
    private LocalTime horaFim;
    private Boolean ativo;
    private Boolean disponivel;
    private Long pacienteId; // ID do paciente que ocupa o horário (se houver)
    private String nomePaciente; // Nome do paciente que ocupa o horário (se houver)

    // Métodos auxiliares
    public String getHorarioFormatado() {
        if (horaInicio != null && horaFim != null) {
            return horaInicio.toString() + " - " + horaFim.toString();
        }
        return "";
    }

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
