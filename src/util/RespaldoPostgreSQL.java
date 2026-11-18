package util;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/** Ejecuta pg_dump con una contraseña leída de DPAPI y pasada al proceso hijo solo en memoria. */
public final class RespaldoPostgreSQL {
    private RespaldoPostgreSQL() {}

    public static void main(String[] args) throws Exception {
        if (args.length != 6) {
            System.err.println("Uso: RespaldoPostgreSQL HOST PUERTO USUARIO BASE ARCHIVO PG_DUMP_EXE");
            System.exit(2);
        }
        String secreto = BovedaSecretos.obtener("BODEGA_DB_BACKUP_PASSWORD");
        if (secreto == null || secreto.isBlank()) {
            throw new IllegalStateException("Falta guardar BODEGA_DB_BACKUP_PASSWORD en la bóveda DPAPI del usuario de la tarea.");
        }
        Path archivo = Path.of(args[4]).toAbsolutePath();
        if (Files.exists(archivo)) throw new IllegalStateException("El archivo de respaldo ya existe; no se sobrescribirá: " + archivo);
        Files.createDirectories(archivo.getParent());
        List<String> comando = new ArrayList<>();
        comando.add(args[5]); comando.add("-h"); comando.add(args[0]);
        comando.add("-p"); comando.add(args[1]); comando.add("-U"); comando.add(args[2]);
        comando.add("-Fc"); comando.add("-f"); comando.add(archivo.toString()); comando.add(args[3]);
        ProcessBuilder pb = new ProcessBuilder(comando).inheritIO();
        pb.environment().put("PGPASSWORD", secreto);
        pb.environment().put("PGSSLMODE", "require");
        int resultado = pb.start().waitFor();
        pb.environment().remove("PGPASSWORD");
        if (resultado != 0) {
            Files.deleteIfExists(archivo);
            throw new IllegalStateException("pg_dump terminó con código " + resultado + "; revise el log de PostgreSQL y las credenciales de respaldo.");
        }
        System.out.println("Respaldo PostgreSQL completado: " + archivo);
    }
}
