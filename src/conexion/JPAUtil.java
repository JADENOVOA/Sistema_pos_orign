package conexion;

import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Persistence;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Map;

/** Inicializa las tablas JPA sin borrar los datos que ya existen. */
public final class JPAUtil {
    private JPAUtil() {}

    public static void inicializarEsquema() {
        crearTiposPostgreSQLSiFaltan();

        Map<String, Object> propiedades = new HashMap<>();
        propiedades.put("jakarta.persistence.jdbc.driver", "org.postgresql.Driver");
        propiedades.put("jakarta.persistence.jdbc.url", ConexionBD.getUrl());
        propiedades.put("jakarta.persistence.jdbc.user", ConexionBD.getUsuario());
        propiedades.put("jakarta.persistence.jdbc.password", ConexionBD.getPassword());
        propiedades.put("hibernate.hbm2ddl.auto", "update");

        EntityManagerFactory emf = Persistence.createEntityManagerFactory("BodegaPU", propiedades);
        emf.close();
        restaurarDefaultsPostgreSQL();
        cargarCatalogosIniciales();
        System.out.println("Esquema JPA verificado; las tablas y relaciones faltantes fueron creadas.");
    }

    /** Hibernate update no conserva todos los DEFAULT del esquema PostgreSQL existente. */
    private static void restaurarDefaultsPostgreSQL() {
        String[] defaults = {
            "ALTER TABLE public.usuarios ALTER COLUMN rol SET DEFAULT 'Cajero'::public.rol_usuario",
            "ALTER TABLE public.usuarios ALTER COLUMN estado SET DEFAULT 'Activo'::public.estado_general",
            "ALTER TABLE public.usuarios ALTER COLUMN intentos_fallidos SET DEFAULT 0",
            "ALTER TABLE public.productos ALTER COLUMN precio_costo SET DEFAULT 0.00",
            "ALTER TABLE public.productos ALTER COLUMN stock SET DEFAULT 0",
            "ALTER TABLE public.productos ALTER COLUMN stock_minimo SET DEFAULT 5",
            "ALTER TABLE public.productos ALTER COLUMN unidad_medida SET DEFAULT 'und'",
            "ALTER TABLE public.productos ALTER COLUMN estado SET DEFAULT 'Activo'::public.estado_general",
            "ALTER TABLE public.cajas ALTER COLUMN total_efectivo SET DEFAULT 0.00",
            "ALTER TABLE public.cajas ALTER COLUMN total_billetera SET DEFAULT 0.00",
            "ALTER TABLE public.cajas ALTER COLUMN estado SET DEFAULT 'Abierta'::public.estado_caja",
            "ALTER TABLE public.clientes ALTER COLUMN estado SET DEFAULT TRUE",
            "ALTER TABLE public.metodos_pago ALTER COLUMN estado SET DEFAULT TRUE",
            "ALTER TABLE public.ventas ALTER COLUMN cliente SET DEFAULT 'Cliente Mostrador'",
            "ALTER TABLE public.ventas ALTER COLUMN vuelto SET DEFAULT 0.00"
        };
        try (Connection conexion = ConexionBD.getConexion()) {
            if (conexion == null) throw new IllegalStateException("No se pudo conectar a PostgreSQL para restaurar valores predeterminados.");
            try (Statement sentencia = conexion.createStatement()) {
                for (String ddl : defaults) sentencia.execute(ddl);
            }
        } catch (SQLException e) {
            throw new IllegalStateException("No se pudieron restaurar valores predeterminados de PostgreSQL: " + e.getMessage(), e);
        }
    }

