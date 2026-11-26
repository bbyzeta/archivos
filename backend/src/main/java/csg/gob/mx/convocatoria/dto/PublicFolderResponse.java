package csg.gob.mx.convocatoria.dto;

import java.util.List;

public record PublicFolderResponse(String name, String publicSlug, List<FolderFileResponse> files) {}
