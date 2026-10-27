package edu.uam.tecnouam.prestamos.excepcion;

import java.time.Instant;
import java.util.List;


public record ErrorRespuesta(
        Instant timestamp,
        int status,
        String error,
        String mensaje,
        List<String> detalles
) {
    public static ErrorRespuesta de(int status, String error, String mensaje) {
        return new ErrorRespuesta(Instant.now(), status, error, mensaje, List.of());
    }


    public static ErrorRespuesta de(int status, String error, String mensaje, List<String> detalles) {
        return new ErrorRespuesta(Instant.now(), status, error, mensaje, detalles);
    }
}
