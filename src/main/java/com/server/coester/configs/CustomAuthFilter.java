package com.server.coester.configs;
import com.server.coester.services.UsuarioService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.filter.OncePerRequestFilter;
import java.io.IOException;
import java.util.Collections;

//s
public class CustomAuthFilter extends OncePerRequestFilter {

    private final UsuarioService usuarioService;
    private final PasswordEncoder passwordEncoder;

    public CustomAuthFilter(UsuarioService usuarioService, PasswordEncoder passwordEncoder) {
        this.usuarioService = usuarioService;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        String authHeader = request.getHeader("Authorization");

        if (authHeader != null && authHeader.startsWith("Basic ")) {
            try {
                // Extrai o token (remove "Basic ")
                String base64Credentials = authHeader.substring(6);

                // Decodifica Base64
                byte[] decodedBytes = java.util.Base64.getDecoder().decode(base64Credentials);
                String credentials = new String(decodedBytes);

                // Separa email e senha
                String[] parts = credentials.split(":", 2);
                if (parts.length == 2) {
                    String email = parts[0];
                    String password = parts[1];

                    // Usa o mesmo método do seu login para validar
                    boolean isValid = usuarioService.login(email, password).isPresent();

                    if (isValid) {
                        // Cria a autenticação no Spring Security
                        UsernamePasswordAuthenticationToken authentication =
                                new UsernamePasswordAuthenticationToken(
                                        email,
                                        null,
                                        Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER"))
                                );

                        SecurityContextHolder.getContext().setAuthentication(authentication);
                    } else {
                        // Credenciais inválidas - não define autenticação
                        System.out.println("Credenciais inválidas para: " + email);
                    }
                }
            } catch (Exception e) {
                System.out.println("Erro ao processar token: " + e.getMessage());
            }
        }

        filterChain.doFilter(request, response);
    }
}
