package cuidar.mais.api.dto;

import cuidar.mais.api.models.Paciente;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

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
    private Integer totalSessoes;
    private LocalDate dataCriacao;
    private Boolean ativo;

    // Campos para agendamento inicial ou atualização
    private DayOfWeek diaSemana;
    private LocalDate dataInicio;
    private Long horarioDisponivelId;

    // Campos para rastreamento de alterações em edição
    private DayOfWeek diaSemanaAntigo;
    private Long horarioDisponivelIdAntigo;
    private UUID serieId; // Para identificar a série de agendamentos

    // Método para obter a URL de dados da imagem
    public String getImagemDataUrl() {
        if (imagemBase64 != null && imagemTipo != null) {
            return "data:" + imagemTipo + ";base64," + imagemBase64;
        }
        return null;
    }
}
