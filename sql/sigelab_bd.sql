/* =====================================================================
   SIGELAB - Sistema de Gestión y Control de Laboratorios Universitarios
   Programación II - UMG
   Script para Microsoft SQL Server
   ===================================================================== */

IF DB_ID('SIGELAB') IS NULL
    CREATE DATABASE SIGELAB;
GO
USE SIGELAB;
GO

/* ---------------------------------------------------------------------
   LIMPIEZA (permite re-ejecutar el script)
   --------------------------------------------------------------------- */
DROP VIEW  IF EXISTS vw_dashboard;
DROP VIEW  IF EXISTS vw_prestamos_vencidos;
DROP VIEW  IF EXISTS vw_prestamos_con_pendientes;
DROP TABLE IF EXISTS Auditoria;
DROP TABLE IF EXISTS Mantenimiento;
DROP TABLE IF EXISTS Devolucion;
DROP TABLE IF EXISTS DetallePrestamo;
DROP TABLE IF EXISTS Prestamo;
DROP TABLE IF EXISTS Equipo;
DROP TABLE IF EXISTS TipoEquipo;
DROP TABLE IF EXISTS Laboratorio;
DROP TABLE IF EXISTS Usuario;
DROP TABLE IF EXISTS Rol;
GO

/* ---------------------------------------------------------------------
   ROL
   --------------------------------------------------------------------- */
CREATE TABLE Rol (
    id_rol       INT IDENTITY(1,1) CONSTRAINT PK_Rol PRIMARY KEY,
    nombre       NVARCHAR(30)  NOT NULL CONSTRAINT UQ_Rol_nombre UNIQUE,
    descripcion  NVARCHAR(150) NULL
);

/* ---------------------------------------------------------------------
   USUARIO
   contrasena_hash: SHA-256 en hexadecimal (64 caracteres), nunca texto plano.
   referencia_facial: identificador/carpeta de la imagen usada por el
   componente Python + OpenCV (RN-02).
   --------------------------------------------------------------------- */
CREATE TABLE Usuario (
    id_usuario         INT IDENTITY(1,1) CONSTRAINT PK_Usuario PRIMARY KEY,
    nombre             NVARCHAR(60)  NOT NULL,
    apellido           NVARCHAR(60)  NOT NULL,
    nombre_usuario     NVARCHAR(40)  NOT NULL CONSTRAINT UQ_Usuario_nombre_usuario UNIQUE,
    contrasena_hash    VARCHAR(64)   NOT NULL,
    correo             NVARCHAR(120) NOT NULL CONSTRAINT UQ_Usuario_correo UNIQUE,
    id_rol             INT           NOT NULL,
    estado             VARCHAR(10)   NOT NULL CONSTRAINT DF_Usuario_estado DEFAULT 'ACTIVO',
    referencia_facial  NVARCHAR(200) NULL,
    fecha_creacion     DATETIME2(0)  NOT NULL CONSTRAINT DF_Usuario_fecha DEFAULT SYSDATETIME(),
    CONSTRAINT FK_Usuario_Rol    FOREIGN KEY (id_rol) REFERENCES Rol(id_rol),
    CONSTRAINT CK_Usuario_estado CHECK (estado IN ('ACTIVO','INACTIVO')),
    CONSTRAINT CK_Usuario_correo CHECK (correo LIKE '%_@_%._%')
);

/* ---------------------------------------------------------------------
   LABORATORIO (RN-03: código único)
   --------------------------------------------------------------------- */
CREATE TABLE Laboratorio (
    id_laboratorio INT IDENTITY(1,1) CONSTRAINT PK_Laboratorio PRIMARY KEY,
    codigo         NVARCHAR(20)  NOT NULL CONSTRAINT UQ_Laboratorio_codigo UNIQUE,
    nombre         NVARCHAR(100) NOT NULL,
    ubicacion      NVARCHAR(150) NOT NULL,
    capacidad      INT           NOT NULL,
    estado         VARCHAR(10)   NOT NULL CONSTRAINT DF_Laboratorio_estado DEFAULT 'ACTIVO',
    CONSTRAINT CK_Laboratorio_capacidad CHECK (capacidad > 0),
    CONSTRAINT CK_Laboratorio_estado    CHECK (estado IN ('ACTIVO','INACTIVO'))
);

/* ---------------------------------------------------------------------
   TIPO DE EQUIPO (catálogo)
   --------------------------------------------------------------------- */
CREATE TABLE TipoEquipo (
    id_tipo_equipo INT IDENTITY(1,1) CONSTRAINT PK_TipoEquipo PRIMARY KEY,
    nombre         NVARCHAR(60)  NOT NULL CONSTRAINT UQ_TipoEquipo_nombre UNIQUE,
    descripcion    NVARCHAR(150) NULL
);

