package com.server.coester.repositories;

import com.server.coester.dtos.UsuarioDto;
import com.server.coester.dtos.UsuarioDtoResume;
import com.server.coester.entities.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface UsuarioRepository extends JpaRepository<Usuario, Long> {

    // Buscar por email (para login)
    Optional<Usuario> findByEmail(String email);

    // Buscar por username
    Optional<Usuario> findByUsername(String username);

    // Verificar se email existe
    boolean existsByEmail(String email);

    // Verificar se username existe
    boolean existsByUsername(String username);

    // Buscar usuários por role (usando PostgreSQL array)
    @Query(value = "SELECT * FROM usuarios WHERE :role = ANY(array_roles)", nativeQuery = true)
    List<Usuario> findByRole(@Param("role") String role);

    // Buscar usuários que tenham qualquer uma das roles
    @Query(value = "SELECT * FROM usuarios WHERE array_roles && ARRAY[:roles]", nativeQuery = true)
    List<Usuario> findByAnyRole(@Param("roles") List<String> roles);
    List<UsuarioDto> findAllByOrderByUsernameAsc();
}