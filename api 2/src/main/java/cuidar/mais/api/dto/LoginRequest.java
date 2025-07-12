package cuidar.mais.api.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
public class LoginRequest {
    @JsonAlias({"username", "login", "user"})
    private String email;

    @JsonAlias({"password", "pass"})
    private String senha;
}
