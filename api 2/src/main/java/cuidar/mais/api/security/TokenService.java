package cuidar.mais.api.security;

import cuidar.mais.api.models.Usuario;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Date;

@Service
public class TokenService {

    @Value("${jwt.secret:sua-chave-secreta-supersegura}")
    private String secret;

    @Value("${jwt.expiration:86400000}")
    private long expiration; // 1 dia em ms por padrão

    public String gerarToken(Usuario usuario) {
        if (usuario == null) {
            throw new IllegalArgumentException("Usuário não pode ser nulo");
        }

        if (usuario.getId() == null) {
            throw new IllegalArgumentException("ID do usuário não pode ser nulo");
        }

        Date agora = new Date();
        Date validade = new Date(agora.getTime() + expiration);

        try {
            return Jwts.builder()
                    .setIssuer("API Cuidar Mais")
                    .setSubject(usuario.getId().toString())
                    .setIssuedAt(agora)
                    .setExpiration(validade)
                    .signWith(SignatureAlgorithm.HS256, secret)
                    .compact();
        } catch (Exception e) {
            throw new RuntimeException("Erro ao gerar token: " + e.getMessage(), e);
        }
    }

    public Long getIdUsuario(String token) {
        Claims claims = Jwts.parser()
                .setSigningKey(secret)
                .parseClaimsJws(token)
                .getBody();

        return Long.parseLong(claims.getSubject());
    }

    public boolean isTokenValido(String token) {
        try {
            Jwts.parser().setSigningKey(secret).parseClaimsJws(token);
            return true;
        } catch (Exception e) {
            System.err.println("Error validating token: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
}
