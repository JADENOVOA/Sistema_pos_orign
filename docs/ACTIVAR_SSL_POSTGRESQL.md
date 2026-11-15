# Activar SSL/TLS en PostgreSQL (Windows)

El sistema se conecta con `sslmode=require`. Si su PostgreSQL no tiene SSL activo, el sistema no inicia.

1. Abra Git Bash/OpenSSL y, en la carpeta `data` de PostgreSQL (ej. `C:\Program Files\PostgreSQL\17\data`):
   `openssl req -new -x509 -days 365 -nodes -text -out server.crt -keyout server.key -subj "/CN=localhost"`
2. Edite `postgresql.conf`: `ssl = on` (y `ssl_cert_file='server.crt'`, `ssl_key_file='server.key'`).
3. Reinicie el servicio "postgresql-x64-17" (services.msc).
4. Compruebe en pgAdmin: `SELECT ssl, version FROM pg_stat_ssl WHERE pid = pg_backend_pid();` -> `true | TLSv1.3`.

Solo para pruebas locales sin SSL: en `bodega.properties` ponga `db.sslmode=disable` (no usar en producción).
