# TecnoUAM – Sistema de préstamo de equipos tecnológicos (Backend)

API REST con **Spring Boot 3.3 / Java 17**, **Spring Data JPA + PostgreSQL**, y **Spring Security + JWT** (sin sesiones, roles `USER` y `ADMIN`).
Examen práctico integrador – Servicios Web, Segundo corte (UAM).

## Requisitos

- Java 17 o superior
- Maven 3.9+
- PostgreSQL 16 (o Docker, con el `docker-compose.yml` incluido)

## Ejecución

```bash
# 1. Base de datos (opción Docker)
docker compose up -d

# 2. Variables de entorno (ver tabla). Mínimo: JWT_SECRET
export JWT_SECRET="$(openssl rand -base64 48)"
export DB_PASSWORD=tecnouam          # si usa el docker-compose por defecto

# 3. Iniciar el backend (http://localhost:8080)
mvn spring-boot:run
```

En Windows (PowerShell):

```powershell
$env:JWT_SECRET = [Convert]::ToBase64String((1..48 | % { Get-Random -Max 256 }) -as [byte[]])
mvn spring-boot:run
```

Al iniciar, la aplicación **crea las tablas** y las **cuentas de prueba**, y carga 4 equipos de ejemplo si la tabla está vacía.
Si prefiere crear la base manualmente: `psql -U tecnouam -d tecnouam -f docs/script.sql`.

Pruebas automatizadas (usan H2 en memoria, no requieren PostgreSQL): `mvn test`

## Variables de entorno

Ningún secreto está escrito en el repositorio. Ver `.env.example`.

| Variable | Obligatoria | Descripción | Valor por defecto |
|---|---|---|---|
| `JWT_SECRET` | **Sí** | Secreto de firma del JWT, mínimo 32 caracteres. Sin ella la app no inicia | – |
| `DB_URL` | No | URL JDBC | `jdbc:postgresql://localhost:5432/tecnouam` |
| `DB_USER` | No | Usuario de la BD | `tecnouam` |
| `DB_PASSWORD` | No | Contraseña de la BD | `tecnouam` (solo desarrollo) |
| `JWT_EXPIRATION_MINUTES` | No | Vigencia del token | `60` |
| `CORS_ORIGINS` | No | Orígenes permitidos del frontend, separados por coma | `http://localhost:5173,http://localhost:3000` |
| `SEED_ADMIN_PASSWORD` / `SEED_USER_PASSWORD` | No | Contraseñas de las cuentas de prueba | `Admin123*` / `User123*` |
| `SEED_SAMPLE_DATA` | No | Cargar equipos de ejemplo si no hay ninguno | `true` |

## Cuentas de prueba

| Usuario | Contraseña | Rol | Permisos |
|---|---|---|---|
| `admin` | `Admin123*` | ADMIN | Consultar, crear, actualizar, eliminar equipos; registrar préstamos y devoluciones |
| `usuario` | `User123*` | USER | Solo consultar equipos y préstamos |

## Endpoints

Todas las rutas excepto el login requieren `Authorization: Bearer <token>`.

| Método y ruta | Rol | Éxito | Errores posibles |
|---|---|---|---|
| `POST /api/auth/login` | público | 200 + token | 400 campos vacíos, **401** credenciales incorrectas |
| `GET /api/equipos` | USER, ADMIN | 200 | 401 |
| `GET /api/equipos/{id}` | USER, ADMIN | 200 | 401, 404 |
| `POST /api/equipos` | ADMIN | 201 (+ `Location`) | 400 datos inválidos, 401, **403**, 409 código duplicado |
| `PUT /api/equipos/{id}` | ADMIN | 200 | 400, 401, 403, 404, 409 |
| `DELETE /api/equipos/{id}` | ADMIN | 204 | 401, 403, 404, **409 si tiene préstamo activo** |
| `GET /api/prestamos` | USER, ADMIN | 200 | 401 |
| `GET /api/prestamos/{id}` | USER, ADMIN | 200 | 401, 404 |
| `POST /api/prestamos` | ADMIN | 201 | 400 (campos/fechas), 401, 403, 404 equipo inexistente, **409 equipo ya PRESTADO** |
| `PUT /api/prestamos/{id}/devolver` | ADMIN | 200 | 401, 403, 404, 409 ya devuelto |

Ejemplos de cuerpos:

```jsonc
// POST /api/auth/login
{ "username": "admin", "password": "Admin123*" }

// POST /api/equipos
{ "codigoPatrimonial": "LAP-010", "nombre": "Laptop Lenovo T14", "tipo": "Laptop" }

// POST /api/prestamos   (fechas en formato yyyy-MM-dd)
{ "equipoId": 1, "solicitante": "María González", "fechaPrestamo": "2026-10-28", "fechaPrevistaDevolucion": "2026-11-04" }
```

