package csg.gob.mx.convocatoria.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class EstadoJuradoDTO {
    @NotNull
    private Boolean activo;
}