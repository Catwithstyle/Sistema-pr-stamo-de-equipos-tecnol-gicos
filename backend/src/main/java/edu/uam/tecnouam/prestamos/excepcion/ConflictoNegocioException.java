package edu.uam.tecnouam.prestamos.excepcion;


public class ConflictoNegocioException extends RuntimeException {
    public ConflictoNegocioException(String mensaje) {
        super(mensaje);
    }
}

