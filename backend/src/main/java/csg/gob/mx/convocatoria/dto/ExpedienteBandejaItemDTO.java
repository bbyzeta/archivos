package csg.gob.mx.convocatoria.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ExpedienteBandejaItemDTO {

    private Long idExpediente;
    private String folio;
    private Long idConvocatoria;
    private String tituloConvocatoria;
    private Integer periodo;
    private Long idPostulante;
    private String nombrePostulante;
    private String correoPostulante;
    private String curpPostulante;
    private String institucionPostulante;
    private String estadoExpediente; 
    private Integer totalDocumentos;
    private Integer documentosAprobados;
    private LocalDateTime fechaEnvio;
}