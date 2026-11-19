package csg.gob.mx.convocatoria.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DatosInstitucionalesDTO {

    private Long idInstitucional;

    @NotNull(message = "El idEstado es obligatorio")
    private Long idEstado;

    private String nombreEstado;

    @NotBlank(message = "La institución es obligatoria")
    private String institucion;

    @NotBlank(message = "El cargo es obligatorio")
    private String cargo;
}