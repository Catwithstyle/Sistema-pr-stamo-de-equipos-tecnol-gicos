package edu.uam.tecnouam.prestamos.service;

import edu.uam.tecnouam.prestamos.dto.PrestamoRequest;
import edu.uam.tecnouam.prestamos.dto.PrestamoResponse;
import edu.uam.tecnouam.prestamos.exception.ConflictoNegocioException;
import edu.uam.tecnouam.prestamos.exception.RecursoNoEncontradoException;
import edu.uam.tecnouam.prestamos.exception.SolicitudInvalidaException;
import edu.uam.tecnouam.prestamos.model.Equipo;
import edu.uam.tecnouam.prestamos.model.EstadoEquipo;
import edu.uam.tecnouam.prestamos.model.EstadoPrestamo;
import edu.uam.tecnouam.prestamos.model.Prestamo;
import edu.uam.tecnouam.prestamos.repository.EquipoRepository;
import edu.uam.tecnouam.prestamos.repository.PrestamoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
public class PrestamoService {

    private final PrestamoRepository prestamoRepository;
    private final EquipoRepository equipoRepository;

    public PrestamoService(PrestamoRepository prestamoRepository, EquipoRepository equipoRepository) {
        this.prestamoRepository = prestamoRepository;
        this.equipoRepository = equipoRepository;
    }

    @Transactional(readOnly = true)
    public List<PrestamoResponse> listar() {
        return prestamoRepository.findAllByOrderByIdAsc().stream().map(PrestamoResponse::desde).toList();
    }

    @Transactional(readOnly = true)
    public PrestamoResponse obtener(Long id) {
        return PrestamoResponse.desde(buscar(id));
    }

    /**
     * Registra un prestamo ACTIVO y pasa el equipo a PRESTADO, en una sola transaccion.
     * Se bloquea la fila del equipo para que dos peticiones simultaneas no presten el mismo equipo.
     */
    @Transactional
    public PrestamoResponse registrar(PrestamoRequest req) {
        if (req.fechaPrevistaDevolucion().isBefore(req.fechaPrestamo())) {
            throw new SolicitudInvalidaException(
                    "La fecha prevista de devolucion no puede ser anterior a la fecha del prestamo");
        }

        Equipo equipo = equipoRepository.findByIdForUpdate(req.equipoId())
                .orElseThrow(() -> new RecursoNoEncontradoException("No existe el equipo con id " + req.equipoId()));

        if (equipo.getEstado() == EstadoEquipo.PRESTADO
                || prestamoRepository.existsByEquipoIdAndEstado(equipo.getId(), EstadoPrestamo.ACTIVO)) {
            throw new ConflictoNegocioException(
                    "El equipo '" + equipo.getCodigoPatrimonial() + "' ya se encuentra PRESTADO");
        }

        Prestamo prestamo = new Prestamo();
        prestamo.setEquipo(equipo);
        prestamo.setSolicitante(req.solicitante().trim());
        prestamo.setFechaPrestamo(req.fechaPrestamo());
        prestamo.setFechaPrevistaDevolucion(req.fechaPrevistaDevolucion());
        prestamo.setEstado(EstadoPrestamo.ACTIVO);

        equipo.setEstado(EstadoEquipo.PRESTADO);
        equipoRepository.save(equipo);

        return PrestamoResponse.desde(prestamoRepository.save(prestamo));
    }

    /** Marca el prestamo como DEVUELTO y libera el equipo (vuelve a DISPONIBLE). */
    @Transactional
    public PrestamoResponse devolver(Long id) {
        Prestamo prestamo = buscar(id);
        if (prestamo.getEstado() == EstadoPrestamo.DEVUELTO) {
            throw new ConflictoNegocioException("El prestamo " + id + " ya fue devuelto");
        }

        Equipo equipo = equipoRepository.findByIdForUpdate(prestamo.getEquipo().getId())
                .orElseThrow(() -> new RecursoNoEncontradoException("El equipo del prestamo ya no existe"));

        prestamo.setEstado(EstadoPrestamo.DEVUELTO);
        prestamo.setFechaDevolucionReal(LocalDate.now());
        equipo.setEstado(EstadoEquipo.DISPONIBLE);

        equipoRepository.save(equipo);
        return PrestamoResponse.desde(prestamoRepository.save(prestamo));
    }

    private Prestamo buscar(Long id) {
        return prestamoRepository.findWithEquipoById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("No existe el prestamo con id " + id));
    }
}
