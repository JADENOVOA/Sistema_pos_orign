package entidades;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "clientes")
public class ClienteEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_cliente")
    private Integer id;
    @Column(nullable = false, length = 150)
    private String nombre;
    @Column(length = 30)
    private String documento;
    @Column(length = 30)
    private String telefono;
    @Column(nullable = false)
    private Boolean estado = true;
    @Column(name = "fecha_registro", nullable = false, columnDefinition = "timestamp default CURRENT_TIMESTAMP")
    private LocalDateTime fechaRegistro;
    protected ClienteEntity() {}
}
