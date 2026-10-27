package edu.uam.tecnouam.prestamos.excepcion;

public class SolicitudInvalidaException extends RuntimeException {
    public SolicitudInvalidaException(String mensaje) {
        super(mensaje);
    }
}

