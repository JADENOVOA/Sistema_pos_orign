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
        cargarDatosDemostracionIniciales();
        System.out.println("Esquema JPA verificado; las tablas y relaciones faltantes fueron creadas.");
    }

    /** Hibernate update no conserva todos los DEFAULT del esquema PostgreSQL existente. */
    private static void restaurarDefaultsPostgreSQL() {
        String[] defaults = {
            "ALTER TABLE public.usuarios ADD COLUMN IF NOT EXISTS intentos_fallidos INT NOT NULL DEFAULT 0",
            "ALTER TABLE public.usuarios ADD COLUMN IF NOT EXISTS bloqueado_hasta TIMESTAMP NULL",
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

    /** Carga datos útiles para una instalación nueva sin duplicarlos ni borrar datos existentes. */
    private static void cargarDatosDemostracionIniciales() {
        String[] inserts = {
            "INSERT INTO public.clientes (nombre, documento, telefono, estado) "
                + "SELECT 'Cliente de demostración', '00000001', '900000001', TRUE "
                + "WHERE NOT EXISTS (SELECT 1 FROM public.clientes WHERE documento='00000001')",

            "INSERT INTO public.usuarios (nombre_completo, usuario, password, rol, estado, id_rol, intentos_fallidos) "
                + "SELECT 'Usuario Demo Administrador', 'demo_admin', 'demo-inactivo-2026', "
                + "'Administrador'::public.rol_usuario, 'Activo'::public.estado_general, r.id_rol, 0 "
                + "FROM public.roles r WHERE LOWER(r.nombre)='administrador' "
                + "AND NOT EXISTS (SELECT 1 FROM public.usuarios WHERE LOWER(usuario)='demo_admin')",
            "INSERT INTO public.usuarios (nombre_completo, usuario, password, rol, estado, id_rol, intentos_fallidos) "
                + "SELECT 'Usuario Demo Cajero', 'demo_cajero', 'demo-inactivo-2026', "
                + "'Cajero'::public.rol_usuario, 'Activo'::public.estado_general, r.id_rol, 0 "
                + "FROM public.roles r WHERE LOWER(r.nombre)='cajero' "
                + "AND NOT EXISTS (SELECT 1 FROM public.usuarios WHERE LOWER(usuario)='demo_cajero')",

            "INSERT INTO public.productos (nombre, id_categoria, precio_venta, precio_costo, stock, stock_minimo, unidad_medida, estado, id_unidad_medida) "
                + "SELECT d.nombre, c.id_categoria, d.precio, d.costo, d.stock, d.stock_minimo, d.unidad, "
                + "'Activo'::public.estado_general, u.id_unidad_medida "
                + "FROM (VALUES ('Arroz extra 1 kg','Abarrotes',4.50::numeric,3.60::numeric,28,5,'kg'), "
                + "('Leche evaporada 400 g','Lácteos',4.20::numeric,3.25::numeric,24,5,'und'), "
                + "('Agua sin gas 625 ml','Bebidas',1.50::numeric,0.85::numeric,35,5,'und'), "
                + "('Galletas de vainilla','Golosinas',2.00::numeric,1.30::numeric,20,5,'und'), "
                + "('Detergente en polvo 750 g','Limpieza',7.90::numeric,6.10::numeric,12,3,'und')) "
                + "AS d(nombre,categoria,precio,costo,stock,stock_minimo,unidad) "
                + "JOIN public.categorias c ON c.nombre=d.categoria "
                + "JOIN public.unidades_medida u ON u.abreviatura=d.unidad "
                + "WHERE NOT EXISTS (SELECT 1 FROM public.productos p WHERE p.nombre=d.nombre)",

            "INSERT INTO public.cajas (id_usuario, fecha_apertura, monto_inicial, fecha_cierre, monto_cierre_real, total_efectivo, total_billetera, estado, observacion) "
                + "SELECT u.id_usuario, CURRENT_TIMESTAMP - INTERVAL '1 day', 100.00, "
                + "CURRENT_TIMESTAMP - INTERVAL '1 day' + INTERVAL '8 hours', 109.00, 9.00, 0.00, "
                + "'Cerrada'::public.estado_caja, 'Caja de demostración inicial' "
                + "FROM public.usuarios u WHERE u.usuario='demo_cajero' "
                + "AND NOT EXISTS (SELECT 1 FROM public.cajas WHERE observacion='Caja de demostración inicial')",

            "INSERT INTO public.ventas (numero_ticket, id_caja, id_usuario, cliente, fecha_hora, subtotal, igv, total, metodo_pago, monto_recibido, vuelto, id_cliente, id_metodo_pago) "
                + "SELECT 'DEMO-0001', c.id_caja, u.id_usuario, 'Cliente de demostración', CURRENT_TIMESTAMP - INTERVAL '1 day', "
                + "9.00, 0.00, 9.00, 'Efectivo'::public.metodo_pago_tipo, 9.00, 0.00, cl.id_cliente, mp.id_metodo_pago "
                + "FROM public.cajas c JOIN public.usuarios u ON u.usuario='demo_cajero' "
                + "JOIN public.clientes cl ON cl.documento='00000001' "
                + "JOIN public.metodos_pago mp ON LOWER(mp.nombre)='efectivo' "
                + "WHERE NOT EXISTS (SELECT 1 FROM public.ventas WHERE numero_ticket='DEMO-0001') "
                + "ORDER BY c.id_caja LIMIT 1",

            "INSERT INTO public.detalle_ventas (id_venta, id_producto, cantidad, precio_unitario, subtotal) "
                + "SELECT v.id_venta, p.id_producto, 2, 4.50, 9.00 FROM public.ventas v "
                + "JOIN public.productos p ON p.nombre='Arroz extra 1 kg' "
                + "WHERE v.numero_ticket='DEMO-0001' AND NOT EXISTS "
                + "(SELECT 1 FROM public.detalle_ventas d WHERE d.id_venta=v.id_venta AND d.id_producto=p.id_producto)",

            "INSERT INTO public.movimientos_inventario (id_producto, id_usuario, tipo, cantidad, stock_anterior, stock_posterior, motivo, fecha_hora) "
                + "SELECT p.id_producto, u.id_usuario, 'Entrada'::public.tipo_movimiento, d.cantidad, 0, d.cantidad, "
                + "'Carga inicial de demostración', CURRENT_TIMESTAMP - INTERVAL '2 days' "
                + "FROM (VALUES ('Arroz extra 1 kg',30),('Leche evaporada 400 g',24),('Agua sin gas 625 ml',36), "
                + "('Galletas de vainilla',20),('Detergente en polvo 750 g',12)) AS d(producto,cantidad) "
                + "JOIN public.productos p ON p.nombre=d.producto "
                + "JOIN public.usuarios u ON u.usuario='demo_admin' "
                + "WHERE NOT EXISTS (SELECT 1 FROM public.movimientos_inventario m "
                + "WHERE m.id_producto=p.id_producto AND m.motivo='Carga inicial de demostración')"
        };

        try (Connection conexion = ConexionBD.getConexion()) {
            if (conexion == null) {
                throw new IllegalStateException("No se pudo conectar a PostgreSQL para cargar los datos iniciales.");
            }
            conexion.setAutoCommit(false);
            try (Statement sentencia = conexion.createStatement()) {
                for (String sql : inserts) sentencia.executeUpdate(sql);
                conexion.commit();
            } catch (SQLException e) {
                conexion.rollback();
                throw e;
            } finally {
                conexion.setAutoCommit(true);
            }
        } catch (SQLException e) {
            throw new IllegalStateException("No se pudieron cargar los datos iniciales: " + e.getMessage(), e);
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
