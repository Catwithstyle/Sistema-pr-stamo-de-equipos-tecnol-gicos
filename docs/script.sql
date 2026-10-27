-- =====================================================================
-- TecnoUAM - Sistema de prestamo de equipos tecnologicos
-- Script SQL para PostgreSQL: esquema + datos minimos de demostracion
--
-- Uso:   psql -U tecnouam -d tecnouam -f docs/script.sql
--
-- NOTA: la aplicacion tambien crea las tablas (spring.jpa.hibernate.ddl-auto=update)
-- y las cuentas de prueba al iniciar. Este script permite reproducir la base
-- sin arrancar el backend. Es idempotente: se puede ejecutar mas de una vez.
-- =====================================================================

CREATE EXTENSION IF NOT EXISTS pgcrypto;   -- para generar hashes BCrypt de las cuentas de prueba

-- ---------------------------------------------------------------- Esquema
CREATE TABLE IF NOT EXISTS usuarios (
    id        BIGSERIAL PRIMARY KEY,
    username  VARCHAR(50)  NOT NULL UNIQUE,
    password  VARCHAR(255) NOT NULL,                       -- hash BCrypt
    rol       VARCHAR(20)  NOT NULL CHECK (rol IN ('USER', 'ADMIN'))
);

CREATE TABLE IF NOT EXISTS equipos (
    id                  BIGSERIAL PRIMARY KEY,
    codigo_patrimonial  VARCHAR(50)  NOT NULL UNIQUE,
    nombre              VARCHAR(100) NOT NULL,
    tipo                VARCHAR(50)  NOT NULL,
    estado              VARCHAR(20)  NOT NULL CHECK (estado IN ('DISPONIBLE', 'PRESTADO'))
);

CREATE TABLE IF NOT EXISTS prestamos (
    id                         BIGSERIAL PRIMARY KEY,
    equipo_id                  BIGINT       NOT NULL REFERENCES equipos (id),
    solicitante                VARCHAR(100) NOT NULL,
    fecha_prestamo             DATE         NOT NULL,
    fecha_prevista_devolucion  DATE         NOT NULL,
    fecha_devolucion_real      DATE,
    estado                     VARCHAR(20)  NOT NULL CHECK (estado IN ('ACTIVO', 'DEVUELTO')),
    CONSTRAINT ck_fechas CHECK (fecha_prevista_devolucion >= fecha_prestamo)
);

CREATE INDEX IF NOT EXISTS idx_prestamos_equipo_estado ON prestamos (equipo_id, estado);

-- Garantia extra a nivel de BD: un equipo no puede tener dos prestamos ACTIVOS a la vez
CREATE UNIQUE INDEX IF NOT EXISTS ux_prestamo_activo_por_equipo
    ON prestamos (equipo_id) WHERE estado = 'ACTIVO';

-- ------------------------------------------------------------ Datos minimos
-- Cuentas de prueba (cambiar las contrasenas fuera del entorno de demostracion)
--   admin   / Admin123*  -> rol ADMIN
--   usuario / User123*   -> rol USER
INSERT INTO usuarios (username, password, rol)
VALUES ('admin',   crypt('Admin123*', gen_salt('bf', 10)), 'ADMIN'),
       ('usuario', crypt('User123*',  gen_salt('bf', 10)), 'USER')
ON CONFLICT (username) DO NOTHING;

INSERT INTO equipos (codigo_patrimonial, nombre, tipo, estado)
VALUES ('LAP-001', 'Laptop Dell Latitude 5420',        'Laptop',    'DISPONIBLE'),
       ('LAP-002', 'Laptop HP ProBook 450',            'Laptop',    'DISPONIBLE'),
       ('PRY-001', 'Proyector Epson X49',              'Proyector', 'DISPONIBLE'),
       ('TAB-001', 'Tableta Samsung Galaxy Tab A8',    'Tableta',   'DISPONIBLE')
ON CONFLICT (codigo_patrimonial) DO NOTHING;