/* ---------------------------------------------------------------------
   EQUIPO (RN-04: código de inventario único; cinco estados obligatorios)
   El código de inventario es también el valor que contiene el QR.
   --------------------------------------------------------------------- */
CREATE TABLE Equipo (
    id_equipo          INT IDENTITY(1,1) CONSTRAINT PK_Equipo PRIMARY KEY,
    codigo_inventario  NVARCHAR(30)  NOT NULL CONSTRAINT UQ_Equipo_codigo UNIQUE,
    nombre             NVARCHAR(100) NOT NULL,
    id_tipo_equipo     INT           NOT NULL,
    marca              NVARCHAR(60)  NULL,
    modelo             NVARCHAR(60)  NULL,
    numero_serie       NVARCHAR(80)  NULL,
    id_laboratorio     INT           NOT NULL,
    estado             VARCHAR(15)   NOT NULL CONSTRAINT DF_Equipo_estado DEFAULT 'DISPONIBLE',
    observaciones      NVARCHAR(300) NULL,
    CONSTRAINT FK_Equipo_TipoEquipo  FOREIGN KEY (id_tipo_equipo) REFERENCES TipoEquipo(id_tipo_equipo),
    CONSTRAINT FK_Equipo_Laboratorio FOREIGN KEY (id_laboratorio) REFERENCES Laboratorio(id_laboratorio),
    CONSTRAINT CK_Equipo_estado CHECK (estado IN ('DISPONIBLE','PRESTADO','DANADO','MANTENIMIENTO','BAJA'))
);

-- Número de serie único cuando está informado (permite varios NULL)
CREATE UNIQUE INDEX UX_Equipo_numero_serie
    ON Equipo(numero_serie) WHERE numero_serie IS NOT NULL;
CREATE INDEX IX_Equipo_estado      ON Equipo(estado);
CREATE INDEX IX_Equipo_laboratorio ON Equipo(id_laboratorio);

/* ---------------------------------------------------------------------
   PRESTAMO (encabezado)
   estado: ACTIVO mientras exista algún equipo pendiente; FINALIZADO
   cuando todos fueron devueltos (RN-16). "Vencido" NO se guarda: se
   calcula (RN-17) con la vista vw_prestamos_vencidos.
   --------------------------------------------------------------------- */
CREATE TABLE Prestamo (
    id_prestamo               INT IDENTITY(1,1) CONSTRAINT PK_Prestamo PRIMARY KEY,
    solicitante_nombre        NVARCHAR(120) NOT NULL,
    solicitante_identificacion NVARCHAR(30) NULL,   -- carné / código de docente
    fecha_prestamo            DATETIME2(0)  NOT NULL CONSTRAINT DF_Prestamo_fecha DEFAULT SYSDATETIME(),
    fecha_esperada_devolucion DATETIME2(0)  NOT NULL,
    id_usuario_registra       INT           NOT NULL,
    estado                    VARCHAR(12)   NOT NULL CONSTRAINT DF_Prestamo_estado DEFAULT 'ACTIVO',
    observaciones             NVARCHAR(300) NULL,
    CONSTRAINT FK_Prestamo_Usuario FOREIGN KEY (id_usuario_registra) REFERENCES Usuario(id_usuario),
    CONSTRAINT CK_Prestamo_estado  CHECK (estado IN ('ACTIVO','FINALIZADO')),
    CONSTRAINT CK_Prestamo_fechas  CHECK (fecha_esperada_devolucion >= fecha_prestamo)
);
CREATE INDEX IX_Prestamo_estado ON Prestamo(estado);
CREATE INDEX IX_Prestamo_fechas ON Prestamo(fecha_prestamo, fecha_esperada_devolucion);

/* ---------------------------------------------------------------------
   DETALLE_PRESTAMO (RN-05, RN-06)
   UNIQUE(id_prestamo, id_equipo): el mismo equipo no se repite en un préstamo.
   --------------------------------------------------------------------- */
CREATE TABLE DetallePrestamo (
    id_detalle   INT IDENTITY(1,1) CONSTRAINT PK_DetallePrestamo PRIMARY KEY,
    id_prestamo  INT NOT NULL,
    id_equipo    INT NOT NULL,
    CONSTRAINT FK_Detalle_Prestamo FOREIGN KEY (id_prestamo) REFERENCES Prestamo(id_prestamo),
    CONSTRAINT FK_Detalle_Equipo   FOREIGN KEY (id_equipo)   REFERENCES Equipo(id_equipo),
    CONSTRAINT UQ_Detalle_prestamo_equipo UNIQUE (id_prestamo, id_equipo)
);
CREATE INDEX IX_Detalle_equipo ON DetallePrestamo(id_equipo);

