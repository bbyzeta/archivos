package csg.gob.mx.convocatoria.dto;

import java.time.LocalDateTime;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter 
@Setter 
@AllArgsConstructor
@Builder
public class PostulacionResponseDTO {
    private Long id;
    private Long idConvocatoria;
    private Long idUsuario;
    private String folio;
    private String estado;
    private String tituloProyecto;
    private String resumenEjecutivo;
    private LocalDateTime fechaCreacion;
    private LocalDateTime fechaEnvio;
    private List<DocumentoAdjuntoDTO> documentos;
    private List<AnexoAdjuntoDTO> anexos;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DocumentoAdjuntoDTO {
        private Long id;
        private Long idRequisito;
        private String nombreRequisito;
        private Boolean esAnexo;
        private String nombreOriginal;
        private String rutaArchivo;
        private LocalDateTime fechaCarga;
        private String estadoRevision;
        private String observacionAuditor;
    }
    
    @Getter @Setter @AllArgsConstructor @NoArgsConstructor @Builder
    public static class AnexoAdjuntoDTO {
        private Long id;
        private Long idConvocatoria;
        private String nombreAnexo;
        private String nombreOriginal;
        private String rutaArchivo;
        private LocalDateTime fechaCarga;
        private String estadoRevision;
        private String observacionAuditor;
    }
}