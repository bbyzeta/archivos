package csg.gob.mx.convocatoria.dto;

import lombok.Data;

@Data
public class PostulacionRegistroDTO {
    private Long idConvocatoria;
    private Long idUsuario;
    private String tituloProyecto;
    private String resumenEjecutivo;
}