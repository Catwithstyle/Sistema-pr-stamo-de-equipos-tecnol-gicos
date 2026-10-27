package edu.uam.tecnouam.prestamos.repository;


import edu.uam.tecnouam.prestamos.model.EstadoPrestamo;
import edu.uam.tecnouam.prestamos.model.Prestamo;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;


import java.util.List;
import java.util.Optional;


public interface PrestamoRepository extends JpaRepository<Prestamo, Long> {


    boolean existsByEquipoIdAndEstado(Long equipoId, EstadoPrestamo estado);


    @EntityGraph(attributePaths = "equipo")
    List<Prestamo> findAllByOrderByIdAsc();


    @EntityGraph(attributePaths = "equipo")
    Optional<Prestamo> findWithEquipoById(Long id);
}
