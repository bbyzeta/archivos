package csg.gob.mx.convocatoria.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** Asigna (o quita, con color null) el color de la subcarpeta `path`. */
public record DirectoryColorRequest(
        @NotBlank @Size(max = 300) String path,
        @Pattern(regexp = "^#[0-9a-fA-F]{6}$") String color) {}
