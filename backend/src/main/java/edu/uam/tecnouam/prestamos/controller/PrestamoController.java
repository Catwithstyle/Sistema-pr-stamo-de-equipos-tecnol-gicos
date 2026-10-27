package edu.uam.tecnouam.prestamos.controller;

import edu.uam.tecnouam.prestamos.dto.PrestamoRequest;
import edu.uam.tecnouam.prestamos.dto.PrestamoResponse;
import edu.uam.tecnouam.prestamos.service.PrestamoService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/prestamos")
public class PrestamoController {

    private final PrestamoService service;

    public PrestamoController(PrestamoService service) {
        this.service = service;
    }

    /** 200 OK */
    @GetMapping
    public List<PrestamoResponse> listar() {
        return service.listar();
    }

    /** 200 OK, o 404 */
    @GetMapping("/{id}")
    public PrestamoResponse obtener(@PathVariable Long id) {
        return service.obtener(id);
    }

    /** 201 Created, 400 datos/fechas invalidos, 404 equipo inexistente, 409 equipo ya PRESTADO */
    @PostMapping
    public ResponseEntity<PrestamoResponse> registrar(@Valid @RequestBody PrestamoRequest req) {
        PrestamoResponse creado = service.registrar(req);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}").buildAndExpand(creado.id()).toUri();
        return ResponseEntity.created(location).body(creado);
    }

    /** 200 OK, 404 si no existe, 409 si ya estaba devuelto */
    @PutMapping("/{id}/devolver")
    public PrestamoResponse devolver(@PathVariable Long id) {
        return service.devolver(id);
    }
}
