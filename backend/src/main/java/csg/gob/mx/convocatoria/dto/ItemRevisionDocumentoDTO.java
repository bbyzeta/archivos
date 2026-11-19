package csg.gob.mx.convocatoria.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ItemRevisionDocumentoDTO {
    private Long idDocumento;
    private String nombre;
    private Boolean esAnexo;
    private String nombreOriginal;
    private String rutaArchivo;
    private String estadoRevision;
    private String observacionAuditor;
    private Integer corregido;
}