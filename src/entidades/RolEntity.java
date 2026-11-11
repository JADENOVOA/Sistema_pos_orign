package entidades;

import jakarta.persistence.*;

@Entity
@Table(name = "roles")
public class RolEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_rol")
    private Integer id;
    @Column(nullable = false, length = 60)
    private String nombre;
    private String descripcion;
    protected RolEntity() {}
}
