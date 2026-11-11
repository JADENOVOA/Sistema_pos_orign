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

Al iniciar la aplicación, Hibernate revisa el esquema PostgreSQL y crea o actualiza las tablas y claves foráneas declaradas en `src/entidades`. La actualización usa `hibernate.hbm2ddl.auto=update`: conserva los datos y no borra tablas existentes. También inserta, solo si faltan, los roles, categorías, unidades y métodos de pago iniciales.

PostgreSQL debe estar instalado y encendido, y la base de datos configurada debe existir. Por defecto se usa `jdbc:postgresql://localhost:5432/BODEGA_LAPERUANITA`, usuario `postgres` y contraseña `root`. Se pueden cambiar con las variables de entorno `BODEGA_DB_URL`, `BODEGA_DB_USER` y `BODEGA_DB_PASSWORD`.

El proyecto incluye Maven Wrapper, así que no necesitas instalar Maven. Se requiere Java 17 o superior y conexión a Internet la primera vez para descargar las dependencias. Desde esta carpeta:

```powershell
.\mvnw.cmd compile exec:java
```

Para generar el ejecutable con dependencias incluidas:

```powershell
.\mvnw.cmd package
```
