package csg.gob.mx.convocatoria.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConvocatoriaGeneralResponseDTO {
    private Long id;
    private String titulo;
    private String descripcion;
    private Boolean activa;
}