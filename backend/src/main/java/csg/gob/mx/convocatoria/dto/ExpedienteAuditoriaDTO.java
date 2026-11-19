package csg.gob.mx.convocatoria.dto;

import java.time.LocalDateTime;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ExpedienteAuditoriaDTO {
    private Long idExpediente;
    private String folio;
    private String estadoExpediente;
    private LocalDateTime fechaEnvio;

    private Long idPostulante;
    private String nombrePostulante;
    private String correoPostulante;
    private String curpPostulante;
    private String telefonoPostulante;
    private String institucionPostulante;

    private Long idConvocatoria;
    private String tituloConvocatoria;
    private Integer periodoConvocatoria;
    private LocalDateTime fechaApertura;
    private LocalDateTime fechaCierre;
    private String rutaImagenPremio;

    private List<ItemRevisionDocumentoDTO> documentos;
}