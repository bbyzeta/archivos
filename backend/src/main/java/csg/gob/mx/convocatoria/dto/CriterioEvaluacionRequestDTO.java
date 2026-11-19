package csg.gob.mx.convocatoria.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CriterioEvaluacionRequestDTO {

    @NotNull(message = "El ID de la convocatoria es obligatorio")
    private Long idConvocatoria;

    @NotBlank(message = "El nombre del criterio no puede estar vacío")
    @Size(max = 255, message = "El nombre no debe exceder los 255 caracteres")
    private String nombreCriterio;

    @NotBlank(message = "Los factores a considerar no pueden estar vacíos")
    @Size(max = 500, message = "Los factores no deben exceder los 500 caracteres")
    private String factoresConsiderar;

    @NotNull(message = "El porcentaje máximo es obligatorio")
    @Min(value = 1, message = "El porcentaje mínimo debe ser 1")
    @Max(value = 100, message = "El porcentaje máximo no debe ser mayor a 100")
    private Integer porcentajeMaximo;

    @NotNull(message = "El período es obligatorio")
    private Integer periodo;

    private Boolean activa = true;
}