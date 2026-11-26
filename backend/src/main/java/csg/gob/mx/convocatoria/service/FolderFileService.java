package csg.gob.mx.convocatoria.service;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.UUID;
import java.util.regex.Pattern;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.util.UriUtils;

import csg.gob.mx.convocatoria.dto.FolderFileResponse;
import csg.gob.mx.convocatoria.dto.PublicFolderResponse;
import csg.gob.mx.convocatoria.entity.FolderEntity;
import csg.gob.mx.convocatoria.entity.FolderFileEntity;
import csg.gob.mx.convocatoria.repository.FolderFileRepository;
import csg.gob.mx.convocatoria.repository.FolderRepository;

@Service
public class FolderFileService {

    /** Caracteres no válidos en nombres de archivo (Windows/Linux) y de control. */
    private static final Pattern INVALID_NAME_CHARS = PathSanitizer.INVALID_NAME_CHARS;
    private static final int MAX_FILENAME = 255;

    /** Archivo público listo para enviar. */
    public record PublicFile(Resource resource, String filename, String mimeType) {}

    private final FolderRepository folderRepository;
    private final FolderFileRepository fileRepository;
    private final FolderDirectoryService directoryService;
    private final String baseUrl;

    public FolderFileService(FolderRepository folderRepository, FolderFileRepository fileRepository,
            FolderDirectoryService directoryService,
            @Value("${app.base-url}") String baseUrl) {
        this.folderRepository = folderRepository;
        this.fileRepository = fileRepository;
        this.directoryService = directoryService;
        this.baseUrl = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
    }

    @Transactional(readOnly = true)
    public List<FolderFileResponse> list(UUID folderId) {
        FolderEntity folder = getFolder(folderId);
        return fileRepository.findByFolderIdOrderByUploadedAtDesc(folderId).stream()
                .map(f -> toResponse(folder, f))
                .toList();
    }

    @Transactional
    public FolderFileResponse upload(UUID folderId, MultipartFile file, String rawRelativePath) {
        FolderEntity folder = getFolder(folderId);
        if (file == null) {
            throw badRequest("No se recibió ningún archivo.");
        }

        String filename = sanitizeFilename(file.getOriginalFilename());
        String relativePath = sanitizeRelativePath(rawRelativePath);

        if (fileRepository.existsByFolderIdAndRelativePathAndFilename(folderId, relativePath, filename)) {
            throw conflict();
        }

        Path root = Path.of(folder.getDiskPath()).toAbsolutePath().normalize();
        Path targetDir = root.resolve(relativePath).normalize();
        Path target = targetDir.resolve(filename).normalize();
        // Defensa contra Path Traversal: el destino debe permanecer dentro de la carpeta
        if (!targetDir.startsWith(root) || !target.startsWith(targetDir)) {
            throw badRequest("Ruta no permitida.");
        }

        try {
            Files.createDirectories(targetDir);
            try (InputStream in = file.getInputStream()) {
                Files.copy(in, target); // sin REPLACE_EXISTING: nunca sobrescribe
            }
        } catch (FileAlreadyExistsException e) {
            throw conflict();
        } catch (IOException e) {
            throw new UncheckedIOException("No se pudo guardar el archivo en disco.", e);
        }

        try {
            FolderFileEntity entity = new FolderFileEntity();
            entity.setFolder(folder);
            entity.setFilename(filename);
            entity.setRelativePath(relativePath);
            entity.setMimeType(resolveMimeType(file, target));
            entity.setSizeBytes(file.getSize());
            return toResponse(folder, fileRepository.saveAndFlush(entity));
        } catch (RuntimeException e) {
            try {
                Files.deleteIfExists(target); // no dejar archivos huérfanos si falla la BD
            } catch (IOException ignored) {
                // se conserva el error original
            }
            throw e;
        }
    }

