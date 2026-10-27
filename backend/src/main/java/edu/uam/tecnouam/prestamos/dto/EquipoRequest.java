package edu.uam.tecnouam.prestamos.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Datos para crear o modificar un equipo. El estado no se recibe: lo controlan
 * exclusivamente los prestamos y devoluciones.
 */
public record EquipoRequest(
        @NotBlank(message = "El codigo patrimonial es obligatorio")
        @Size(max = 50, message = "El codigo patrimonial no puede superar 50 caracteres")
        String codigoPatrimonial,

        @NotBlank(message = "El nombre es obligatorio")
        @Size(max = 100, message = "El nombre no puede superar 100 caracteres")
        String nombre,

        @NotBlank(message = "El tipo es obligatorio")
        @Size(max = 50, message = "El tipo no puede superar 50 caracteres")
        String tipo
) {
}
