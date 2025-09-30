package com.server.coester.configs;
import com.server.coester.services.UsuarioService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.filter.OncePerRequestFilter;
import java.io.IOException;
import java.util.Base64;
import java.util.Collections;

// O filtro deve ser usado após a autenticação (Basic)
public class CustomAuthFilter extends OncePerRequestFilter {

    // ✅ CORREÇÃO: Usamos a interface do Spring Security
    private final UserDetailsService userDetailsService;
    private final PasswordEncoder passwordEncoder;

    // ✅ CORREÇÃO: O construtor recebe a interface
    public CustomAuthFilter(UserDetailsService userDetailsService, PasswordEncoder passwordEncoder) {
        this.userDetailsService = userDetailsService;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        String authHeader = request.getHeader("Authorization");

        // Permite que requisições sem o cabeçalho "Basic" prossigam (para o .permitAll() funcionar)
        if (authHeader == null || !authHeader.startsWith("Basic ")) {
            filterChain.doFilter(request, response);
            return;
        }

        try {
            // 1. Extrai e decodifica o Basic Auth
            String base64Credentials = authHeader.substring(6);
            String credentials = new String(Base64.getDecoder().decode(base64Credentials));

            String[] parts = credentials.split(":", 2);
            if (parts.length != 2) {
                throw new IllegalArgumentException("Formato de credenciais Basic inválido.");
            }

            String email = parts[0];
            String rawPassword = parts[1]; // Senha em formato de texto simples

            // 2. Carrega o UserDetails (usuário e senha hasheada)
            UserDetails userDetails = userDetailsService.loadUserByUsername(email);

            // 3. Verifica a senha usando o PasswordEncoder
            if (passwordEncoder.matches(rawPassword, userDetails.getPassword())) {

                // 4. Cria o objeto de autenticação
                UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(
                                userDetails, // Principal (o objeto UserDetails)
                                null,        // Credenciais (null após validação)
                                userDetails.getAuthorities() // Autoridades/Roles
                        );

                // 5. Define a autenticação no contexto de segurança
                SecurityContextHolder.getContext().setAuthentication(authentication);
                System.out.println("Usuário autenticado: " + email);
            } else {
                System.out.println("Senha inválida para: " + email);
                // O fluxo de segurança padrão lidará com a falha (geralmente 401 Unauthorized)
            }

        } catch (Exception e) {
            System.err.println("Erro durante a autenticação Basic: " + e.getMessage());
            // A exceção fará com que o request siga e seja bloqueado pelo .anyRequest().authenticated()
        }

        filterChain.doFilter(request, response);
    }
}
