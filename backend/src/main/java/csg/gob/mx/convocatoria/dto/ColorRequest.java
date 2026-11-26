package csg.gob.mx.convocatoria.dto;

import jakarta.validation.constraints.Pattern;

/** Color en hexadecimal #RRGGBB; null quita el color. */
public record ColorRequest(@Pattern(regexp = "^#[0-9a-fA-F]{6}$") String color) {}
