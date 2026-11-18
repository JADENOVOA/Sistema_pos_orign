# TLS para PostgreSQL en Windows

Para una base en otra computadora, use TLS y restrinja el acceso a la LAN. En producción el modo recomendado para pgJDBC es `sslmode=verify-full`: cifra y verifica que el certificado corresponda al servidor.

1. Instale en PostgreSQL un certificado firmado por una CA que controle la tienda. Incluya el DNS/IP de conexión en el SAN del certificado. Configure `postgresql.conf`:

   ```conf
   ssl = on
   ssl_cert_file = 'server.crt'
   ssl_key_file = 'server.key'
   ```

   La cuenta del servicio PostgreSQL debe poder leer la clave privada. Reinicie el servicio y compruebe en pgAdmin que `pg_stat_ssl.ssl` sea `true`.
2. Instale el certificado raíz de esa CA en cada cliente, por ejemplo `C:\BodegaPOS\certs\bodega-ca.crt`, y manténgalo fuera del USB si la política local así lo requiere.
3. Configure en `bodega.properties` una URL con el nombre cubierto por el certificado:

   ```properties
   db.url=jdbc:postgresql://bodega-db.local:5432/BODEGA_LAPERUANITA?sslmode=verify-full&sslrootcert=C%3A%2FBodegaPOS%2Fcerts%2Fbodega-ca.crt
   ```

4. En `pg_hba.conf`, use una regla `hostssl` limitada a la subred de la tienda, método `scram-sha-256`, y permita TCP/5432 solo desde esa subred en el firewall.

`sslmode=require` cifra el tráfico pero no verifica por sí solo la identidad del servidor; puede servir como transición en una LAN controlada. No use `sslmode=disable` para clientes remotos. En modo de despliegue (`db.bootstrap=false`), la aplicación rechaza los modos `disable`, `allow` y `prefer`.

En desarrollo local sin TLS se conserva el modo `disable` como valor predeterminado. No use esa configuración al distribuir a otras computadoras.