/* ---------------------------------------------------------------------
   DEVOLUCION (una por línea de detalle)
   UNIQUE(id_detalle) => un equipo no puede devolverse dos veces (RN-11).
   Un detalle SIN fila en Devolucion = equipo pendiente (RN-10, RN-16).
   --------------------------------------------------------------------- */
CREATE TABLE Devolucion (
    id_devolucion        INT IDENTITY(1,1) CONSTRAINT PK_Devolucion PRIMARY KEY,
    id_detalle           INT           NOT NULL CONSTRAINT UQ_Devolucion_detalle UNIQUE,
    fecha_devolucion     DATETIME2(0)  NOT NULL CONSTRAINT DF_Devolucion_fecha DEFAULT SYSDATETIME(),
    condicion            VARCHAR(10)   NOT NULL,
    observaciones        NVARCHAR(300) NULL,
    id_usuario_recibe    INT           NOT NULL,
    CONSTRAINT FK_Devolucion_Detalle FOREIGN KEY (id_detalle) REFERENCES DetallePrestamo(id_detalle),
    CONSTRAINT FK_Devolucion_Usuario FOREIGN KEY (id_usuario_recibe) REFERENCES Usuario(id_usuario),
    CONSTRAINT CK_Devolucion_condicion CHECK (condicion IN ('BUENO','DANADO'))
);

/* ---------------------------------------------------------------------
   MANTENIMIENTO
   resultado se llena al cerrar: REPARADO -> DISPONIBLE, NO_REPARABLE -> BAJA
   --------------------------------------------------------------------- */
CREATE TABLE Mantenimiento (
    id_mantenimiento INT IDENTITY(1,1) CONSTRAINT PK_Mantenimiento PRIMARY KEY,
    id_equipo        INT           NOT NULL,
    fecha_ingreso    DATETIME2(0)  NOT NULL CONSTRAINT DF_Mant_ingreso DEFAULT SYSDATETIME(),
    tipo             VARCHAR(12)   NOT NULL,
    descripcion      NVARCHAR(300) NOT NULL,
    responsable      NVARCHAR(100) NOT NULL,
    fecha_salida     DATETIME2(0)  NULL,
    resultado        VARCHAR(15)   NULL,
    estado           VARCHAR(12)   NOT NULL CONSTRAINT DF_Mant_estado DEFAULT 'EN_PROCESO',
    observaciones    NVARCHAR(300) NULL,
    id_usuario_registra INT        NOT NULL,
    CONSTRAINT FK_Mant_Equipo  FOREIGN KEY (id_equipo) REFERENCES Equipo(id_equipo),
    CONSTRAINT FK_Mant_Usuario FOREIGN KEY (id_usuario_registra) REFERENCES Usuario(id_usuario),
    CONSTRAINT CK_Mant_tipo      CHECK (tipo IN ('PREVENTIVO','CORRECTIVO')),
    CONSTRAINT CK_Mant_estado    CHECK (estado IN ('EN_PROCESO','FINALIZADO')),
    CONSTRAINT CK_Mant_resultado CHECK (resultado IS NULL OR resultado IN ('REPARADO','NO_REPARABLE')),
    CONSTRAINT CK_Mant_fechas    CHECK (fecha_salida IS NULL OR fecha_salida >= fecha_ingreso),
    -- Coherencia: finalizado <=> tiene fecha de salida y resultado
    CONSTRAINT CK_Mant_cierre CHECK (
        (estado = 'EN_PROCESO' AND fecha_salida IS NULL AND resultado IS NULL) OR
        (estado = 'FINALIZADO' AND fecha_salida IS NOT NULL AND resultado IS NOT NULL)
    )
);
CREATE INDEX IX_Mant_equipo ON Mantenimiento(id_equipo);
-- Un equipo no puede tener dos mantenimientos abiertos a la vez
CREATE UNIQUE INDEX UX_Mant_un_abierto_por_equipo
    ON Mantenimiento(id_equipo) WHERE estado = 'EN_PROCESO';

/* ---------------------------------------------------------------------
   AUDITORIA (RN-18)
   id_usuario es NULL-able para LOGIN_FALLIDO con usuario inexistente.
   --------------------------------------------------------------------- */
