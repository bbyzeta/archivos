package csg.gob.mx.convocatoria.entity;


import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "evaluacion_postulante")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EvaluacionPostulanteEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_evaluacion")
    private Long idEvaluacion;

    @Column(name = "id_expediente", nullable = false)
    private Long idExpediente;

    @Column(name = "id_criterio", nullable = false)
    private Integer idCriterio;

    @Column(name = "id_usuario", nullable = false)
    private Long idUsuario;

    @Column(name = "porcentaje_asignado", nullable = false)
    private Integer porcentajeAsignado;

    @Column(name = "fecha_evaluacion", insertable = false, updatable = false)
    private LocalDateTime fechaEvaluacion;
    
    @Column(name="criterio_evaluado")
    private Integer criterioEvaluado;
}