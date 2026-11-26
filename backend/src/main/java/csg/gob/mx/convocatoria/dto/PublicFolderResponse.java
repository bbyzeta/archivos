package csg.gob.mx.convocatoria.dto;

import java.util.List;

/** `directories`: subcarpetas registradas explícitamente (incluye las vacías). */
public record PublicFolderResponse(String name, String publicSlug, List<FolderFileResponse> files, List<String> directories) {}