CREATE TABLE Auditoria (
    id_auditoria INT IDENTITY(1,1) CONSTRAINT PK_Auditoria PRIMARY KEY,
    id_usuario   INT           NULL,
    fecha_hora   DATETIME2(0)  NOT NULL CONSTRAINT DF_Auditoria_fecha DEFAULT SYSDATETIME(),
    accion       NVARCHAR(40)  NOT NULL,
    entidad      NVARCHAR(40)  NOT NULL,
    id_registro  INT           NULL,
    descripcion  NVARCHAR(300) NULL,
    CONSTRAINT FK_Auditoria_Usuario FOREIGN KEY (id_usuario) REFERENCES Usuario(id_usuario)
);
CREATE INDEX IX_Auditoria_fecha   ON Auditoria(fecha_hora);
CREATE INDEX IX_Auditoria_usuario ON Auditoria(id_usuario);
GO

/* ---------------------------------------------------------------------
   VISTAS PARA DASHBOARD Y REPORTES
   --------------------------------------------------------------------- */

-- Préstamos con la cantidad de equipos pendientes (sin devolución)
CREATE VIEW vw_prestamos_con_pendientes AS
SELECT  p.id_prestamo,
        p.solicitante_nombre,
        p.fecha_prestamo,
        p.fecha_esperada_devolucion,
        p.estado,
        COUNT(d.id_detalle)                                   AS total_equipos,
        SUM(CASE WHEN dv.id_devolucion IS NULL THEN 1 ELSE 0 END) AS equipos_pendientes
FROM    Prestamo p
JOIN    DetallePrestamo d  ON d.id_prestamo = p.id_prestamo
LEFT JOIN Devolucion dv    ON dv.id_detalle = d.id_detalle
GROUP BY p.id_prestamo, p.solicitante_nombre, p.fecha_prestamo,
         p.fecha_esperada_devolucion, p.estado;
GO

-- RN-17: vencido = fecha esperada ya pasó y aún hay equipos pendientes
CREATE VIEW vw_prestamos_vencidos AS
SELECT  *
FROM    vw_prestamos_con_pendientes
WHERE   equipos_pendientes > 0
  AND   fecha_esperada_devolucion < SYSDATETIME();
GO

-- Una sola fila con todos los contadores del dashboard
CREATE VIEW vw_dashboard AS
SELECT
    (SELECT COUNT(*) FROM Equipo)                                     AS total_equipos,
    (SELECT COUNT(*) FROM Equipo WHERE estado = 'DISPONIBLE')         AS disponibles,
    (SELECT COUNT(*) FROM Equipo WHERE estado = 'PRESTADO')           AS prestados,
    (SELECT COUNT(*) FROM Equipo WHERE estado = 'DANADO')             AS danados,
    (SELECT COUNT(*) FROM Equipo WHERE estado = 'MANTENIMIENTO')      AS en_mantenimiento,
    (SELECT COUNT(*) FROM Equipo WHERE estado = 'BAJA')               AS de_baja,
    (SELECT COUNT(*) FROM Prestamo WHERE estado = 'ACTIVO')           AS prestamos_activos,
    (SELECT COUNT(*) FROM vw_prestamos_vencidos)                      AS prestamos_vencidos;
GO

/* ---------------------------------------------------------------------
   DATOS MÍNIMOS DE PRUEBA
   Contraseñas (SHA-256 hex en MAYÚSCULAS):
     admin / Admin123*   encargado1 / Encargado123*   docente1 / Docente123*
   En Java compare ignorando mayúsculas/minúsculas o convierta el hash a mayúsculas.
   --------------------------------------------------------------------- */
INSERT INTO Rol (nombre, descripcion) VALUES
 ('ADMIN',     'Acceso completo al sistema'),
 ('ENCARGADO', 'Operación cotidiana de laboratorios'),
 ('DOCENTE',   'Consultas / solicitante');

INSERT INTO Usuario (nombre, apellido, nombre_usuario, contrasena_hash, correo, id_rol, estado) VALUES
 ('Administrador', 'Sistema',  'admin',      CONVERT(VARCHAR(64), HASHBYTES('SHA2_256','Admin123*'),2),      'admin@umg.edu.gt',      1, 'ACTIVO'),
 ('Carlos',        'Pérez',    'encargado1', CONVERT(VARCHAR(64), HASHBYTES('SHA2_256','Encargado123*'),2),  'encargado1@umg.edu.gt', 2, 'ACTIVO'),
 ('María',         'López',    'docente1',   CONVERT(VARCHAR(64), HASHBYTES('SHA2_256','Docente123*'),2),    'docente1@umg.edu.gt',   3, 'INACTIVO'); -- sirve para probar RN-01

