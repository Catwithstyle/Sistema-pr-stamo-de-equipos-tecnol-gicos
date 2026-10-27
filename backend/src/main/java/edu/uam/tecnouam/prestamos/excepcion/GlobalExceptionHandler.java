package edu.uam.tecnouam.prestamos.excepcion;


import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;


import java.util.List;

@RestControllerAdvice
public class GlobalExceptionHandler {


    @ExceptionHandler(RecursoNoEncontradoException.class)
    public ResponseEntity<ErrorRespuesta> noEncontrado(RecursoNoEncontradoException ex) {
        return construir(HttpStatus.NOT_FOUND, ex.getMessage(), List.of());
    }


    @ExceptionHandler(ConflictoNegocioException.class)
    public ResponseEntity<ErrorRespuesta> conflicto(ConflictoNegocioException ex) {
        return construir(HttpStatus.CONFLICT, ex.getMessage(), List.of());
    }


    @ExceptionHandler(SolicitudInvalidaException.class)
    public ResponseEntity<ErrorRespuesta> solicitudInvalida(SolicitudInvalidaException ex) {
        return construir(HttpStatus.BAD_REQUEST, ex.getMessage(), List.of());
    }


    /** Fallo de las anotaciones @Valid sobre el cuerpo (campos obligatorios, etc.). */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorRespuesta> validacion(MethodArgumentNotValidException ex) {
        List<String> detalles = ex.getBindingResult().getFieldErrors().stream()
                .map(f -> f.getField() + ": " + f.getDefaultMessage())
                .sorted()
                .toList();
        return construir(HttpStatus.BAD_REQUEST, "Datos de entrada invalidos", detalles);
    }


    /** JSON mal formado o fechas que no cumplen el formato yyyy-MM-dd. */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorRespuesta> cuerpoIlegible(HttpMessageNotReadableException ex) {
        return construir(HttpStatus.BAD_REQUEST,
                "El cuerpo de la peticion es invalido. Verifique el JSON y que las fechas usen el formato yyyy-MM-dd",
                List.of());
    }


    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorRespuesta> tipoIncorrecto(MethodArgumentTypeMismatchException ex) {
        return construir(HttpStatus.BAD_REQUEST, "El parametro '" + ex.getName() + "' tiene un valor invalido", List.of());
    }



    /** Respaldo ante condiciones de carrera sobre restricciones unicas (codigo patrimonial). */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErrorRespuesta> integridad(DataIntegrityViolationException ex) {
        return construir(HttpStatus.CONFLICT, "La operacion viola una restriccion de integridad de datos", List.of());
    }


    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ErrorRespuesta> credenciales(BadCredentialsException ex) {
        return construir(HttpStatus.UNAUTHORIZED, "Credenciales incorrectas", List.of());
    }


    private ResponseEntity<ErrorRespuesta> construir(HttpStatus status, String mensaje, List<String> detalles) {
        return ResponseEntity.status(status)
                .body(ErrorRespuesta.de(status.value(), status.getReasonPhrase(), mensaje, detalles));
    }
}
