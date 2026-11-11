package entidades;

import jakarta.persistence.*;

@Entity
@Table(name = "metodos_pago")
public class MetodoPagoEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_metodo_pago")
    private Integer id;
    @Column(nullable = false, length = 60)
    private String nombre;
    @Column(nullable = false)
    private Boolean estado = true;
    protected MetodoPagoEntity() {}
}
