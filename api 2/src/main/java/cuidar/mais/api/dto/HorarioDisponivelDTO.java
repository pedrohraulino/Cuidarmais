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
    private Boolean disponivel; // Indica se o horário está disponível para agendamento
}