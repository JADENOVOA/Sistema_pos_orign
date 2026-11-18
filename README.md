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

PostgreSQL debe estar instalado y encendido. La aplicación usa `jdbc:postgresql://localhost:5432/BODEGA_LAPERUANITA`; con `db.bootstrap=true` (predeterminado), comprueba la base al iniciar y la crea si falta, y luego crea/actualiza el esquema. Para ello se necesita configurar la contraseña de la cuenta administradora PostgreSQL (`BODEGA_DB_ADMIN_PASSWORD` o la bóveda Windows DPAPI). Configure URL/usuario en `bodega.properties` o con `BODEGA_DB_URL`, `BODEGA_DB_USER` y contraseña con `BODEGA_DB_PASSWORD`; no hay una contraseña predeterminada. Las claves heredadas `db.admin.password` y `db.app.password` se migran al iniciar a la bóveda Windows DPAPI y se quitan del archivo externo.

En desarrollo, `db.bootstrap=true` (predeterminado) agrega de forma idempotente las columnas y tablas de recuperación/sesiones. Para producción configure `db.bootstrap=false` (o `BODEGA_DB_BOOTSTRAP=false`) en cada cliente y use una cuenta PostgreSQL dedicada. Ese modo valida el esquema sin ejecutar DDL, crear datos demo o necesitar credenciales de administrador; ejecute antes `scripts/migracion_recuperacion_sesion.sql` con una cuenta administradora.

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
- Para conexiones remotas configure TLS explícitamente en la URL JDBC (`sslmode=verify-full` recomendado) y limite PostgreSQL al rango LAN: ver [Guía de instalación y despliegue](docs/INSTALACION_DESPLIEGUE.md).
- Contraseñas con PBKDF2-HMAC-SHA256 + sal; las existentes se cifran al iniciar.
- Recuperación por correo SMTP real con token aleatorio de un solo uso, almacenado como hash y con vencimiento de 15 minutos. El proveedor SMTP debe configurarse; no hay modo de envío simulado.
- “Recordarme” persiste un token aleatorio cifrado por Windows DPAPI, almacenado como hash en PostgreSQL, con expiración de 14 días y revocación al cerrar sesión/inactividad.
- Para producir una distribución Windows portable con JDK 17: `scripts\empaquetar_portable.ps1`.
- Módulo **Seguridad** (solo Administrador): auditoría de solo lectura, respaldos/restauración y control de integridad SHA-256.
- Auditoría de dependencias: `.\mvnw.cmd -Pseguridad verify`.
- Guía de evidencias: `docs/GUIA_PRUEBAS_Y_EVIDENCIAS.md`.
