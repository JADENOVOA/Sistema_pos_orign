# Instalación y despliegue en Windows 11

Esta guía corresponde al proyecto actual: Java Swing, Java 17, PostgreSQL y clientes Windows conectados directamente al servidor de base de datos por la LAN. El proceso de recuperación envía un código SMTP real; si SMTP no está configurado, no envía nada y no completa la recuperación.

## 1. Preparar y migrar la base existente

1. Haga una copia de seguridad antes de cualquier cambio.
2. Complete el campo `email` en **Usuarios** para cada cuenta que necesite recuperación. Use direcciones confirmadas con cada operador y únicas; los correos antiguos quedan vacíos y no podrán recuperar la cuenta hasta completarse. El panel muestra `Correo confirmado`: pasa a `Sí` cuando la persona demuestra control del buzón al restablecer su contraseña con el código recibido.
3. Con pgAdmin o `psql`, abra la base correcta y ejecute `scripts/migracion_recuperacion_sesion.sql` con una cuenta administradora. El script es transaccional e idempotente. Si detecta direcciones repetidas, revierte el cambio: corrija los duplicados y vuelva a ejecutarlo.
4. Al desplegar la aplicación por primera vez con el rol de aplicación configurado, el inicio normal crea/actualiza ese rol con permisos de aplicación. Después, en cada cliente configure `db.bootstrap=false`; así Hibernate valida el esquema y no intenta crear tablas, catálogos, cuentas de demostración ni alterar PostgreSQL al iniciar.

El proyecto no borra tablas mediante esta migración. Aun así, no ejecute migraciones en la base de producción sin una copia que haya comprobado que se puede restaurar.

## 2. Servidor PostgreSQL

1. Instale PostgreSQL en la computadora servidor y manténgala encendida durante el horario de operación. Permita suspensión solo si sabe cómo afectará a los clientes.
2. En el router, reserve una dirección IPv4 para el servidor (por ejemplo, `192.168.1.10`) o configure una dirección fija fuera del rango DHCP. Use la misma red privada para las cajas.
3. En `postgresql.conf`, configure:

   ```conf
   listen_addresses = '*'
   port = 5432
   ssl = on
   ```

   Configure certificado y clave del servidor conforme a PostgreSQL. Para producción, use una CA confiable y un nombre DNS/IP que esté incluido en el certificado.
4. En `pg_hba.conf`, agregue una regla limitada a la subred real de la tienda, antes de reglas que la rechacen:

   ```conf
   hostssl  BODEGA_LAPERUANITA  pos_app  192.168.1.0/24  scram-sha-256
   ```

   Sustituya `192.168.1.0/24` por su subred; no use `0.0.0.0/0`, `trust` ni exponga el puerto a Internet. Reinicie el servicio PostgreSQL después de cambiar la configuración.
5. En Firewall de Windows, abra TCP 5432 solo para la red privada local. En PowerShell como administrador (reemplace el rango):

   ```powershell
   New-NetFirewallRule -DisplayName 'PostgreSQL POS LAN' -Direction Inbound -Action Allow -Protocol TCP -LocalPort 5432 -RemoteAddress 192.168.1.0/24 -Profile Private
   ```

6. Use un rol de aplicación dedicado, no `postgres`, para las cajas. El proceso de preparación del esquema usa la cuenta administradora solo durante la instalación inicial o tareas administrativas. **No copie la contraseña administradora a los clientes.**
7. Si no tiene una base que restaurar, el modo inicial `db.bootstrap=true` comprueba y crea la base configurada si falta (con una cuenta administradora que tenga permiso `CREATEDB`), y luego crea el esquema y catálogos. Revise y retire las cuentas/datos de demostración antes de operar; configure el usuario de aplicación y ejecute después la migración de este documento. No deje `db.bootstrap=true` en los clientes.

## 3. Configurar clientes

