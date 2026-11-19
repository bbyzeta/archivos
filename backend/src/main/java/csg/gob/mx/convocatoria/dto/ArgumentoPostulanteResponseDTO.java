package csg.gob.mx.convocatoria.dto;

import java.time.LocalDateTime;

import lombok.Data;

@Data
public class ArgumentoPostulanteResponseDTO {
    private Long idArgumento;
    private Long idConvocatoria;
    private Integer idCriterio;
    private Long idUsuario;
    private Long idJuez;
    private String argumento;
    private LocalDateTime fechaRegistro;
}