INSERT INTO Laboratorio (codigo, nombre, ubicacion, capacidad, estado) VALUES
 ('LAB-01', 'Laboratorio de Computación 1', 'Edificio A, nivel 2', 30, 'ACTIVO'),
 ('LAB-02', 'Laboratorio de Redes',         'Edificio B, nivel 1', 20, 'ACTIVO'),
 ('LAB-03', 'Laboratorio de Electrónica',   'Edificio B, nivel 2', 15, 'ACTIVO');

INSERT INTO TipoEquipo (nombre, descripcion) VALUES
 ('Laptop',          'Computadora portátil'),
 ('Cámara',          'Cámara fotográfica o de video'),
 ('Proyector',       'Proyector multimedia'),
 ('Router',          'Equipo de enrutamiento'),
 ('Switch',          'Switch de red'),
 ('Tablet',          'Tableta'),
 ('Monitor',         'Monitor / pantalla'),
 ('Kit electrónico', 'Kit de componentes electrónicos');

INSERT INTO Equipo (codigo_inventario, nombre, id_tipo_equipo, marca, modelo, numero_serie, id_laboratorio, estado, observaciones) VALUES
 ('CAM-001', 'Cámara Canon 1',        2, 'Canon',  'EOS 250D',   'SN-CAM-001', 1, 'DISPONIBLE',    NULL),
 ('CAM-002', 'Cámara Canon 2',        2, 'Canon',  'EOS 250D',   'SN-CAM-002', 1, 'DISPONIBLE',    NULL),
 ('LAP-001', 'Laptop Dell 1',         1, 'Dell',   'Latitude 5420','SN-LAP-001',1, 'DISPONIBLE',   NULL),
 ('LAP-002', 'Laptop Dell 2',         1, 'Dell',   'Latitude 5420','SN-LAP-002',1, 'DISPONIBLE',   NULL),
 ('PRY-001', 'Proyector Epson',       3, 'Epson',  'X49',        'SN-PRY-001', 1, 'DISPONIBLE',    NULL),
 ('RTR-001', 'Router Cisco',          4, 'Cisco',  'ISR 1100',   'SN-RTR-001', 2, 'DISPONIBLE',    NULL),
 ('SWT-001', 'Switch Cisco',          5, 'Cisco',  'Catalyst 2960','SN-SWT-001',2,'DISPONIBLE',    NULL),
 ('TAB-001', 'Tablet Samsung',        6, 'Samsung','Tab A8',     'SN-TAB-001', 1, 'DANADO',        'Pantalla rota'),
 ('MON-001', 'Monitor LG',            7, 'LG',     '24MK430',    'SN-MON-001', 1, 'MANTENIMIENTO', 'En revisión'),
 ('KIT-001', 'Kit Arduino',           8, 'Arduino','Starter Kit','SN-KIT-001', 3, 'BAJA',          'Obsoleto');

-- Mantenimiento abierto coherente con MON-001 (estado MANTENIMIENTO)
INSERT INTO Mantenimiento (id_equipo, tipo, descripcion, responsable, id_usuario_registra)
SELECT id_equipo, 'CORRECTIVO', 'Revisión por fallo de imagen', 'Téc. Juan Ramírez', 2
FROM Equipo WHERE codigo_inventario = 'MON-001';
GO

/* ---------------------------------------------------------------------
   CONSULTAS DE REPORTE DE EJEMPLO (para Repository)
   --------------------------------------------------------------------- */
-- Equipos por estado
-- SELECT estado, COUNT(*) AS cantidad FROM Equipo GROUP BY estado ORDER BY estado;

-- Préstamos entre fechas
-- SELECT * FROM Prestamo WHERE fecha_prestamo BETWEEN @desde AND @hasta ORDER BY fecha_prestamo DESC;

-- Historial de préstamos de un equipo
-- SELECT p.id_prestamo, p.solicitante_nombre, p.fecha_prestamo, p.fecha_esperada_devolucion,
--        dv.fecha_devolucion, dv.condicion
-- FROM Equipo e
-- JOIN DetallePrestamo d ON d.id_equipo = e.id_equipo
-- JOIN Prestamo p        ON p.id_prestamo = d.id_prestamo
-- LEFT JOIN Devolucion dv ON dv.id_detalle = d.id_detalle
-- WHERE e.codigo_inventario = @codigo ORDER BY p.fecha_prestamo DESC;

-- Historial de mantenimiento de un equipo
-- SELECT m.* FROM Mantenimiento m JOIN Equipo e ON e.id_equipo = m.id_equipo
-- WHERE e.codigo_inventario = @codigo ORDER BY m.fecha_ingreso DESC;

-- Dashboard
-- SELECT * FROM vw_dashboard;
