package cuidar.mais.api.dto;

import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
public class PsicologoDTO {
    private Long id;
    private String nome;
    private String email;
    private String crp;
    private String imagemDataUrl;
    private List<String> perfis;

    public PsicologoDTO(Long id, String nome, String email, List<String> perfis) {
        this.id = id;
        this.nome = nome;
        this.email = email;
        this.perfis = perfis;
    }

    public PsicologoDTO(Long id, String nome, String email, String crp, String imagemDataUrl, List<String> perfis) {
        this.id = id;
        this.nome = nome;
        this.email = email;
        this.crp = crp;
        this.imagemDataUrl = imagemDataUrl;
        this.perfis = perfis;
    }
}