En cada PC cliente, cree `bodega.properties` junto a la carpeta portable (o en `%USERPROFILE%\.bodega_laperuanita\`). Ejemplo sin contraseñas:

```properties
db.url=jdbc:postgresql://bodega-db.local:5432/BODEGA_LAPERUANITA?sslmode=verify-full
db.app.user=pos_app
db.bootstrap=false
```

`bodega-db.local` debe resolver al servidor y estar incluido en el certificado TLS. Como alternativa temporal para una LAN de confianza, `sslmode=require` cifra el tráfico pero no verifica por sí solo que el servidor sea el correcto. La configuración actual del proyecto tiene un cambio local previo que deja el modo SSL predeterminado en `disable`; por ello, establezca explícitamente el modo TLS en la URL de producción.

### Secretos de base de datos y correo

- La aplicación migra las claves `db.admin.password`, `db.app.password` y `mail.smtp.password` que encuentre en `bodega.properties` a una bóveda cifrada con Windows DPAPI y las elimina del archivo una vez cifradas. La bóveda queda ligada al perfil de Windows de cada operador/PC y no debe copiarse a otra computadora.
- Para realizar esa migración antes de arrancar el POS, desde el directorio que contiene `bodega.properties` ejecute `java -cp target\bodega-la-peruanita-pos-1.0.0.jar util.BovedaSecretos --migrate` (en una distribución portable use la ruta Java/JAR que aparece abajo).
- Para agregar una contraseña sin ponerla en la línea de comandos, ejecute desde una consola del mismo usuario de Windows:

  ```powershell
  .\runtime\bin\java.exe -cp .\app\bodega-la-peruanita-pos-1.0.0.jar util.BovedaSecretos BODEGA_DB_PASSWORD
  ```

  Use `BODEGA_DB_ADMIN_PASSWORD` solo en el equipo administrativo durante una instalación controlada, y `BODEGA_SMTP_PASSWORD` si el proveedor elegido autentica con contraseña. También se pueden usar variables de entorno, que tienen precedencia.
- No coloque secretos en scripts, argumentos de procesos, USB, repositorio o archivos `.properties`. La cuenta SMTP se limita a enviar correo y no debe ser una cuenta personal con acceso a mensajes/archivos.

### Activar recuperación por correo

Se necesita un servidor SMTP o servicio transaccional, su host/puerto, usuario, remitente autorizado, clave de aplicación/API y TLS. Para Gmail se requiere una cuenta configurada y, si se usa contraseña de aplicación, la verificación en dos pasos habilitada; Google considera que las contraseñas de aplicación son menos recomendables que la autenticación moderna. Un proveedor transaccional puede ser más adecuado para mensajes automáticos. La contraseña se guarda en la bóveda DPAPI o se pasa por variable de entorno `BODEGA_SMTP_PASSWORD`.

En `bodega.properties` pueden declararse estos valores no secretos:

```properties
mail.smtp.host=smtp.example.com
mail.smtp.port=587
mail.smtp.user=pos@midominio.pe
mail.smtp.from=pos@midominio.pe
mail.smtp.starttls=true
mail.smtp.ssl=false
```

No se puede probar el envío hasta que configure un proveedor y una cuenta remitente válidos. El sistema no presenta éxito de entrega: informa que procesó la solicitud y que solo llegará un código si la cuenta existe y SMTP está configurado. Cada código vence en 15 minutos, se invalida al usarlo y no se guarda en claro.

## 4. Preparar memoria USB

1. En una computadora de compilación, instale JDK 17 o superior de 64 bits y compruebe `java -version` y `jpackage --version`.
2. Desde la carpeta del proyecto, ejecute `scripts\empaquetar_portable.ps1`. El paquete se genera en `dist\portable\build-<fecha>\BodegaLaPeruanitaPOS` con el runtime de Java incluido, por lo que el cliente no necesita NetBeans ni un JDK instalado. El código se compila para Java 17 y el runtime incluido coincide con el JDK usado para empaquetar.
3. Copie esa carpeta a la memoria USB. Mantenga la configuración no secreta en `bodega.properties` junto a la carpeta portable. La bóveda de cada usuario se genera en su perfil local, no en el USB.
4. Ejecute `BodegaLaPeruanitaPOS.exe`. Para guardar la clave DB en ese perfil, abra PowerShell en esa carpeta y ejecute el comando anterior. Use “Quitar hardware con seguridad” después de cerrar el sistema; el USB es solo medio de distribución, no almacenamiento de la base ni de logs/respaldo.

El paquete que ya existía en `dist/prueba` incluye un runtime Java 26. El script genera una distribución nueva y crea una configuración local sin contraseñas junto al ejecutable; guarde las claves necesarias con DPAPI en el equipo donde se ejecutará el POS.

## 5. Probar la conexión y operación

Desde cada cliente, en PowerShell:

```powershell
Test-NetConnection bodega-db.local -Port 5432
```

`TcpTestSucceeded` debe ser `True`. Si no, compruebe primero IP/DNS, red Wi-Fi/LAN, servicio PostgreSQL, `listen_addresses`, regla de `pg_hba.conf` y firewall. Después inicie el POS y confirme el estado de PostgreSQL en el pie de la ventana.

Antes de operar, realice en un entorno de prueba una venta con varios productos, revise el stock y el movimiento de inventario, consulte reportes y cierre/reabra caja. Verifique que el registro de venta y su inventario se reflejan desde otra estación. Pruebe la recuperación solo con una cuenta y buzón controlados.

El POS escribe directamente en PostgreSQL: **no registra ventas sin conexión**. Si se interrumpe la red, las operaciones que requieran datos deben fallar y el usuario debe reconectar antes de reintentar; confirme en la base que una venta no se duplicó. No mantenga abierta una caja con operaciones pendientes fuera de línea.

## 6. Respaldos y restauración

1. En el servidor, cree un rol dedicado `pos_backup`, asígnele una contraseña con la interfaz segura de pgAdmin o el comando interactivo `\password pos_backup`, y concédale solo conexión y lectura:

   ```sql
   GRANT CONNECT ON DATABASE BODEGA_LAPERUANITA TO pos_backup;
   GRANT USAGE ON SCHEMA public TO pos_backup;
   GRANT SELECT ON ALL TABLES IN SCHEMA public TO pos_backup;
   GRANT SELECT ON ALL SEQUENCES IN SCHEMA public TO pos_backup;
   ```

2. Copie la carpeta portable al disco local del servidor. En `bodega.properties` configure también `db.bootstrap=false` y los ajustes de conexión que use la aplicación. Abra una consola con el mismo usuario de Windows que ejecutará la tarea y guarde la contraseña de respaldo con DPAPI:

   ```powershell
   .\runtime\bin\java.exe -cp .\app\bodega-la-peruanita-pos-1.0.0.jar util.BovedaSecretos BODEGA_DB_BACKUP_PASSWORD
   ```

3. Defina `BODEGA_POS_HOME` como la carpeta portable local del servidor. Ajuste `BODEGA_PG_DUMP` si PostgreSQL no está instalado en la ruta predeterminada. Ejecute `scripts\respaldo_programado.bat` una vez a mano y compruebe que genera un `.dump`. El script no sobrescribe respaldos y no contiene la contraseña; `pg_dump` la recibe del proceso Java desde DPAPI.
4. En el Programador de tareas de Windows, cree una tarea diaria para `respaldo_programado.bat`, con el mismo usuario de Windows que guardó el secreto DPAPI. Guarde copias en un segundo disco, NAS o medio desconectado; no dependa solo del servidor ni del USB de distribución.

Para restaurar, cree una base nueva vacía y use `pg_restore` sobre esa base; confirme conteos y operaciones antes de volver a conectarle los POS. No restaure con limpieza destructiva sobre la base activa sin respaldo y ventana de mantenimiento. Pruebe periódicamente la restauración en una base aparte.

## 7. Límites conocidos de esta entrega

- SMTP real depende de las credenciales y dominio que configure el propietario. No se implementa SMS/WhatsApp en esta etapa.
- “Recordarme” almacena un token aleatorio solo como hash en PostgreSQL y protegido por DPAPI en el equipo; vence en 14 días. Cerrar sesión, la inactividad o restablecer la contraseña revoca el token.
- El acceso directo cliente-PostgreSQL funciona en una LAN pequeña, pero distribuye credenciales de base de datos a las cajas. Para una expansión a varias sedes o acceso por Internet, conviene poner un servicio de aplicación autenticado entre las cajas y la base.
