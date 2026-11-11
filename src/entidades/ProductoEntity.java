package entidades;

import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity
@Table(name = "productos")
public class ProductoEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_producto")
    private Integer id;
    @Column(nullable = false, length = 150)
    private String nombre;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_categoria", nullable = false)
    private CategoriaEntity categoria;
    @Column(name = "precio_venta", nullable = false, precision = 12, scale = 2)
    private BigDecimal precioVenta;
    @Column(name = "precio_costo", nullable = false, precision = 12, scale = 2)
    private BigDecimal precioCosto;
    @Column(nullable = false)
    private Integer stock = 0;
    @Column(name = "stock_minimo", nullable = false)
    private Integer stockMinimo = 5;
    @Column(name = "unidad_medida", nullable = false, length = 30)
    private String unidadMedida;
    @Column(nullable = false, columnDefinition = "estado_general")
    private String estado;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_unidad_medida", nullable = false)
    private UnidadMedidaEntity unidad;
    protected ProductoEntity() {}
}
