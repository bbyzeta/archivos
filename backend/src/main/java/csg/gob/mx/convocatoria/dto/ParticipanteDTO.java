package csg.gob.mx.convocatoria.dto;

import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ParticipanteDTO {

    private Long idParticipante;
    private Long idUsuario;
    private String nombres;
    private String apellidos;
    private String curp;
    private String telefono;
    private String username;
    private Boolean activo;
    private Boolean aceptoTerminos;
    private LocalDateTime fechaRegistro;
    private DatosInstitucionalesDTO datosInstitucionales;
}