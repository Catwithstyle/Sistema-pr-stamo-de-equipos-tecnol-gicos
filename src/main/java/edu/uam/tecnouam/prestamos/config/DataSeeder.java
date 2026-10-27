package edu.uam.tecnouam.prestamos.config;

import edu.uam.tecnouam.prestamos.model.Equipo;
import edu.uam.tecnouam.prestamos.model.EstadoEquipo;
import edu.uam.tecnouam.prestamos.model.Rol;
import edu.uam.tecnouam.prestamos.model.Usuario;
import edu.uam.tecnouam.prestamos.repository.EquipoRepository;
import edu.uam.tecnouam.prestamos.repository.UsuarioRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Crea al iniciar las cuentas de prueba (admin / usuario) y unos equipos de ejemplo,
 * solo si aun no existen. Las contrasenas vienen de variables de entorno, no del codigo.
 */
@Component
@ConditionalOnProperty(name = "app.seed.enabled", havingValue = "true", matchIfMissing = true)
public class DataSeeder implements CommandLineRunner {

    private final UsuarioRepository usuarioRepository;
    private final EquipoRepository equipoRepository;
    private final PasswordEncoder passwordEncoder;
    private final String adminPassword;
    private final String userPassword;
    private final boolean sampleData;

    public DataSeeder(UsuarioRepository usuarioRepository,
                      EquipoRepository equipoRepository,
                      PasswordEncoder passwordEncoder,
                      @Value("${app.seed.admin-password}") String adminPassword,
                      @Value("${app.seed.user-password}") String userPassword,
                      @Value("${app.seed.sample-data:true}") boolean sampleData) {
        this.usuarioRepository = usuarioRepository;
        this.equipoRepository = equipoRepository;
        this.passwordEncoder = passwordEncoder;
        this.adminPassword = adminPassword;
        this.userPassword = userPassword;
        this.sampleData = sampleData;
    }

    @Override
    public void run(String... args) {
        crearUsuario("admin", adminPassword, Rol.ADMIN);
        crearUsuario("usuario", userPassword, Rol.USER);

        if (sampleData && equipoRepository.count() == 0) {
            equipoRepository.saveAll(List.of(
                    equipo("LAP-001", "Laptop Dell Latitude 5420", "Laptop"),
                    equipo("LAP-002", "Laptop HP ProBook 450", "Laptop"),
                    equipo("PRY-001", "Proyector Epson X49", "Proyector"),
                    equipo("TAB-001", "Tableta Samsung Galaxy Tab A8", "Tableta")));
        }
    }

    private void crearUsuario(String username, String password, Rol rol) {
        if (!usuarioRepository.existsByUsername(username)) {
            usuarioRepository.save(new Usuario(username, passwordEncoder.encode(password), rol));
        }
    }

    private Equipo equipo(String codigo, String nombre, String tipo) {
        Equipo e = new Equipo();
        e.setCodigoPatrimonial(codigo);
        e.setNombre(nombre);
        e.setTipo(tipo);
        e.setEstado(EstadoEquipo.DISPONIBLE);
        return e;
    }
}
