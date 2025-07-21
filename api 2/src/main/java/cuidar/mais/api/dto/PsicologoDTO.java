package cuidar.mais.api.dto;

import java.util.List;

public class PsicologoDTO {

    private Long id;
    private String nome;
    private String email;
    private String crp;
    private String imagemDataUrl;
    private List<String> authorities;

    public PsicologoDTO() {}

    public PsicologoDTO(Long id, String nome, String email, String crp, String imagemDataUrl, List<String> authorities) {
        this.id = id;
        this.nome = nome;
        this.email = email;
        this.crp = crp;
        this.imagemDataUrl = imagemDataUrl;
        this.authorities = authorities;
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getNome() { return nome; }
    public void setNome(String nome) { this.nome = nome; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getCrp() { return crp; }
    public void setCrp(String crp) { this.crp = crp; }

    public String getImagemDataUrl() { return imagemDataUrl; }
    public void setImagemDataUrl(String imagemDataUrl) { this.imagemDataUrl = imagemDataUrl; }

    public List<String> getAuthorities() { return authorities; }
    public void setAuthorities(List<String> authorities) { this.authorities = authorities; }

    // Helper methods
    public String getNomeCompleto() {
        return nome;
    }

    public boolean hasAuthority(String authority) {
        return authorities != null && authorities.contains(authority);
    }
}
