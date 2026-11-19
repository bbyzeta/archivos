package csg.gob.mx.convocatoria.entity;


import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Entity
@Table(name = "estados")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class EstadoEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_estado")
    private Long idEstado;

    @Column(name = "codigo", nullable = false, length = 5, unique = true)
    private String codigo;

    @Column(name = "nombre", nullable = false, length = 100)
    private String nombre;
}