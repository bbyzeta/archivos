package csg.gob.mx.convocatoria.dto;

import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Builder
public class JuradoResponseDTO {
    private Integer idJurado;
    private Integer idUsuario;
    private String gradoAcademico;
    private String nombres;
    private String apellidos;
    private String rfc;
    private String telefono;
    private String username;
    private String institucionAdscripcion;
    private String cargoActual;
    private String areaConocimiento;
    private String orcidUrl;
    private String lineasInvestigacion;
    private Boolean aceptoConflictoInteres;
    private Boolean aceptoConfidencialidad;
    private Boolean activo;
    private LocalDateTime fechaRegistro;
}