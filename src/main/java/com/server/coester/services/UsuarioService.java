package com.server.coester.services;


import com.server.coester.dtos.UsuarioDto;
import com.server.coester.entities.Usuario;
import com.server.coester.repositories.UsuarioRepository;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class UsuarioService implements UserDetailsService {

    private final UsuarioRepository usuarioRepository;
    private final BCryptPasswordEncoder passwordEncoder;

    public UsuarioService(UsuarioRepository usuarioRepository) {
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = new BCryptPasswordEncoder();
    }

    public Usuario register(Usuario usuario) {

        // Verifica se já existe email
        if (usuarioRepository.findByEmail(usuario.getEmail()).isPresent()) {
            throw new RuntimeException("Email já existe");
        }

        // Criptografa a senha e salva
        usuario.setPassword(passwordEncoder.encode(usuario.getPassword()));
        return usuarioRepository.save(usuario);
    }

    public Optional<Usuario> login(String email, String senha) {
        Optional<Usuario> usuarioOpt = usuarioRepository.findByEmail(email);
        if (usuarioOpt.isPresent()) {
            Usuario usuario = usuarioOpt.get();
            if (passwordEncoder.matches(senha, usuario.getPassword())) {
                return Optional.of(usuario);
            }
        }
        return Optional.empty();
    }

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        // 1. Busca o usuário pelo email
        Usuario usuario = usuarioRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("Usuário não encontrado com email: " + email));

        // 2. Mapeia as roles (strings) para GrantedAuthority
        Collection<? extends GrantedAuthority> authorities = usuario.getArrayRoles().stream()
                // CRÍTICO: Converte cada string de role em SimpleGrantedAuthority
                .map(SimpleGrantedAuthority::new)
                .collect(Collectors.toList());

        // 3. Retorna o objeto UserDetails padrão do Spring Security
        return new User(
                usuario.getEmail(),
                usuario.getPassword(), // A senha já está hasheada
                authorities
        );
    }

    public List<UsuarioDto> getAllUsuarios(){
        List<UsuarioDto> usuarios = usuarioRepository.findAllByOrderByUsernameAsc();
        return usuarios;
    }
    public Usuario getUsuarioAutenticado() {
        // 1. Obtém o email/username do contexto de segurança
        String email = SecurityContextHolder.getContext().getAuthentication().getName();

        // 2. Busca o objeto Usuario completo no banco de dados
        return usuarioRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Usuário autenticado não encontrado no DB"));
    }
}

