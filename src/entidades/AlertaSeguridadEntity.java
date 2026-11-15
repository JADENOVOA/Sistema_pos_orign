package entidades;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/** Alertas dirigidas al administrador (ej.: cuenta bloqueada por intentos fallidos). */
@Entity
@Table(name = "alertas_seguridad")
public class AlertaSeguridadEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_alerta")
    private Integer id;
    @Column(name = "fecha_hora", nullable = false, columnDefinition = "timestamp default CURRENT_TIMESTAMP")
    private LocalDateTime fechaHora;
    @Column(nullable = false, length = 40)
    private String tipo;
    @Column(name = "usuario_afectado", length = 60)
    private String usuarioAfectado;
    @Column(nullable = false, length = 255)
    private String mensaje;
    @Column(nullable = false, columnDefinition = "boolean default false")
    private Boolean atendida = false;
    protected AlertaSeguridadEntity() {}
}
