package csg.gob.mx.convocatoria.dto;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;


@Data
@AllArgsConstructor
@NoArgsConstructor
public class ConvocatoriaJuradoDTO {

	    private Integer id;
	    private Integer idConvocatoria;
	    private Integer idJurado;
	    private String estadoInvitacion;
	    private LocalDateTime fechaAsignacion = LocalDateTime.now();
}
