package csg.gob.mx.convocatoria.dto;


public interface CriterioEvaluacionProjection {
    Integer getIdCriterio();
    Long getIdConvocatoria();
    String getNombreCriterio();
    String getFactoresConsiderar();
    Integer getPorcentajeMaximo();
    Integer getPeriodo();
    Boolean getActiva();
    Integer getPorcentajeAsignado(); 
    Integer getCriterioEvaluado();
}