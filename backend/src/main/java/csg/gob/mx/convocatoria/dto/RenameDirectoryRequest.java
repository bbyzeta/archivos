package csg.gob.mx.convocatoria.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Renombra la subcarpeta `path` con el nuevo nombre (solo el último segmento). */
public record RenameDirectoryRequest(@NotBlank @Size(max = 300) String path, @NotBlank @Size(max = 100) String name) {}
