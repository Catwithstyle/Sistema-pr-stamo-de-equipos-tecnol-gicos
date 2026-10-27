package edu.uam.tecnouam.prestamos.dto;

import edu.uam.tecnouam.prestamos.model.EstadoPrestamo;
import edu.uam.tecnouam.prestamos.model.Prestamo;

import java.time.LocalDate;

public record PrestamoResponse(
        Long id,
        Long equipoId,
        String equipoCodigo,
        String equipoNombre,
        String solicitante,
        LocalDate fechaPrestamo,
        LocalDate fechaPrevistaDevolucion,
        LocalDate fechaDevolucionReal,
        EstadoPrestamo estado
) {
    public static PrestamoResponse desde(Prestamo p) {
        return new PrestamoResponse(
                p.getId(),
                p.getEquipo().getId(),
                p.getEquipo().getCodigoPatrimonial(),
                p.getEquipo().getNombre(),
                p.getSolicitante(),
                p.getFechaPrestamo(),
                p.getFechaPrevistaDevolucion(),
                p.getFechaDevolucionReal(),
                p.getEstado());
    }
}
