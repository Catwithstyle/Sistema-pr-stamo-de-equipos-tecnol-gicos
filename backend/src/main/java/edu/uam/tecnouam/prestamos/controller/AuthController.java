package edu.uam.tecnouam.prestamos.controller;

import edu.uam.tecnouam.prestamos.dto.LoginRequest;
import edu.uam.tecnouam.prestamos.dto.LoginResponse;
import edu.uam.tecnouam.prestamos.exception.ErrorRespuesta;
import edu.uam.tecnouam.prestamos.model.Usuario;
import edu.uam.tecnouam.prestamos.repository.UsuarioRepository;
import edu.uam.tecnouam.prestamos.security.JwtService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final UsuarioRepository usuarioRepository;
    private final JwtService jwtService;

    public AuthController(AuthenticationManager authenticationManager,
                          UsuarioRepository usuarioRepository,
                          JwtService jwtService) {
        this.authenticationManager = authenticationManager;
        this.usuarioRepository = usuarioRepository;
        this.jwtService = jwtService;
    }

    /** 200 con el JWT, 400 si faltan campos, 401 si las credenciales son incorrectas. */
    @PostMapping("/login")
    public LoginResponse login(@Valid @RequestBody LoginRequest req) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(req.username(), req.password()));

        Usuario usuario = usuarioRepository.findByUsername(req.username())
                .orElseThrow(() -> new BadCredentialsException("Credenciales incorrectas"));

        String token = jwtService.generarToken(usuario.getUsername(), usuario.getRol());
        return new LoginResponse(token, "Bearer", usuario.getUsername(), usuario.getRol().name(),
                jwtService.getExpirationMinutes());
    }

    /** Cualquier fallo de autenticacion en el login responde 401 con el mismo mensaje. */
    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ErrorRespuesta> fallo(AuthenticationException ex) {
        return ResponseEntity.status(401)
                .body(ErrorRespuesta.de(401, "Unauthorized", "Credenciales incorrectas"));
    }
}
