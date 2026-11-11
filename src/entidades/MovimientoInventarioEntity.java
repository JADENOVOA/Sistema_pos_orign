package entidades;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "movimientos_inventario")
public class MovimientoInventarioEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_movimiento")
    private Integer id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_producto", nullable = false)
    private ProductoEntity producto;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_usuario", nullable = false)
    private UsuarioEntity usuario;
    @Column(nullable = false, columnDefinition = "tipo_movimiento")
    private String tipo;
    @Column(nullable = false)
    private Integer cantidad;
    @Column(name = "stock_anterior", nullable = false)
    private Integer stockAnterior;
    @Column(name = "stock_posterior", nullable = false)
    private Integer stockPosterior;
    @Column(nullable = false, length = 255)
    private String motivo;
    @Column(name = "fecha_hora", nullable = false, columnDefinition = "timestamp default CURRENT_TIMESTAMP")
    private LocalDateTime fechaHora;
    protected MovimientoInventarioEntity() {}
}
