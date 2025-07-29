package cuidar.mais.api.dto;

import cuidar.mais.api.models.Sessao;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SessaoDTO {

    private Long id;
    private Long pacienteId;
    private String nomePaciente;
    private Long psicologoId;
    private Long horarioDisponivelId;
    private Integer numeroSessao;
    private LocalDate dataSessao;
    private LocalTime horaInicio;
    private LocalTime horaFim;
    private Sessao.StatusSessao status;
    private String observacoes;
    private Boolean ativo;
    private LocalDateTime dataCriacao;
    private LocalDateTime dataAtualizacao;

    // Campos auxiliares para exibição
    private String horarioFormatado;
    private String statusFormatado;

    // Novos campos adicionados
    private String pacienteEmail;
    private String pacienteTelefone;
    private String pacienteSobrenome;
    private String pacienteImagem;

    // Métodos auxiliares
    public String getHorarioFormatado() {
        if (horaInicio != null && horaFim != null) {
            return horaInicio.toString() + " - " + horaFim.toString();
        }
        return "";
    }

    public String getStatusFormatado() {
        if (status == null) return "";
        
        return switch (status) {
            case AGENDADA -> "Agendada";
            case REALIZADA -> "Realizada";
            case CANCELADA -> "Cancelada";
            case FALTOU -> "Faltou";
        };
    }
}
