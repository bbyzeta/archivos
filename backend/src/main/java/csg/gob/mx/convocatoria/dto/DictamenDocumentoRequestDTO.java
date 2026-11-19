package csg.gob.mx.convocatoria.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class DictamenDocumentoRequestDTO {
    @NotBlank(message = "El estado de revisión es obligatorio (APROBADO o RECHAZADO)")
    private String estado;
    
    private String observacion;
}