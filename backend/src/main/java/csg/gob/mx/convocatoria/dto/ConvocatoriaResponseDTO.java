package csg.gob.mx.convocatoria.dto;

import java.time.LocalDateTime;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConvocatoriaResponseDTO {
    private Long id;
    private String titulo;
    private String subtitulo;
    private String tituloBlog;
    private LocalDateTime fechaApertura;
    private LocalDateTime fechaCierre;
    private String contenidoBlog;
    private String rutaImagenHeader;
    private String rutaPdfBases;
    public Integer periodo;
    private Boolean activa;
    private List<RequisitoDTO> requisitos;
    private List<GanadorDTO> ganadoresHistoricos;
    public Integer idGrupoPremio;
}