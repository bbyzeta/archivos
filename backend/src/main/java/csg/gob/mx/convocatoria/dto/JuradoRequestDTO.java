package csg.gob.mx.convocatoria.dto;

import jakarta.validation.constraints.*;
import lombok.Data;

@Data
public class JuradoRequestDTO {

    private Integer idJurado;

    @NotBlank(message = "El grado académico es obligatorio")
    private String gradoAcademico;

    @NotBlank(message = "El nombre es obligatorio")
    @Size(max = 100)
    private String nombres;

    @NotBlank(message = "Los apellidos son obligatorios")
    @Size(max = 100)
    private String apellidos;

    @Pattern(regexp = "^[A-Z&Ñ]{3,4}[0-9]{6}[A-V1-9][A-Z1-9][0-9A]?$", message = "Estructura de RFC inválida")
    private String rfc;

    @NotBlank(message = "El teléfono es obligatorio")
    @Pattern(regexp = "^[0-9]{10}$", message = "El teléfono debe contener exactamente 10 dígitos")
    private String telefono;

    @NotBlank(message = "El correo institucional es obligatorio")
    @Email(message = "El correo debe ser válido")
    private String username;

    private String password;

    @NotBlank(message = "La institución de adscripción es obligatoria")
    private String institucionAdscripcion;

    @NotBlank(message = "El cargo actual es obligatorio")
    private String cargoActual;

    @NotBlank(message = "El área de conocimiento es obligatoria")
    private String areaConocimiento;

    private String orcidUrl;

    @NotBlank(message = "Las líneas de investigación son obligatorias")
    private String lineasInvestigacion;

    @AssertTrue(message = "Debe aceptar el manifiesto de no conflicto de interés")
    private Boolean aceptoConflictoInteres;

    @AssertTrue(message = "Debe aceptar el acuerdo de confidencialidad")
    private Boolean aceptoConfidencialidad;

    private Boolean activo = true;
}