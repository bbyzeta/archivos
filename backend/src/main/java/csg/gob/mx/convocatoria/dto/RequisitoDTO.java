package csg.gob.mx.convocatoria.dto;

import lombok.Data;

@Data
public class RequisitoDTO {
	private Long idRequisito;
    private String nombre;
    private Boolean esAnexo;
    private Integer porcentajeMinimo;
    private Integer porcentajeMaximo;
}