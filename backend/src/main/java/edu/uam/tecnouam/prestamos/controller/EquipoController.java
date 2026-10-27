package edu.uam.tecnouam.prestamos.controller;

import edu.uam.tecnouam.prestamos.dto.EquipoRequest;
import edu.uam.tecnouam.prestamos.dto.EquipoResponse;
import edu.uam.tecnouam.prestamos.service.EquipoService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/equipos")
public class EquipoController {

    private final EquipoService service;

    public EquipoController(EquipoService service) {
        this.service = service;
    }

    /** 200 OK */
    @GetMapping
    public List<EquipoResponse> listar() {
        return service.listar();
    }

    /** 200 OK, o 404 si no existe */
    @GetMapping("/{id}")
    public EquipoResponse obtener(@PathVariable Long id) {
        return service.obtener(id);
    }

    /** 201 Created (con cabecera Location), 400 si hay datos invalidos, 409 si el codigo ya existe */
    @PostMapping
    public ResponseEntity<EquipoResponse> crear(@Valid @RequestBody EquipoRequest req) {
        EquipoResponse creado = service.crear(req);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}").buildAndExpand(creado.id()).toUri();
        return ResponseEntity.created(location).body(creado);
    }

    /** 200 OK, 400, 404 o 409 */
    @PutMapping("/{id}")
    public EquipoResponse actualizar(@PathVariable Long id, @Valid @RequestBody EquipoRequest req) {
        return service.actualizar(id, req);
    }

    /** 204 No Content, 404 si no existe, 409 si tiene prestamos activos */
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void eliminar(@PathVariable Long id) {
        service.eliminar(id);
    }
}
