package util;

import jakarta.mail.Authenticator;
import jakarta.mail.Message;
import jakarta.mail.PasswordAuthentication;
import jakarta.mail.Session;
import jakarta.mail.Transport;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import java.nio.charset.StandardCharsets;
import java.util.Properties;

/** Envío SMTP real. No registra códigos ni credenciales y no tiene modo simulado. */
public final class CorreoRecuperacion {
    private CorreoRecuperacion() {}

    public static void enviar(String destino, String nombre, String codigo) throws Exception {
        String host = Config.get("BODEGA_SMTP_HOST", "mail.smtp.host", null);
        String puerto = Config.get("BODEGA_SMTP_PORT", "mail.smtp.port", "587");
        String usuario = Config.get("BODEGA_SMTP_USER", "mail.smtp.user", null);
        String remitente = Config.get("BODEGA_SMTP_FROM", "mail.smtp.from", usuario);
        String clave = Config.get("BODEGA_SMTP_PASSWORD", "mail.smtp.password", null);
        String startTls = Config.get("BODEGA_SMTP_STARTTLS", "mail.smtp.starttls", "true");
        String ssl = Config.get("BODEGA_SMTP_SSL", "mail.smtp.ssl", "false");
        if (host == null || usuario == null || remitente == null || clave == null || clave.isBlank()) {
            throw new IllegalStateException("Falta configurar BODEGA_SMTP_HOST, BODEGA_SMTP_USER, BODEGA_SMTP_FROM y BODEGA_SMTP_PASSWORD.");
        }
        if (!Boolean.parseBoolean(startTls) && !Boolean.parseBoolean(ssl)) {
            throw new IllegalStateException("El correo de recuperación requiere TLS SMTP (STARTTLS o SSL).");
        }
        Properties props = new Properties();
        props.setProperty("mail.smtp.host", host);
        props.setProperty("mail.smtp.port", puerto);
        props.setProperty("mail.smtp.auth", "true");
        props.setProperty("mail.smtp.starttls.enable", Boolean.toString(Boolean.parseBoolean(startTls)));
        props.setProperty("mail.smtp.starttls.required", Boolean.toString(Boolean.parseBoolean(startTls)));
        props.setProperty("mail.smtp.ssl.enable", Boolean.toString(Boolean.parseBoolean(ssl)));
        props.setProperty("mail.smtp.connectiontimeout", "10000");
        props.setProperty("mail.smtp.timeout", "10000");
        props.setProperty("mail.smtp.writetimeout", "10000");
        Session session = Session.getInstance(props, new Authenticator() {
            @Override protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(usuario, clave);
            }
        });
        MimeMessage mensaje = new MimeMessage(session);
        mensaje.setFrom(new InternetAddress(remitente));
        mensaje.setRecipient(Message.RecipientType.TO, new InternetAddress(destino));
        mensaje.setSubject("Código para recuperar tu cuenta - Bodega La Peruanita", StandardCharsets.UTF_8.name());
        mensaje.setText("Hola " + (nombre == null ? "" : nombre) + ",\n\n"
                + "Tu código temporal para cambiar la contraseña es: " + codigo + "\n"
                + "Vence en 15 minutos y solo puede utilizarse una vez. Si no solicitaste este cambio, ignora este mensaje.\n",
                StandardCharsets.UTF_8.name());
        Transport.send(mensaje);
    }
}
