package edu.uam.tecnouam.prestamos.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record PrestamoRequest(
        @NotNull(message = "El equipo es obligatorio")
        Long equipoId,

        @NotBlank(message = "El nombre del solicitante es obligatorio")
        @Size(max = 100, message = "El solicitante no puede superar 100 caracteres")
        String solicitante,

        @NotNull(message = "La fecha de prestamo es obligatoria")
        LocalDate fechaPrestamo,

        @NotNull(message = "La fecha prevista de devolucion es obligatoria")
        LocalDate fechaPrevistaDevolucion
) {
}
