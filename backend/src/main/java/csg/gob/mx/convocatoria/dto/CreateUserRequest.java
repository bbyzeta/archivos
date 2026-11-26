package csg.gob.mx.convocatoria.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CreateUserRequest(
        @NotBlank @Pattern(regexp = "^[A-Za-z0-9._@-]{3,100}$") String username,
        @NotBlank @Size(max = 150) String fullName,
        // bcrypt solo considera los primeros 72 bytes
        @NotBlank @Size(min = 8, max = 72) String password,
        @NotBlank @Pattern(regexp = "^(ADMIN|USER)$") String role) {}
