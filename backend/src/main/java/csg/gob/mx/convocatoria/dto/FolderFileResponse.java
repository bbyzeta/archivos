package csg.gob.mx.convocatoria.dto;

import java.time.LocalDateTime;
import java.util.UUID;

public record FolderFileResponse(UUID id, String filename, String relativePath, String mimeType, long sizeBytes,
        LocalDateTime uploadedAt, String publicUrl) {}
