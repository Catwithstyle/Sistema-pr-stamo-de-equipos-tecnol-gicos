package edu.uam.tecnouam.prestamos;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.time.LocalDate;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class ApiIntegrationTest {

    @Autowired
    MockMvc mvc;
    @Autowired
    ObjectMapper om;

    String adminToken;
    String userToken;

    @BeforeEach
    void login() throws Exception {
        adminToken = token("admin", "Admin123*");
        userToken = token("usuario", "User123*");
    }

    // ---------------------------------------------------------------- Seguridad

    @Test
    void sinTokenDevuelve401() throws Exception {
        mvc.perform(get("/api/equipos")).andExpect(status().isUnauthorized());
    }

    @Test
    void tokenInvalidoDevuelve401() throws Exception {
        mvc.perform(get("/api/equipos").header("Authorization", "Bearer esto.no.es.un.jwt"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void loginConCredencialesIncorrectasDevuelve401() throws Exception {
        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"admin\",\"password\":\"incorrecta\"}"))
                .andExpect(status().isUnauthorized());
        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"noexiste\",\"password\":\"x\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void loginSinCamposDevuelve400() throws Exception {
        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void userPuedeConsultarPeroNoCrear() throws Exception {
        mvc.perform(get("/api/equipos").header("Authorization", bearer(userToken))).andExpect(status().isOk());
        mvc.perform(get("/api/prestamos").header("Authorization", bearer(userToken))).andExpect(status().isOk());
        mvc.perform(post("/api/equipos").header("Authorization", bearer(userToken))
                        .contentType(MediaType.APPLICATION_JSON).content(equipoJson(codigo(), "X", "Laptop")))
                .andExpect(status().isForbidden());
    }

    @Test
    void userNoPuedeActualizarEliminarNiPrestar() throws Exception {
        long id = crearEquipo("Laptop");
        String auth = bearer(userToken);
        mvc.perform(put("/api/equipos/" + id).header("Authorization", auth)
                .contentType(MediaType.APPLICATION_JSON).content(equipoJson(codigo(), "X", "Laptop")))
                .andExpect(status().isForbidden());
        mvc.perform(delete("/api/equipos/" + id).header("Authorization", auth)).andExpect(status().isForbidden());
        mvc.perform(post("/api/prestamos").header("Authorization", auth)
                .contentType(MediaType.APPLICATION_JSON).content(prestamoJson(id, "Ana", 0, 3)))
                .andExpect(status().isForbidden());
        mvc.perform(put("/api/prestamos/1/devolver").header("Authorization", auth)).andExpect(status().isForbidden());
    }

    // ------------------------------------------------------------------ Equipos

    @Test
    void adminCreaEquipoYQuedaDisponible() throws Exception {
        mvc.perform(post("/api/equipos").header("Authorization", bearer(adminToken))
                        .contentType(MediaType.APPLICATION_JSON).content(equipoJson(codigo(), "Laptop Lenovo", "Laptop")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.estado").value("DISPONIBLE"))
                .andExpect(jsonPath("$.id").isNumber());
    }

    @Test
    void equipoConDatosInvalidosDevuelve400() throws Exception {
        mvc.perform(post("/api/equipos").header("Authorization", bearer(adminToken))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"codigoPatrimonial\":\"\",\"nombre\":\" \"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detalles").isArray());
    }

    @Test
    void codigoPatrimonialDuplicadoDevuelve409() throws Exception {
        String cod = codigo();
        crearEquipo(cod, "Laptop");
        mvc.perform(post("/api/equipos").header("Authorization", bearer(adminToken))
                        .contentType(MediaType.APPLICATION_JSON).content(equipoJson(cod, "Otro", "Laptop")))
                .andExpect(status().isConflict());
    }

    @Test
    void consultarYActualizarEquipo() throws Exception {
        long id = crearEquipo("Tableta");
        mvc.perform(get("/api/equipos/" + id).header("Authorization", bearer(userToken)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.tipo").value("Tableta"));

        mvc.perform(put("/api/equipos/" + id).header("Authorization", bearer(adminToken))
                        .contentType(MediaType.APPLICATION_JSON).content(equipoJson(codigo(), "Nombre nuevo", "Tableta")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.nombre").value("Nombre nuevo"));
    }

    @Test
    void equipoInexistenteDevuelve404() throws Exception {
        mvc.perform(get("/api/equipos/999999").header("Authorization", bearer(userToken)))
                .andExpect(status().isNotFound());
        mvc.perform(put("/api/equipos/999999").header("Authorization", bearer(adminToken))
                        .contentType(MediaType.APPLICATION_JSON).content(equipoJson(codigo(), "X", "Y")))
                .andExpect(status().isNotFound());
        mvc.perform(delete("/api/equipos/999999").header("Authorization", bearer(adminToken)))
                .andExpect(status().isNotFound());
    }

    @Test
    void eliminarEquipoSinPrestamosActivosDevuelve204() throws Exception {
        long id = crearEquipo("Proyector");
        mvc.perform(delete("/api/equipos/" + id).header("Authorization", bearer(adminToken)))
                .andExpect(status().isNoContent());
        mvc.perform(get("/api/equipos/" + id).header("Authorization", bearer(adminToken)))
                .andExpect(status().isNotFound());
    }

    // ---------------------------------------------------------------- Prestamos

    @Test
    void flujoCompletoPrestamoDevolucionYEliminacion() throws Exception {
        long equipoId = crearEquipo("Laptop");

        // Prestamo -> equipo PRESTADO
        long prestamoId = registrarPrestamo(equipoId);
        assertEquals("PRESTADO", estadoEquipo(equipoId));

        // Prestar de nuevo el mismo equipo -> 409
        mvc.perform(post("/api/prestamos").header("Authorization", bearer(adminToken))
                        .contentType(MediaType.APPLICATION_JSON).content(prestamoJson(equipoId, "Luis", 0, 2)))
                .andExpect(status().isConflict());

        // Eliminar con prestamo activo -> 409
        mvc.perform(delete("/api/equipos/" + equipoId).header("Authorization", bearer(adminToken)))
                .andExpect(status().isConflict());

        // Devolver -> prestamo DEVUELTO y equipo DISPONIBLE
        mvc.perform(put("/api/prestamos/" + prestamoId + "/devolver").header("Authorization", bearer(adminToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("DEVUELTO"));
        assertEquals("DISPONIBLE", estadoEquipo(equipoId));

        // Devolver otra vez -> 409
        mvc.perform(put("/api/prestamos/" + prestamoId + "/devolver").header("Authorization", bearer(adminToken)))
                .andExpect(status().isConflict());

        // Ya sin prestamos activos se puede prestar de nuevo y tambien eliminar
        long segundo = registrarPrestamo(equipoId);
        mvc.perform(put("/api/prestamos/" + segundo + "/devolver").header("Authorization", bearer(adminToken)))
                .andExpect(status().isOk());
        mvc.perform(delete("/api/equipos/" + equipoId).header("Authorization", bearer(adminToken)))
                .andExpect(status().isNoContent());
    }

    @Test
    void fechaDevolucionAnteriorAlPrestamoDevuelve400() throws Exception {
        long equipoId = crearEquipo("Laptop");
        mvc.perform(post("/api/prestamos").header("Authorization", bearer(adminToken))
                        .contentType(MediaType.APPLICATION_JSON).content(prestamoJson(equipoId, "Ana", 5, 2)))
                .andExpect(status().isBadRequest());
        assertEquals("DISPONIBLE", estadoEquipo(equipoId));
    }

    @Test
    void prestamoConCamposFaltantesOFechaMalFormadaDevuelve400() throws Exception {
        long equipoId = crearEquipo("Laptop");
        mvc.perform(post("/api/prestamos").header("Authorization", bearer(adminToken))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"equipoId\":" + equipoId + "}"))
                .andExpect(status().isBadRequest());
        mvc.perform(post("/api/prestamos").header("Authorization", bearer(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"equipoId\":" + equipoId + ",\"solicitante\":\"Ana\","
                                + "\"fechaPrestamo\":\"26-10-2026\",\"fechaPrevistaDevolucion\":\"2026-10-30\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void prestamoDeEquipoInexistenteDevuelve404() throws Exception {
        mvc.perform(post("/api/prestamos").header("Authorization", bearer(adminToken))
                        .contentType(MediaType.APPLICATION_JSON).content(prestamoJson(999999L, "Ana", 0, 2)))
                .andExpect(status().isNotFound());
        mvc.perform(put("/api/prestamos/999999/devolver").header("Authorization", bearer(adminToken)))
                .andExpect(status().isNotFound());
    }

    @Test
    void userPuedeListarPrestamos() throws Exception {
        long equipoId = crearEquipo("Laptop");
        registrarPrestamo(equipoId);
        mvc.perform(get("/api/prestamos").header("Authorization", bearer(userToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    // ------------------------------------------------------------------ Ayudas

    private String token(String user, String pass) throws Exception {
        MvcResult r = mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"" + user + "\",\"password\":\"" + pass + "\"}"))
                .andExpect(status().isOk())
                .andReturn();
        return om.readTree(r.getResponse().getContentAsString()).get("token").asText();
    }

    private static String bearer(String t) {
        return "Bearer " + t;
    }

    private static String codigo() {
        return "T-" + UUID.randomUUID().toString().substring(0, 8);
    }

    private static String equipoJson(String codigo, String nombre, String tipo) {
        return String.format("{\"codigoPatrimonial\":\"%s\",\"nombre\":\"%s\",\"tipo\":\"%s\"}", codigo, nombre, tipo);
    }

    private static String prestamoJson(long equipoId, String solicitante, int diasInicio, int diasFin) {
        return String.format(
                "{\"equipoId\":%d,\"solicitante\":\"%s\",\"fechaPrestamo\":\"%s\",\"fechaPrevistaDevolucion\":\"%s\"}",
                equipoId, solicitante, LocalDate.now().plusDays(diasInicio), LocalDate.now().plusDays(diasFin));
    }

    private long crearEquipo(String tipo) throws Exception {
        return crearEquipo(codigo(), tipo);
    }

    private long crearEquipo(String codigo, String tipo) throws Exception {
        MvcResult r = mvc.perform(post("/api/equipos").header("Authorization", bearer(adminToken))
                        .contentType(MediaType.APPLICATION_JSON).content(equipoJson(codigo, "Equipo de prueba", tipo)))
                .andExpect(status().isCreated()).andReturn();
        return om.readTree(r.getResponse().getContentAsString()).get("id").asLong();
    }

    private long registrarPrestamo(long equipoId) throws Exception {
        MvcResult r = mvc.perform(post("/api/prestamos").header("Authorization", bearer(adminToken))
                        .contentType(MediaType.APPLICATION_JSON).content(prestamoJson(equipoId, "Ana Lopez", 0, 3)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.estado").value("ACTIVO"))
                .andReturn();
        return om.readTree(r.getResponse().getContentAsString()).get("id").asLong();
    }

    private String estadoEquipo(long id) throws Exception {
        MvcResult r = mvc.perform(get("/api/equipos/" + id).header("Authorization", bearer(userToken)))
                .andExpect(status().isOk()).andReturn();
        JsonNode n = om.readTree(r.getResponse().getContentAsString());
        return n.get("estado").asText();
    }
}
