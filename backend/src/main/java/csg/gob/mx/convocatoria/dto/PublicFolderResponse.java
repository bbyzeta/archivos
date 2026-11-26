package csg.gob.mx.convocatoria.dto;

import java.util.List;

/** `directories`: subcarpetas registradas explícitamente (incluye las vacías y las que tienen color). */
public record PublicFolderResponse(String name, String publicSlug, String color, List<FolderFileResponse> files,
        List<DirectoryResponse> directories) {}
