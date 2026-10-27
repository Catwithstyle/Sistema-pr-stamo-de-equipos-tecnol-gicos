package edu.uam.tecnouam.prestamos.repository;


import edu.uam.tecnouam.prestamos.model.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;


import java.util.Optional;


public interface UsuarioRepository extends JpaRepository<Usuario, Long> {


    Optional<Usuario> findByUsername(String username);


    boolean existsByUsername(String username);
}
