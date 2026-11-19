package csg.gob.mx.convocatoria.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "argumentos_postulante")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ArgumentoPostulanteEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_argumento")
    private Long idArgumento;

    @Column(name = "id_convocatoria", nullable = false)
    private Long idConvocatoria;

    @Column(name = "id_criterio", nullable = false)
    private Integer idCriterio;

    @Column(name = "id_usuario", nullable = false)
    private Long idUsuario;

    @Column(name = "id_juez", nullable = false)
    private Long idJuez;

    @Column(name = "argumento", nullable = false, length = 1000)
    private String argumento;

    @Column(name = "fecha_registro", insertable = false, updatable = false)
    @UpdateTimestamp
    private LocalDateTime fechaRegistro;
}