package entidades;

import jakarta.persistence.*;

@Entity
@Table(name = "categorias")
public class CategoriaEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_categoria")
    private Integer id;
    @Column(nullable = false, length = 100)
    private String nombre;
    protected CategoriaEntity() {}
}
