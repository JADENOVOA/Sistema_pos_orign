# Bodega La Peruanita POS

## Estructura del proyecto

El código está separado por responsabilidades dentro de `src`:

- `vistas`: ventanas y paneles de Swing.
- `dao`: consultas y operaciones de acceso a datos usadas por la aplicación.
- `entidades`: entidades JPA que representan las tablas y relaciones de PostgreSQL.
- `modelo`: objetos de datos y modelos que consumen las vistas y los DAO.
- `conexion`: conexión JDBC y configuración/inicialización de JPA.
- `util`: utilidades compartidas.
- `META-INF/persistence.xml`: configuración de la unidad de persistencia.
- `imagen` y `librerias`: recursos visuales y dependencias heredadas del proyecto.

Se conserva esta ubicación porque también hay configuración de NetBeans/Ant (`nbproject` y `build.xml`) además de Maven (`pom.xml`). Mantener ambos sistemas apuntando a `src` evita romper la ejecución existente.

## Base de datos JPA

Al iniciar la aplicación, Hibernate revisa el esquema PostgreSQL y crea o actualiza las tablas y claves foráneas declaradas en `src/entidades`. La actualización usa `hibernate.hbm2ddl.auto=update`: conserva los datos y no borra tablas existentes. También agrega si faltan las columnas de bloqueo de usuarios y carga, sin duplicar, roles, categorías, unidades, métodos de pago y datos de demostración para las 11 tablas. Las cuentas iniciales son `demo_admin` y `demo_cajero`, ambas con contraseña `demo-inactivo-2026`.

PostgreSQL debe estar instalado y encendido, y la base de datos configurada debe existir. Por defecto se usa `jdbc:postgresql://localhost:5432/BODEGA_LAPERUANITA`, usuario `postgres` y contraseña `root`. Se pueden cambiar con las variables de entorno `BODEGA_DB_URL`, `BODEGA_DB_USER` y `BODEGA_DB_PASSWORD`.

El proyecto incluye Maven Wrapper, así que no necesitas instalar Maven. Se requiere Java 17 o superior y conexión a Internet la primera vez para descargar las dependencias. Desde esta carpeta:

```powershell
.\mvnw.cmd compile exec:java
```

Para generar el ejecutable con dependencias incluidas:

```powershell
.\mvnw.cmd package
```

## Seguridad (casos de prueba)

- Configuración sin claves en el código: `bodega.properties` (ver `.gitignore`). Cuenta ADMIN solo para crear tablas/restaurar; la app trabaja con `pos_app` (sin DROP/ALTER/DELETE).
- SSL obligatorio (`db.sslmode=require`): ver `docs/ACTIVAR_SSL_POSTGRESQL.md`.
- Contraseñas con PBKDF2-HMAC-SHA256 + sal; las existentes se cifran al iniciar.
- Módulo **Seguridad** (solo Administrador): auditoría de solo lectura, respaldos/restauración y control de integridad SHA-256.
- Auditoría de dependencias: `.\mvnw.cmd -Pseguridad verify`.
- Guía de evidencias: `docs/GUIA_PRUEBAS_Y_EVIDENCIAS.md`.
