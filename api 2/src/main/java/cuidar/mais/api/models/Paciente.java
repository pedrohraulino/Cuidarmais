package cuidar.mais.api.models;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Entity
@Table(name = "paciente")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Paciente {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "nome", nullable = false)
    private String nome;

    @Column(name = "sobrenome", nullable = false)
    private String sobrenome;

    @Enumerated(EnumType.STRING)
    @Column(name = "sexo", nullable = false)
    private Sexo sexo;

    @Column(name = "data_nascimento", nullable = false)
    private LocalDate dataNascimento;

    @Column(name = "email")
    private String email;

    @Column(name = "telefone")
    private String telefone;

    @Column(name = "imagem_base64", columnDefinition = "TEXT")
    private String imagemBase64;

    @Column(name = "imagem_tipo")
    private String imagemTipo;

    @ManyToOne
    @JoinColumn(name = "psicologo_id", nullable = false,
                foreignKey = @ForeignKey(name = "fk_paciente_psicologo"))
    private Usuario psicologo;

    // Campo para vincular ao horário disponível
    @Column(name = "horario_disponivel_id")
    private Long horarioDisponivelId;

    @Column(name = "sessoes_por_pacote", nullable = false)
    private Integer sessoesPorPacote;

    @Column(name = "data_criacao", nullable = false)
    private LocalDate dataCriacao = LocalDate.now();

    @Column(name = "ativo", nullable = false)
    private Boolean ativo = true;

    // Método para obter a URL de dados da imagem
    public String getImagemDataUrl() {
        if (imagemBase64 != null && imagemTipo != null) {
            return "data:" + imagemTipo + ";base64," + imagemBase64;
        }
        return null;
    }

    // Métodos auxiliares
    public String getNomeCompleto() {
        return nome + " " + sobrenome;
    }

    public boolean temHorarioDefinido() {
        return horarioDisponivelId != null;
    }

    // Enum para sexo
    public enum Sexo {
        MASCULINO,
        FEMININO,
        OUTRO
    }
}