Los errores devuelven un JSON uniforme: `{ timestamp, status, error, mensaje, detalles[] }`.

## Reglas de negocio implementadas

- Cada equipo tiene id, **código patrimonial único**, nombre, tipo y estado `DISPONIBLE` / `PRESTADO`. El estado no se envía en el alta ni en `PUT`: lo gobiernan solo los préstamos.
- Cada préstamo tiene id, equipo, solicitante, fecha de préstamo, fecha prevista de devolución y estado `ACTIVO` / `DEVUELTO` (se agrega además la fecha real de devolución).
- No se puede prestar un equipo ya `PRESTADO` → **409**. La fila del equipo se bloquea (bloqueo pesimista) para evitar dos préstamos simultáneos, y la base de datos tiene un índice único parcial como segunda barrera (en `docs/script.sql`).
- Registrar préstamo → equipo `PRESTADO`. Registrar devolución → equipo `DISPONIBLE`. Ambos cambios ocurren en una sola transacción.
- No se puede eliminar un equipo con préstamos `ACTIVO` → **409**. Si solo tiene préstamos devueltos, se elimina junto con ese historial.
- Validaciones: campos obligatorios, formato de fecha, y `fechaPrevistaDevolucion` ≥ `fechaPrestamo` → **400**.

## Seguridad (flujo JWT)

1. `POST /api/auth/login` valida usuario y contraseña (hash **BCrypt** en la tabla `usuarios`) con `AuthenticationManager`.
2. Si son correctas, se emite un JWT firmado (HMAC-SHA) con `sub` = usuario, claim `rol` y expiración.
3. `JwtAuthenticationFilter` lee `Authorization: Bearer ...`, valida firma y expiración y autentica la petición. API **stateless** (`SessionCreationPolicy.STATELESS`).
4. `SecurityConfig` aplica las reglas: `GET` → USER o ADMIN; `POST/PUT/DELETE` → solo ADMIN.
5. **401 Unauthorized**: no hay token, es inválido o expiró (usuario *no autenticado*). **403 Forbidden**: hay token válido pero el rol no alcanza (usuario *autenticado sin permiso*).

## Colección Postman

Importar `docs/TecnoUAM.postman_collection.json`. Variable `baseUrl` = `http://localhost:8080`.
Ejecutar en orden con el **Collection Runner** (la carpeta `0. Autenticación` guarda los tokens). Cubre los 9 casos del examen:

| Caso del examen | Solicitud |
|---|---|
| (1) Alta de equipo | 1.1, 1.2 |
| (2) Consulta de equipos | 1.3, 1.4 |
| (3) Actualización | 1.5 |
| (4) Registro de préstamo | 2.1, 2.2 |
| (5) Prestar de nuevo el mismo equipo (rechazado) | 2.3 |
| (6) Devolución y estado DISPONIBLE | 2.7, 2.8 |
| (7) Eliminación permitida / bloqueada | 3.1 / 2.6 |
| (8) Respuestas 401 y 403 | carpeta 4 |
| (9) Visualización en React | 2.11 deja el equipo B `PRESTADO`; recargar la tabla del frontend |

Cada solicitud trae pruebas automáticas en la pestaña *Tests*.

## Estructura

```
backend/
├── pom.xml, docker-compose.yml, .env.example
├── docs/script.sql                      # esquema + datos mínimos
├── docs/TecnoUAM.postman_collection.json
└── src/main/java/edu/uam/tecnouam/prestamos/
    ├── model/        Equipo, Prestamo, Usuario, enums   (relación 1:N Equipo–Prestamo)
    ├── repository/   Spring Data JPA
    ├── service/      EquipoService, PrestamoService     (reglas de negocio)
    ├── controller/   EquipoController, PrestamoController, AuthController
    ├── security/     JwtService, filtro, SecurityConfig, manejadores 401/403
    ├── exception/    excepciones de negocio + GlobalExceptionHandler
    └── config/       DataSeeder (cuentas y equipos de ejemplo)
```

## Distribución sugerida del equipo (orientativa)

| Integrante | Responsabilidad | Dónde mirar |
|---|---|---|
| 1 | Entidades, relaciones JPA y BD | `model/`, `repository/`, `docs/script.sql` |
| 2 | CRUD, controladores y servicios | `controller/`, `service/`, `exception/` |
| 3 | Spring Security, JWT y roles | `security/`, `AuthController` |
| 4 | React, Axios y visualización | carpeta `frontend/` (siguiente etapa) |
| 5 | Pruebas Postman e integración | `docs/*.json`, `ApiIntegrationTest` |

> Recuerden: todos deben poder explicar el sistema completo en la defensa y hacer commits propios en el repositorio compartido.
