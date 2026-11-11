package entidades;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "cajas")
public class CajaEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_caja")
    private Integer id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_usuario", nullable = false)
    private UsuarioEntity usuario;
    @Column(name = "fecha_apertura", nullable = false, columnDefinition = "timestamp default CURRENT_TIMESTAMP")
    private LocalDateTime fechaApertura;
    @Column(name = "monto_inicial", nullable = false, precision = 12, scale = 2)
    private BigDecimal montoInicial;
    @Column(name = "fecha_cierre")
    private LocalDateTime fechaCierre;
    @Column(name = "monto_cierre_real", precision = 12, scale = 2)
    private BigDecimal montoCierreReal;
    @Column(name = "total_efectivo", nullable = false, precision = 12, scale = 2)
    private BigDecimal totalEfectivo = BigDecimal.ZERO;
    @Column(name = "total_billetera", nullable = false, precision = 12, scale = 2)
    private BigDecimal totalBilletera = BigDecimal.ZERO;
    @Column(nullable = false, columnDefinition = "estado_caja")
    private String estado;
    private String observacion;
    private String observaciones;
    protected CajaEntity() {}
}
