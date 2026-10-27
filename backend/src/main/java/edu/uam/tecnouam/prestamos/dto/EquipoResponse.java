package edu.uam.tecnouam.prestamos.dto;

import edu.uam.tecnouam.prestamos.model.Equipo;
import edu.uam.tecnouam.prestamos.model.EstadoEquipo;

public record EquipoResponse(
        Long id,
        String codigoPatrimonial,
        String nombre,
        String tipo,
        EstadoEquipo estado
) {
    public static EquipoResponse desde(Equipo e) {
        return new EquipoResponse(e.getId(), e.getCodigoPatrimonial(), e.getNombre(), e.getTipo(), e.getEstado());
    }
}
