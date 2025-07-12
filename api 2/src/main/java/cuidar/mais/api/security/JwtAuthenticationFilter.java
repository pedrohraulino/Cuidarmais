package cuidar.mais.api.security;

import cuidar.mais.api.models.Usuario;
import cuidar.mais.api.repository.UsuarioRepository;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.SignatureException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

public class JwtAuthenticationFilter extends OncePerRequestFilter {
    private static final Logger logger = LoggerFactory.getLogger(JwtAuthenticationFilter.class);

    private final TokenService tokenService;
    private final UsuarioRepository usuarioRepository;

    public JwtAuthenticationFilter(TokenService tokenService, UsuarioRepository usuarioRepository) {
        this.tokenService = tokenService;
        this.usuarioRepository = usuarioRepository;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        String token = recuperarToken(request);
        if (token != null) {
            try {
                // Primeiro verifica se o token é válido
                if (tokenService.isTokenValido(token)) {
                    Long idUsuario = tokenService.getIdUsuario(token);
                    Usuario usuario = usuarioRepository.findById(idUsuario).orElse(null);

                    if (usuario != null) {
                        UsernamePasswordAuthenticationToken auth =
                                new UsernamePasswordAuthenticationToken(usuario, null, usuario.getAuthorities());
                        SecurityContextHolder.getContext().setAuthentication(auth);
                        logger.info("Usuário autenticado via token: {}", usuario.getEmail());
                    } else {
                        logger.warn("Usuário não encontrado para o ID: {}", idUsuario);
                    }
                } else {
                    logger.warn("Token inválido ou expirado");
                }
            } catch (ExpiredJwtException e) {
                logger.warn("Token expirado: {}", e.getMessage());
            } catch (MalformedJwtException | SignatureException e) {
                logger.warn("Token inválido: {}", e.getMessage());
            } catch (JwtException e) {
                logger.error("Erro ao processar token: {}", e.getMessage());
            } catch (Exception e) {
                logger.error("Erro inesperado ao processar token: {}", e.getMessage(), e);
            }
        }

        filterChain.doFilter(request, response);
    }

    private String recuperarToken(HttpServletRequest request) {
        String bearer = request.getHeader("Authorization");
        if (bearer != null && bearer.startsWith("Bearer ")) {
            return bearer.substring(7);
        }
        return null;
    }
}
