package cuidar.mais.api.models;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Entity
@Table(name = "sessao")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Sessao {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "paciente_id", nullable = false,
                foreignKey = @ForeignKey(name = "fk_sessao_paciente"))
    private Paciente paciente;

    @ManyToOne
    @JoinColumn(name = "psicologo_id", nullable = false,
                foreignKey = @ForeignKey(name = "fk_sessao_psicologo"))
    private Usuario psicologo;

    @ManyToOne
    @JoinColumn(name = "horario_disponivel_id", nullable = false,
                foreignKey = @ForeignKey(name = "fk_sessao_horario"))
    private HorarioDisponivel horarioDisponivel;

    @Column(name = "numero_sessao", nullable = false)
    private Integer numeroSessao;

    @Column(name = "data_sessao", nullable = false)
    private LocalDate dataSessao;

    @Column(name = "hora_inicio", nullable = false)
    private LocalTime horaInicio;

    @Column(name = "hora_fim", nullable = false)
    private LocalTime horaFim;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private StatusSessao status = StatusSessao.AGENDADA;

    @Column(name = "observacoes", columnDefinition = "TEXT")
    private String observacoes;

    @Column(name = "ativo", nullable = false)
    private Boolean ativo = true;

    @Column(name = "data_criacao", nullable = false)
    private LocalDateTime dataCriacao = LocalDateTime.now();

    @Column(name = "data_atualizacao")
    private LocalDateTime dataAtualizacao;

    @PreUpdate
    public void preUpdate() {
        this.dataAtualizacao = LocalDateTime.now();
    }

    // Enum para status da sessão
    public enum StatusSessao {
        AGENDADA,
        REALIZADA,
        CANCELADA,
        FALTOU
    }

    // Métodos auxiliares
    public boolean isPodeRemarcar() {
        return status == StatusSessao.AGENDADA && dataSessao.isAfter(LocalDate.now());
    }

    public boolean isPodeCancelar() {
        return status == StatusSessao.AGENDADA && dataSessao.isAfter(LocalDate.now());
    }
}
