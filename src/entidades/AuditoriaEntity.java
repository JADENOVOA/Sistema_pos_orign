package entidades;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/** Registro de auditoría: solo se inserta y se consulta; un trigger impide editarlo o borrarlo. */
@Entity
@Table(name = "auditoria")
public class AuditoriaEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_auditoria")
    private Integer id;
    @Column(name = "fecha_hora", nullable = false, columnDefinition = "timestamp default CURRENT_TIMESTAMP")
    private LocalDateTime fechaHora;
    @Column(name = "id_usuario")
    private Integer idUsuario;
    @Column(nullable = false, length = 60)
    private String usuario;
    @Column(nullable = false, length = 40)
    private String modulo;
    @Column(nullable = false, length = 60)
    private String accion;
    @Column(length = 255)
    private String detalle;
    @Column(length = 100)
    private String equipo;
    protected AuditoriaEntity() {}
}
