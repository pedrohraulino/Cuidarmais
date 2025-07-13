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

        logger.info("Processing request to: {}", request.getRequestURI());
        String token = recuperarToken(request);
        if (token != null) {
            try {
                // Primeiro verifica se o token é válido
                boolean isValid = tokenService.isTokenValido(token);
                logger.info("Token validation result for {}: {}", request.getRequestURI(), isValid);

                if (isValid) {
                    Long idUsuario = tokenService.getIdUsuario(token);
                    logger.info("User ID extracted from token: {}", idUsuario);

                    Usuario usuario = usuarioRepository.findById(idUsuario).orElse(null);
                    if (usuario != null) {
                        logger.info("User found in database: {} (ID: {})", usuario.getEmail(), usuario.getId());

                        UsernamePasswordAuthenticationToken auth =
                                new UsernamePasswordAuthenticationToken(usuario, null, usuario.getAuthorities());
                        SecurityContextHolder.getContext().setAuthentication(auth);
                        logger.info("User authenticated via token: {} with authorities: {}", 
                                   usuario.getEmail(), usuario.getAuthorities());
                    } else {
                        logger.warn("User not found for ID: {}", idUsuario);
                    }
                } else {
                    logger.warn("Token invalid or expired for request: {}", request.getRequestURI());
                }
            } catch (ExpiredJwtException e) {
                logger.warn("Token expired for request {}: {}", request.getRequestURI(), e.getMessage());
            } catch (MalformedJwtException | SignatureException e) {
                logger.warn("Invalid token for request {}: {}", request.getRequestURI(), e.getMessage());
            } catch (JwtException e) {
                logger.error("Error processing token for request {}: {}", request.getRequestURI(), e.getMessage());
            } catch (Exception e) {
                logger.error("Unexpected error processing token for request {}: {}", 
                            request.getRequestURI(), e.getMessage(), e);
            }
        } else {
            logger.info("No token provided for request: {}", request.getRequestURI());
        }

        filterChain.doFilter(request, response);
    }

    private String recuperarToken(HttpServletRequest request) {
        String bearer = request.getHeader("Authorization");
        logger.info("Authorization header: {}", bearer);
        if (bearer != null && bearer.startsWith("Bearer ")) {
            String token = bearer.substring(7);
            logger.info("Token extracted: {}", token.substring(0, Math.min(10, token.length())) + "...");
            return token;
        }
        logger.warn("No token found in request to: {}", request.getRequestURI());
        return null;
    }
}
