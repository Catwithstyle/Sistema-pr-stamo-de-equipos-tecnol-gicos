package edu.uam.tecnouam.prestamos.service;

import edu.uam.tecnouam.prestamos.dto.EquipoRequest;
import edu.uam.tecnouam.prestamos.dto.EquipoResponse;
import edu.uam.tecnouam.prestamos.exception.ConflictoNegocioException;
import edu.uam.tecnouam.prestamos.exception.RecursoNoEncontradoException;
import edu.uam.tecnouam.prestamos.model.Equipo;
import edu.uam.tecnouam.prestamos.model.EstadoEquipo;
import edu.uam.tecnouam.prestamos.model.EstadoPrestamo;
import edu.uam.tecnouam.prestamos.repository.EquipoRepository;
import edu.uam.tecnouam.prestamos.repository.PrestamoRepository;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class EquipoService {

    private final EquipoRepository equipoRepository;
    private final PrestamoRepository prestamoRepository;

    public EquipoService(EquipoRepository equipoRepository, PrestamoRepository prestamoRepository) {
        this.equipoRepository = equipoRepository;
        this.prestamoRepository = prestamoRepository;
    }

    @Transactional(readOnly = true)
    public List<EquipoResponse> listar() {
        return equipoRepository.findAll(Sort.by("id")).stream().map(EquipoResponse::desde).toList();
    }

    @Transactional(readOnly = true)
    public EquipoResponse obtener(Long id) {
        return EquipoResponse.desde(buscar(id));
    }

    @Transactional
    public EquipoResponse crear(EquipoRequest req) {
        String codigo = req.codigoPatrimonial().trim();
        if (equipoRepository.existsByCodigoPatrimonial(codigo)) {
            throw new ConflictoNegocioException("Ya existe un equipo con el codigo patrimonial '" + codigo + "'");
        }
        Equipo equipo = new Equipo();
        equipo.setCodigoPatrimonial(codigo);
        equipo.setNombre(req.nombre().trim());
        equipo.setTipo(req.tipo().trim());
        equipo.setEstado(EstadoEquipo.DISPONIBLE);
        return EquipoResponse.desde(equipoRepository.save(equipo));
    }

    @Transactional
    public EquipoResponse actualizar(Long id, EquipoRequest req) {
        Equipo equipo = buscar(id);
        String codigo = req.codigoPatrimonial().trim();
        if (equipoRepository.existsByCodigoPatrimonialAndIdNot(codigo, id)) {
            throw new ConflictoNegocioException("Ya existe otro equipo con el codigo patrimonial '" + codigo + "'");
        }
        equipo.setCodigoPatrimonial(codigo);
        equipo.setNombre(req.nombre().trim());
        equipo.setTipo(req.tipo().trim());
        // El estado no se modifica aqui: lo gobiernan los prestamos y devoluciones.
        return EquipoResponse.desde(equipoRepository.save(equipo));
    }

    @Transactional
    public void eliminar(Long id) {
        Equipo equipo = buscar(id);
        if (prestamoRepository.existsByEquipoIdAndEstado(id, EstadoPrestamo.ACTIVO)) {
            throw new ConflictoNegocioException(
                    "No se puede eliminar el equipo '" + equipo.getCodigoPatrimonial() + "' porque tiene un prestamo activo");
        }
        equipoRepository.delete(equipo);
    }

    private Equipo buscar(Long id) {
        return equipoRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("No existe el equipo con id " + id));
    }
}
