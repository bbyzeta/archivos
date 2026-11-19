package csg.gob.mx.convocatoria.dto;

import lombok.Data;

@Data
public class ArgumentoPostulanteRequestDTO {
    private Long idConvocatoria;
    private Integer idCriterio;
    private Long idUsuario;
    private Long idJuez;
    private String argumento;
}