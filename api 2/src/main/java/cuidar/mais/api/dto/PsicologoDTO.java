package cuidar.mais.api.dto;

import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PsicologoDTO {
    private Long id;
    private String nome;
    private String email;
    private List<String> perfis;
}