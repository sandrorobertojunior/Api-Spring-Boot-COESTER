package com.server.coester.configs;
import com.server.coester.services.UsuarioService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.authority.mapping.GrantedAuthoritiesMapper;
import org.springframework.security.core.authority.mapping.NullAuthoritiesMapper;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;


@Configuration
@EnableMethodSecurity
public class SecurityConfiguration {

    // Bean para criptografia de senha
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(10);
    }

    // Assumindo que você tem esses beans em outro lugar
    // private final UsuarioService usuarioService;
    // private final PasswordEncoder passwordEncoder;

    // ... Construtor ou Autowiring para os serviços

    @Bean
    public GrantedAuthoritiesMapper grantedAuthoritiesMapper() {
        // ESSENCIAL: Diz ao Spring Security para não adicionar ou esperar o prefixo ROLE_
        return new NullAuthoritiesMapper();
    }

    // 1. Definição do Bean de Configuração CORS
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();

        // Permite o seu frontend React/Next.js
        configuration.setAllowedOrigins(List.of("http://localhost:3000","https://api-coester.sandroroberto.uk","https://sistema-de-medicao.sandroroberto.uk"));

        // Métodos HTTP padrão que serão permitidos (GET, POST, PUT, DELETE, etc.)
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"));

        // Cabeçalhos que serão permitidos na requisição
        configuration.setAllowedHeaders(List.of("*"));

        // Importante para enviar credenciais, cookies e Authorization headers
        configuration.setAllowCredentials(true);

        // Define a configuração CORS para todas as rotas (/**)
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http, UserDetailsService userDetailsService) throws Exception {
        http
                // Aplicação da configuração CORS
                .cors(cors -> cors.configurationSource(corsConfigurationSource())) // Adicionado CORS
                .authorizeHttpRequests(authz -> authz
                        .requestMatchers("/auth/**").permitAll()
                        .anyRequest().authenticated()
                )
                // Passe o userDetailsService no lugar do UsuarioService
                .addFilterBefore(new CustomAuthFilter((UsuarioService) userDetailsService, passwordEncoder()),
                        UsernamePasswordAuthenticationFilter.class)
                .csrf(csrf -> csrf.disable());

        return http.build();
    }


}