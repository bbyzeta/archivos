package csg.gob.mx.convocatoria.service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import csg.gob.mx.convocatoria.entity.FolderDirectoryEntity;
import csg.gob.mx.convocatoria.entity.FolderEntity;
import csg.gob.mx.convocatoria.repository.FolderDirectoryRepository;
import csg.gob.mx.convocatoria.repository.FolderFileRepository;
import csg.gob.mx.convocatoria.repository.FolderRepository;

/** Subcarpetas registradas explícitamente (permiten conservar carpetas vacías). */
@Service
public class FolderDirectoryService {

    private final FolderRepository folderRepository;
    private final FolderDirectoryRepository directoryRepository;
    private final FolderFileRepository fileRepository;

    public FolderDirectoryService(FolderRepository folderRepository, FolderDirectoryRepository directoryRepository,
            FolderFileRepository fileRepository) {
        this.folderRepository = folderRepository;
        this.directoryRepository = directoryRepository;
        this.fileRepository = fileRepository;
    }

    @Transactional(readOnly = true)
    public List<String> list(UUID folderId) {
        getFolder(folderId);
        return paths(folderId);
    }

    /** Lista las rutas registradas; también la usa la vista pública. */
    @Transactional(readOnly = true)
    public List<String> paths(UUID folderId) {
        return directoryRepository.findByFolderIdOrderByPathAsc(folderId).stream()
                .map(FolderDirectoryEntity::getPath)
                .toList();
    }

    /** Crea la subcarpeta (idempotente): registro en BD y directorio en disco. */
    @Transactional
    public String create(UUID folderId, String rawPath) {
        FolderEntity folder = getFolder(folderId);
        String path = PathSanitizer.relativePath(rawPath);
        if (path.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El nombre de la carpeta es obligatorio.");
        }

        Path onDisk = resolveOnDisk(folder, path);
        try {
            Files.createDirectories(onDisk);
        } catch (IOException e) {
            throw new java.io.UncheckedIOException("No se pudo crear la subcarpeta en disco.", e);
        }

        if (!directoryRepository.existsByFolderIdAndPath(folderId, path)) {
            FolderDirectoryEntity entity = new FolderDirectoryEntity();
            entity.setFolder(folder);
            entity.setPath(path);
            directoryRepository.save(entity);
        }
        return path;
    }

    /** Elimina una subcarpeta vacía (y las subcarpetas vacías registradas dentro de ella). */
    @Transactional
    public void delete(UUID folderId, String rawPath) {
        FolderEntity folder = getFolder(folderId);
        String path = PathSanitizer.relativePath(rawPath);
        if (path.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Ruta de carpeta no válida.");
        }

        if (fileRepository.existsByFolderIdAndRelativePath(folderId, path)
                || fileRepository.existsByFolderIdAndRelativePathStartingWith(folderId, path + "/")) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "La carpeta contiene archivos; elimínalos primero.");
        }

        String prefix = path + "/";
        List<FolderDirectoryEntity> toDelete = directoryRepository.findByFolderIdOrderByPathAsc(folderId).stream()
                .filter(d -> d.getPath().equals(path) || d.getPath().startsWith(prefix))
                .toList();
        directoryRepository.deleteAll(toDelete);
        directoryRepository.flush();

        removeEmptyTree(resolveOnDisk(folder, path));
    }

    // ---------- helpers ----------

    private FolderEntity getFolder(UUID id) {
        return folderRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Carpeta no encontrada."));
    }

    private Path resolveOnDisk(FolderEntity folder, String path) {
        Path root = Path.of(folder.getDiskPath()).toAbsolutePath().normalize();
        Path resolved = root.resolve(path).normalize();
        // Defensa contra Path Traversal: debe permanecer dentro de la carpeta
        if (!resolved.startsWith(root) || resolved.equals(root)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Ruta no permitida.");
        }
        return resolved;
    }

    /** Borra el directorio y los subdirectorios; si quedara algún archivo suelto, lo conserva. */
    private void removeEmptyTree(Path dir) {
        if (!Files.isDirectory(dir)) return;
        try (Stream<Path> walk = Files.walk(dir)) {
            walk.sorted(Comparator.reverseOrder()).forEach(p -> {
                try {
                    Files.deleteIfExists(p);
                } catch (IOException ignored) {
                    // directorio no vacío: se conserva
                }
            });
        } catch (IOException ignored) {
            // limpieza de disco en el mejor esfuerzo; el registro en BD ya se eliminó
        }
    }
}
