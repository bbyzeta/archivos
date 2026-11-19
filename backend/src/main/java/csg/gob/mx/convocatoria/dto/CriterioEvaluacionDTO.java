package csg.gob.mx.convocatoria.dto;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CriterioEvaluacionDTO {
    private Integer idCriterio;
    private Long idConvocatoria;
    private String nombreCriterio;
    private String factoresConsiderar;
    private Integer porcentajeMaximo;
    private Integer periodo;
    private Boolean activa;
    private Integer porcentajeAsignado;
    private Integer criterioEvaluado;
}