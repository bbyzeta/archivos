package csg.gob.mx.convocatoria.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** Edición de un usuario: nombre completo y rol (el nombre de usuario no cambia). */
public record UpdateUserRequest(
        @NotBlank @Size(max = 150) String fullName,
        @NotBlank @Pattern(regexp = "^(ADMIN|USER)$") String role) {}
