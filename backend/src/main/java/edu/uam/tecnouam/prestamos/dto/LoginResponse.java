package edu.uam.tecnouam.prestamos.dto;

public record LoginResponse(
        String token,
        String tipo,
        String username,
        String rol,
        long expiraEnMinutos
) {
}
