package csg.gob.mx.convocatoria.dto;


import java.util.List;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class DictamenReporteDTO {
    private Long idConvocatoria;
    private String tituloConvocatoria;
    private Integer periodo;
    private Long idJuez;
    private String nombreJuez;
    private Long idPostulante;
    private String nombrePostulante;
    private String curpPostulante;
    private Integer porcentajeTotal;
    private Integer estadoEvaluacion;
    private String argumentoGeneral;
    private List<CriterioEvaluadoDTO> criterios;

    public DictamenReporteDTO() {}

    public DictamenReporteDTO(Long idConvocatoria, String tituloConvocatoria, Integer periodo, 
                              Long idJuez, String nombreJuez, Long idPostulante, 
                              String nombrePostulante, String curpPostulante, 
                              Integer porcentajeTotal, Integer estadoEvaluacion, String argumentoGeneral) {
        this.idConvocatoria = idConvocatoria;
        this.tituloConvocatoria = tituloConvocatoria;
        this.periodo = periodo;
        this.idJuez = idJuez;
        this.nombreJuez = nombreJuez;
        this.idPostulante = idPostulante;
        this.nombrePostulante = nombrePostulante;
        this.curpPostulante = curpPostulante;
        this.porcentajeTotal = porcentajeTotal;
        this.estadoEvaluacion = estadoEvaluacion;
        this.argumentoGeneral = argumentoGeneral;
    }

    
}
