# Modelo entidad-relación - SIGELAB

Diagrama generado a partir de `sql/sigelab_bd.sql`: las tablas, columnas y llaves foráneas son las del script.
GitHub lo dibuja automáticamente. Si necesita una imagen, ábralo en GitHub y tome una captura, o genere el
diagrama desde DBeaver (clic derecho en la base `SIGELAB` → *View Diagram*).

```mermaid
erDiagram
    Rol ||--o{ Usuario : tiene
    Usuario ||--o{ Prestamo : registra
    Usuario ||--o{ Devolucion : recibe
    Usuario ||--o{ Mantenimiento : registra
    Usuario |o--o{ Auditoria : genera
    Laboratorio ||--o{ Equipo : contiene
    TipoEquipo ||--o{ Equipo : clasifica
    Prestamo ||--|{ DetallePrestamo : detalla
    Equipo ||--o{ DetallePrestamo : incluido_en
    DetallePrestamo ||--o| Devolucion : se_devuelve
    Equipo ||--o{ Mantenimiento : recibe

    Rol {
        int id_rol PK
        string nombre UK
        string descripcion
    }
    Usuario {
        int id_usuario PK
        string nombre
        string apellido
        string nombre_usuario UK
        string contrasena_hash
        string correo UK
        int id_rol FK
        string estado
        string referencia_facial
        datetime fecha_creacion
    }
    Laboratorio {
        int id_laboratorio PK
        string codigo UK
        string nombre
        string ubicacion
        int capacidad
        string estado
    }
    TipoEquipo {
        int id_tipo_equipo PK
        string nombre UK
        string descripcion
    }
    Equipo {
        int id_equipo PK
        string codigo_inventario UK
        string nombre
        int id_tipo_equipo FK
        string marca
        string modelo
        string numero_serie
        int id_laboratorio FK
        string estado
        string observaciones
    }
    Prestamo {
        int id_prestamo PK
        string solicitante_nombre
        string solicitante_identificacion
        datetime fecha_prestamo
        datetime fecha_esperada_devolucion
        int id_usuario_registra FK
        string estado
        string observaciones
    }
    DetallePrestamo {
        int id_detalle PK
        int id_prestamo FK
        int id_equipo FK
    }
    Devolucion {
        int id_devolucion PK
        int id_detalle FK, UK
        datetime fecha_devolucion
        string condicion
        string observaciones
        int id_usuario_recibe FK
    }
    Mantenimiento {
        int id_mantenimiento PK
        int id_equipo FK
        datetime fecha_ingreso
        string tipo
        string descripcion
        string responsable
        datetime fecha_salida
        string resultado
        string estado
        string observaciones
        int id_usuario_registra FK
    }
    Auditoria {
        int id_auditoria PK
        int id_usuario FK
        datetime fecha_hora
        string accion
        string entidad
        int id_registro
        string descripcion
    }
```

## Cardinalidades

| Relación | Cardinalidad | Significado |
|---|---|---|
| Rol → Usuario | 1 a N | Un rol lo tienen muchos usuarios; cada usuario tiene un rol. |
| Laboratorio → Equipo | 1 a N | Un laboratorio contiene muchos equipos. |
| TipoEquipo → Equipo | 1 a N | Un tipo clasifica a muchos equipos. |
| Prestamo → DetallePrestamo | 1 a N (mínimo 1) | Un préstamo tiene al menos un equipo (RN-05). |
| Equipo → DetallePrestamo | 1 a N | Un equipo puede estar en muchos préstamos a lo largo del tiempo. |
| DetallePrestamo → Devolucion | 1 a 0..1 | Una línea se devuelve como máximo una vez (RN-11); sin devolución = pendiente. |
| Equipo → Mantenimiento | 1 a N | Un equipo acumula su historial de mantenimientos. |
| Usuario → Prestamo, Devolucion, Mantenimiento | 1 a N | Cada operación guarda qué usuario la realizó. |
| Usuario → Auditoria | 0..1 a N | Un registro de auditoría puede no tener usuario (login fallido de un usuario inexistente). |
