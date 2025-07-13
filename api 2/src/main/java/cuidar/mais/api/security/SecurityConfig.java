package cuidar.mais.api.security;

import com.fasterxml.jackson.core.filter.TokenFilter;
import cuidar.mais.api.repository.UsuarioRepository;
import cuidar.mais.api.service.AutenticacaoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@EnableWebSecurity
@Configuration

public class SecurityConfig {

    @Autowired
    private AutenticacaoService autenticacaoService;

    @Autowired
    private TokenService tokenService;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        return http
                .csrf(csrf -> csrf.disable())
                .cors(Customizer.withDefaults()) // ← Habilita CORS via bean abaixo
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/auth/**").permitAll()
                        .requestMatchers("/api/pacientes/*/agendamentos/**").permitAll() // Allow all patient appointment endpoints without authentication
                        .requestMatchers("/api/pacientes/*/horario-atual").permitAll() // Allow access to the horario-atual endpoint without authentication
                        .requestMatchers("/api/pacientes/*/inativar").permitAll() // Allow access to the inativar endpoint without authentication
                        .requestMatchers("/api/pacientes/*/reativar").permitAll() // Allow access to the reativar endpoint without authentication
                        .requestMatchers("/api/pacientes/*/criar-sessoes-adicionais").permitAll() // Allow access to the criar-sessoes-adicionais endpoint without authentication
                        .anyRequest().authenticated()
                )
                .addFilterBefore(new JwtAuthenticationFilter(tokenService, usuarioRepository),
                        UsernamePasswordAuthenticationFilter.class)
                .userDetailsService(autenticacaoService)
                .build();
    }
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        // Permitir origens específicas
        config.setAllowedOrigins(List.of("http://localhost:4200", "http://localhost:3000"));
        // Alternativa: permitir todas as origens (descomente se necessário)
        // config.addAllowedOrigin("*");
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("Authorization", "Content-Type", "Accept", "X-Requested-With", "*"));
        config.setExposedHeaders(List.of("Authorization"));
        config.setAllowCredentials(true);
        config.setMaxAge(3600L); // 1 hora de cache para pre-flight requests

        System.out.println("CORS Configuration:");
        System.out.println("Allowed Origins: " + config.getAllowedOrigins());
        System.out.println("Allowed Methods: " + config.getAllowedMethods());
        System.out.println("Allowed Headers: " + config.getAllowedHeaders());
        System.out.println("Exposed Headers: " + config.getExposedHeaders());
        System.out.println("Allow Credentials: " + config.getAllowCredentials());

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
