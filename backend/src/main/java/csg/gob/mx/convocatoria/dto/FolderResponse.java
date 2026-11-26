package csg.gob.mx.convocatoria.dto;

import java.time.LocalDateTime;
import java.util.UUID;

public record FolderResponse(UUID id, String name, String publicSlug, String publicUrl, LocalDateTime createdAt) {}
