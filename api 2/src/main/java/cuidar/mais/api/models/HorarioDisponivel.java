package cuidar.mais.api.models;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.DayOfWeek;
import java.time.LocalTime;

@Entity
@Table(name = "horario_disponivel")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class HorarioDisponivel {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "configuracao_agenda_id", nullable = false,
                foreignKey = @ForeignKey(name = "fk_horario_disponivel_configuracao"))
    private ConfiguracaoAgenda configuracaoAgenda;

    @ManyToOne
    @JoinColumn(name = "psicologo_id", nullable = false,
                foreignKey = @ForeignKey(name = "fk_horario_disponivel_psicologo"))
    private Usuario psicologo;

    @Enumerated(EnumType.STRING)
    @Column(name = "dia_semana", nullable = false)
    private DayOfWeek diaSemana;

    @Column(name = "hora_inicio", nullable = false)
    private LocalTime horaInicio;

    @Column(name = "hora_fim", nullable = false)
    private LocalTime horaFim;

    @Column(name = "ativo", nullable = false)
    private Boolean ativo = true;
}