    @Transactional
    public void delete(UUID folderId, UUID fileId) {
        FolderEntity folder = getFolder(folderId);
        FolderFileEntity entity = fileRepository.findByIdAndFolderId(fileId, folderId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Archivo no encontrado."));

        Path target = resolveOnDisk(folder, entity);
        fileRepository.delete(entity);
        fileRepository.flush();
        try {
            Files.deleteIfExists(target);
        } catch (IOException e) {
            throw new UncheckedIOException("No se pudo eliminar el archivo en disco.", e);
        }
    }

    @Transactional(readOnly = true)
    public PublicFolderResponse getPublicFolder(String slug) {
        FolderEntity folder = getFolderBySlug(slug);
        List<FolderFileResponse> files = fileRepository.findByFolderIdOrderByUploadedAtDesc(folder.getId()).stream()
                .map(f -> toResponse(folder, f))
                .toList();
        return new PublicFolderResponse(folder.getName(), folder.getPublicSlug(), folder.getColor(), files,
                directoryService.directories(folder.getId()));
    }

    /** Busca el archivo por su ruta completa dentro de la carpeta pública (solo archivos registrados en BD). */
    @Transactional(readOnly = true)
    public PublicFile getPublicFile(String slug, String fullPath) {
        FolderEntity folder = getFolderBySlug(slug);

        String normalized = fullPath.replace('\\', '/');
        int cut = normalized.lastIndexOf('/');
        String relativePath = cut < 0 ? "" : normalized.substring(0, cut);
        String filename = cut < 0 ? normalized : normalized.substring(cut + 1);

        FolderFileEntity entity = fileRepository
                .findByFolderIdAndRelativePathAndFilename(folder.getId(), relativePath, filename)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Archivo no encontrado."));

        Path path = resolveOnDisk(folder, entity);
        if (!Files.isRegularFile(path)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Archivo no encontrado.");
        }
        return new PublicFile(new FileSystemResource(path), entity.getFilename(), entity.getMimeType());
    }

    // ---------- helpers ----------

    private FolderEntity getFolder(UUID id) {
        return folderRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Carpeta no encontrada."));
    }

    private FolderEntity getFolderBySlug(String slug) {
        return folderRepository.findByPublicSlug(slug)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Carpeta no encontrada."));
    }

    private Path resolveOnDisk(FolderEntity folder, FolderFileEntity entity) {
        Path root = Path.of(folder.getDiskPath()).toAbsolutePath().normalize();
        Path path = root.resolve(entity.getRelativePath()).resolve(entity.getFilename()).normalize();
        if (!path.startsWith(root)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Archivo no encontrado.");
        }
        return path;
    }

    private String sanitizeFilename(String original) {
        if (original == null) throw badRequest("Nombre de archivo inválido.");
        String name = original.replace('\\', '/');
        name = name.substring(name.lastIndexOf('/') + 1).trim(); // descarta cualquier ruta incluida
        if (name.isEmpty() || name.equals(".") || name.equals("..") || name.length() > MAX_FILENAME
                || INVALID_NAME_CHARS.matcher(name).find()) {
            throw badRequest("Nombre de archivo inválido.");
        }
        return name;
    }

    /** Devuelve la ruta relativa normalizada con '/', o "" para la raíz de la carpeta. */
    private String sanitizeRelativePath(String raw) {
        return PathSanitizer.relativePath(raw);
    }

    private String resolveMimeType(MultipartFile file, Path target) {
        String type = file.getContentType();
        if (type == null || type.isBlank() || type.equals("application/octet-stream")) {
            try {
                String probed = Files.probeContentType(target);
                if (probed != null) type = probed;
            } catch (IOException ignored) {
                // se usa el valor por defecto
            }
        }
        if (type == null || type.isBlank()) type = "application/octet-stream";
        return type.length() > 150 ? "application/octet-stream" : type;
    }

    private FolderFileResponse toResponse(FolderEntity folder, FolderFileEntity f) {
        StringBuilder url = new StringBuilder(baseUrl).append("/api/public/").append(folder.getPublicSlug()).append("/file");
        if (!f.getRelativePath().isEmpty()) {
            for (String segment : f.getRelativePath().split("/")) {
                url.append('/').append(UriUtils.encodePathSegment(segment, StandardCharsets.UTF_8));
            }
        }
        url.append('/').append(UriUtils.encodePathSegment(f.getFilename(), StandardCharsets.UTF_8));
        return new FolderFileResponse(f.getId(), f.getFilename(), f.getRelativePath(), f.getMimeType(),
                f.getSizeBytes(), f.getUploadedAt(), url.toString());
    }

    private ResponseStatusException badRequest(String message) {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
    }

    private ResponseStatusException conflict() {
        return new ResponseStatusException(HttpStatus.CONFLICT, "Ya existe un archivo con ese nombre en la carpeta.");
    }
}
