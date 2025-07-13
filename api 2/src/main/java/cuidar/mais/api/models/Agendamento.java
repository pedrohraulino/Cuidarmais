package cuidar.mais.api.models;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

@Entity
@Table(name = "agendamento")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Agendamento {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "psicologo_id", nullable = false,
                foreignKey = @ForeignKey(name = "fk_agendamento_psicologo"))
    private Usuario psicologo;

    @ManyToOne
    @JoinColumn(name = "paciente_id", nullable = false, 
                foreignKey = @ForeignKey(name = "fk81wc065s6yhapkxh4lwbfsr0m"))
    private Usuario paciente;

    @Column(name = "serie_id")
    private UUID serieId;

    @Column(name = "numero_sessao")
    private Integer numeroSessao;

    @Column(name = "data", nullable = false)
    private LocalDate data;

    @Column(name = "hora_inicio", nullable = false)
    private LocalTime horaInicio;

    @Column(name = "hora_fim", nullable = false)
    private LocalTime horaFim;

    @Column(name = "status", nullable = false)
    @Enumerated(EnumType.STRING)
    private StatusAgendamento status = StatusAgendamento.AGENDADO;

    @Column(name = "ativo", nullable = false)
    private Boolean ativo = true;

    // Enum para status do agendamento
    public enum StatusAgendamento {
        AGENDADO,
        CANCELADO,
        CONCLUIDO
    }
}
