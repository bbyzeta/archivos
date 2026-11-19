package csg.gob.mx.convocatoria.dto;


import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class CriterioEvaluadoDTO {

    private Integer idCriterio;
    private String nombreCriterio;
    private String factoresConsiderar;
    private Integer porcentajeMaximo;
    private Integer porcentajeAsignado;
    private String argumento;

    public CriterioEvaluadoDTO(
            Object idCriterio,
            String nombreCriterio,
            String factoresConsiderar,
            Number porcentajeMaximo,
            Number porcentajeAsignado,
            String argumento) {

        this.idCriterio = idCriterio != null ? ((Number) idCriterio).intValue() : null;
        this.nombreCriterio = nombreCriterio;
        this.factoresConsiderar = factoresConsiderar;
        this.porcentajeMaximo = porcentajeMaximo != null ? porcentajeMaximo.intValue() : 0;
        this.porcentajeAsignado = porcentajeAsignado != null ? porcentajeAsignado.intValue() : 0;
        this.argumento = argumento != null ? argumento : "";
    }
}