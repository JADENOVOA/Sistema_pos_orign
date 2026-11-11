package entidades;

import jakarta.persistence.*;

@Entity
@Table(name = "unidades_medida")
public class UnidadMedidaEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_unidad_medida")
    private Integer id;
    @Column(nullable = false, length = 20)
    private String abreviatura;
    @Column(nullable = false, length = 60)
    private String nombre;
    protected UnidadMedidaEntity() {}
}
