package csg.gob.mx.convocatoria.dto;

import java.util.UUID;

public record LoginResponse(String token, UUID userId, String username, String fullName, String role) {}
