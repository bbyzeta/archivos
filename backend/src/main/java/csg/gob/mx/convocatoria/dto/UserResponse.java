package csg.gob.mx.convocatoria.dto;

import java.time.LocalDateTime;
import java.util.UUID;

public record UserResponse(UUID id, String username, String fullName, String role, boolean active, LocalDateTime createdAt) {}
