package cuidar.mais.api.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class TokenResponse {
    private String token;
    private String tipo = "Bearer";

    // No-args constructor required by some JSON serializers
    public TokenResponse() {
    }

    public TokenResponse(String token) {
        this.token = token;
    }
}
