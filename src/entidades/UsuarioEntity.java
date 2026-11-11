package entidades;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "usuarios")
public class UsuarioEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_usuario")
    private Integer id;
    @Column(name = "nombre_completo", nullable = false, length = 150)
    private String nombreCompleto;
    @Column(nullable = false, length = 60)
    private String usuario;
    @Column(nullable = false, length = 255)
    private String password;
    @Column(nullable = false, columnDefinition = "rol_usuario")
    private String rol;
    @Column(nullable = false, columnDefinition = "estado_general")
    private String estado;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_rol", nullable = false)
    private RolEntity rolRelacion;
    @Column(name = "intentos_fallidos", nullable = false)
    private Integer intentosFallidos = 0;
    @Column(name = "bloqueado_hasta")
    private LocalDateTime bloqueadoHasta;
    protected UsuarioEntity() {}
}
