package csg.gob.mx.convocatoria.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.util.List;

@Data
public class AsignacionJuradosDTO {
    @NotNull(message = "La lista de jurados no puede ser nula")
    private List<Integer> juradoIds;
}