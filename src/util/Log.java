package util;

import java.io.IOException;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

/** Registro interno de errores: el detalle técnico queda en un archivo, nunca en la pantalla. */
public final class Log {

    private static final Logger LOGGER = Logger.getLogger("bodega");

    static {
        try {
            LOGGER.setUseParentHandlers(false);
            FileHandler fh = new FileHandler(
                    Config.carpetaDatos().resolve("sistema.log").toString(), 1_000_000, 5, true);
            fh.setFormatter(new SimpleFormatter());
            LOGGER.addHandler(fh);
        } catch (IOException | RuntimeException e) {
            System.err.println("No se pudo crear el archivo de log interno.");
        }
    }

    private Log() {}

    /** Mensaje genérico que se muestra al usuario ante cualquier falla interna. */
    public static final String MENSAJE_GENERICO =
            "No se pudo completar la operación. Intente nuevamente o contacte al administrador.";

    public static void error(Throwable t) { error("Error", t); }

    public static void error(String contexto, Throwable t) {
        LOGGER.log(Level.SEVERE, contexto, t);
    }

    public static void info(String mensaje) { LOGGER.log(Level.INFO, mensaje); }
}
