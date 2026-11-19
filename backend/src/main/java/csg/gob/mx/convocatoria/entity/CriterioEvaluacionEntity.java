package csg.gob.mx.convocatoria.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "criterios_evaluacion")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CriterioEvaluacionEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_criterio")
    private Integer idCriterio;

    @Column(name = "id_convocatoria", nullable = false)
    private Long idConvocatoria;

    @Column(name = "nombre_criterio", nullable = false, length = 255)
    private String nombreCriterio;

    @Column(name = "factores_considerar", nullable = false, length = 500)
    private String factoresConsiderar;

    @Column(name = "porcentaje_maximo", nullable = false)
    private Integer porcentajeMaximo;

    @Column(name = "periodo", nullable = false)
    private Integer periodo;

    @Column(name = "activa", nullable = false)
    @Builder.Default
    private Boolean activa = true;
}