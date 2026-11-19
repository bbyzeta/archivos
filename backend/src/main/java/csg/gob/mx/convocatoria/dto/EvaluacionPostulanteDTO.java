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
public class EvaluacionPostulanteDTO {

	private Integer idEvaluacion;
	private Integer idExpediente;
	private Integer idCriterio;
	private Integer idUsuario;
	private Integer porcentajeAsignado;
	private Integer criterioEvaluado;
	private LocalDateTime fechaEvaluacion;
	
}