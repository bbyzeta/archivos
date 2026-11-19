package csg.gob.mx.convocatoria.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class DictamenExpedienteRequestDTO {
    @NotBlank(message = "El resultado es obligatorio (ACEPTADO, CON_OBSERVACIONES o RECHAZADO)")
    private String resultado;
    
    private String comentarios;
}