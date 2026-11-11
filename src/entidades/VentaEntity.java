package entidades;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "ventas")
public class VentaEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_venta")
    private Integer id;
    @Column(name = "numero_ticket", nullable = false, length = 40)
    private String numeroTicket;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_caja", nullable = false)
    private CajaEntity caja;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_usuario", nullable = false)
    private UsuarioEntity usuario;
    @Column(nullable = false, length = 150)
    private String cliente = "Cliente Mostrador";
    @Column(name = "fecha_hora", nullable = false, columnDefinition = "timestamp default CURRENT_TIMESTAMP")
    private LocalDateTime fechaHora;
    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal subtotal;
    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal igv;
    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal total;
    @Column(name = "metodo_pago", nullable = false, columnDefinition = "metodo_pago_tipo")
    private String metodoPago;
    @Column(name = "monto_recibido", nullable = false, precision = 12, scale = 2)
    private BigDecimal montoRecibido;
    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal vuelto = BigDecimal.ZERO;
    @Column(name = "id_cliente")
    private Integer idCliente;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_metodo_pago")
    private MetodoPagoEntity metodoPagoRelacion;
    protected VentaEntity() {}
}
