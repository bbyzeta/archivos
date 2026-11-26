package csg.gob.mx.convocatoria.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Ruta de la subcarpeta dentro de la carpeta, p. ej. "Docs/2026". */
public record DirectoryRequest(@NotBlank @Size(max = 300) String path) {}