    /** Agrega solo catálogos faltantes; nunca duplica ni elimina datos. */
    private static void cargarCatalogosIniciales() {
        try (Connection conexion = ConexionBD.getConexion()) {
            if (conexion == null) {
                throw new IllegalStateException("No se pudo conectar a PostgreSQL para cargar los catálogos iniciales.");
            }
            conexion.setAutoCommit(false);
            try {
                insertarSiFalta(conexion,
                        "INSERT INTO public.roles (nombre, descripcion) SELECT ?, ? WHERE NOT EXISTS (SELECT 1 FROM public.roles WHERE LOWER(nombre)=LOWER(?))",
                        "Administrador", "Acceso de administración del sistema", "Administrador");
                insertarSiFalta(conexion,
                        "INSERT INTO public.roles (nombre, descripcion) SELECT ?, ? WHERE NOT EXISTS (SELECT 1 FROM public.roles WHERE LOWER(nombre)=LOWER(?))",
                        "Cajero", "Registro de ventas y operaciones de caja", "Cajero");

                String[] categorias = {"Abarrotes", "Bebidas", "Lácteos", "Golosinas", "Limpieza"};
                for (String categoria : categorias) {
                    insertarSiFalta(conexion,
                            "INSERT INTO public.categorias (nombre) SELECT ? WHERE NOT EXISTS (SELECT 1 FROM public.categorias WHERE LOWER(nombre)=LOWER(?))",
                            categoria, categoria);
                }

                String[][] unidades = {{"und", "Unidad"}, {"kg", "Kilogramo"}, {"lt", "Litro"}};
                for (String[] unidad : unidades) {
                    insertarSiFalta(conexion,
                            "INSERT INTO public.unidades_medida (abreviatura, nombre) SELECT ?, ? WHERE NOT EXISTS (SELECT 1 FROM public.unidades_medida WHERE LOWER(abreviatura)=LOWER(?))",
                            unidad[0], unidad[1], unidad[0]);
                }

                String[] metodos = {"Efectivo", "Billetera Digital"};
                for (String metodo : metodos) {
                    insertarSiFalta(conexion,
                            "INSERT INTO public.metodos_pago (nombre, estado) SELECT ?, TRUE WHERE NOT EXISTS (SELECT 1 FROM public.metodos_pago WHERE LOWER(nombre)=LOWER(?))",
                            metodo, metodo);
                }
                conexion.commit();
            } catch (SQLException e) {
                conexion.rollback();
                throw e;
            } finally {
                conexion.setAutoCommit(true);
            }
        } catch (SQLException e) {
            throw new IllegalStateException("No se pudieron cargar los catálogos iniciales: " + e.getMessage(), e);
        }
    }

    private static void insertarSiFalta(Connection conexion, String sql, String... valores) throws SQLException {
        try (PreparedStatement ps = conexion.prepareStatement(sql)) {
            for (int i = 0; i < valores.length; i++) ps.setString(i + 1, valores[i]);
            ps.executeUpdate();
        }
    }

    private static void crearTiposPostgreSQLSiFaltan() {
        String[] tipos = {
            "DO $$ BEGIN IF NOT EXISTS (SELECT 1 FROM pg_type t JOIN pg_namespace n ON n.oid=t.typnamespace WHERE n.nspname='public' AND t.typname='rol_usuario') THEN CREATE TYPE public.rol_usuario AS ENUM ('Administrador','Cajero'); END IF; END $$",
            "DO $$ BEGIN IF NOT EXISTS (SELECT 1 FROM pg_type t JOIN pg_namespace n ON n.oid=t.typnamespace WHERE n.nspname='public' AND t.typname='estado_general') THEN CREATE TYPE public.estado_general AS ENUM ('Activo','Inactivo'); END IF; END $$",
            "DO $$ BEGIN IF NOT EXISTS (SELECT 1 FROM pg_type t JOIN pg_namespace n ON n.oid=t.typnamespace WHERE n.nspname='public' AND t.typname='estado_caja') THEN CREATE TYPE public.estado_caja AS ENUM ('Abierta','Cerrada'); END IF; END $$",
            "DO $$ BEGIN IF NOT EXISTS (SELECT 1 FROM pg_type t JOIN pg_namespace n ON n.oid=t.typnamespace WHERE n.nspname='public' AND t.typname='metodo_pago_tipo') THEN CREATE TYPE public.metodo_pago_tipo AS ENUM ('Efectivo','Billetera Digital'); END IF; END $$",
            "DO $$ BEGIN IF NOT EXISTS (SELECT 1 FROM pg_type t JOIN pg_namespace n ON n.oid=t.typnamespace WHERE n.nspname='public' AND t.typname='tipo_movimiento') THEN CREATE TYPE public.tipo_movimiento AS ENUM ('Entrada','Venta','Ajuste'); END IF; END $$"
        };

        try (Connection conexion = ConexionBD.getConexion()) {
            if (conexion == null) {
                throw new IllegalStateException("No se pudo conectar a PostgreSQL. Verifica que el servidor esté encendido y que la base BODEGA_LAPERUANITA exista.");
            }
            try (Statement sentencia = conexion.createStatement()) {
                for (String ddl : tipos) sentencia.execute(ddl);
            }
        } catch (Exception e) {
            throw new IllegalStateException("No se pudo preparar el esquema de PostgreSQL: " + e.getMessage(), e);
        }
    }
}
