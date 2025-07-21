package cuidar.mais.api.dto;

import cuidar.mais.api.models.Paciente;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PacienteDTO {

    private Long id;
    private String nome;
    private String sobrenome;
    private Paciente.Sexo sexo;
    private LocalDate dataNascimento;
    private String email;
    private String telefone;
    private String imagemBase64;
    private String imagemTipo;
    private Long psicologoId;
    private Long horarioDisponivelId;
    private Integer sessoesPorPacote;
    private LocalDate dataCriacao;
    private Boolean ativo;

    // Campos auxiliares para exibição
    private String nomeCompleto;
    private String imagemDataUrl;
    private Integer sessoesRestantes;
    private Integer sessoesRealizadas;
    
    // Campos para exibição do horário (obtidos do HorarioDisponivel)
    private DayOfWeek diaSemana;
    private LocalTime horarioInicio;
    private LocalTime horarioFim;
    private String nomeDiaSemana;
    private String horarioFormatado;

    // Métodos auxiliares
    public String getNomeCompleto() {
        if (nome != null && sobrenome != null) {
            return nome + " " + sobrenome;
        }
        return nome != null ? nome : "";
    }

    public String getNomeDiaSemana() {
        if (diaSemana == null) return "Não definido";
        
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

    public String getHorarioFormatado() {
        if (horarioInicio != null && horarioFim != null) {
            return horarioInicio.toString() + " - " + horarioFim.toString();
        }
        return "Não definido";
    }

    public String getImagemDataUrl() {
        if (imagemBase64 != null && imagemTipo != null) {
            return "data:" + imagemTipo + ";base64," + imagemBase64;
        }
        return null;
    }
}
