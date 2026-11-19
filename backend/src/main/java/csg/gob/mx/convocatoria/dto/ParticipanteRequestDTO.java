package csg.gob.mx.convocatoria.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ParticipanteRequestDTO {

    @NotBlank(message = "El nombre es obligatorio")
    private String nombres;

    @NotBlank(message = "Los apellidos son obligatorios")
    private String apellidos;

    @NotBlank(message = "El CURP es obligatorio")
    @Size(min = 18, max = 18, message = "El CURP debe contener 18 caracteres")    
    private String curp;

    @NotBlank(message = "El teléfono es obligatorio")
    @Pattern(regexp = "^[0-9]{10}$", message = "El teléfono debe ser de 10 dígitos")
    private String telefono;

    @NotBlank(message = "El correo es obligatorio")
    @Email(message = "El correo no tiene un formato válido")
    private String username;

    private String password;

    private Boolean activo;

    @NotNull(message = "Los datos institucionales son requeridos")
    @Valid
    private DatosInstitucionalesDTO datosInstitucionales;
}