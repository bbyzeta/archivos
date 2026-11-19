package csg.gob.mx.convocatoria.dto;

import jakarta.validation.constraints.*;
import lombok.Data;

@Data
public class RegistroDTO {

    @NotBlank(message = "El nombre es obligatorio")
    private String nombres;

    @NotBlank(message = "Los apellidos son obligatorios")
    private String apellidos;

    @NotBlank(message = "La CURP es obligatoria")
    @Pattern(regexp = "^[A-Z]{4}\\d{6}[HM][A-Z]{5}[0-9A-Z]\\d$", message = "El formato de la CURP es inválido")
    private String curp;

    @NotBlank(message = "El teléfono es obligatorio")
    @Pattern(regexp = "^[0-9]{10}$", message = "El teléfono debe contener exactamente 10 dígitos")
    private String telefono;

    @NotBlank(message = "El correo es obligatorio")
    @Email(message = "Formato de correo electrónico inválido")
    private String email;

    @NotBlank(message = "La contraseña es obligatoria")
    @Size(min = 8, message = "La contraseña debe tener al menos 8 caracteres")
    private String password;

  //  @NotNull(message = "El estado es obligatorio")
    private Long idEstado;

  //  @NotBlank(message = "La institución es obligatoria")
    private String institucion;

    @NotBlank(message = "El título es obligatorio")
    private String titulo;

    @AssertTrue(message = "Debe aceptar los términos y condiciones")
    private boolean aceptoTerminos;
}