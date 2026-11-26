package csg.gob.mx.convocatoria.service;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.server.ResponseStatusException;

import csg.gob.mx.convocatoria.dto.DirectoryResponse;
import csg.gob.mx.convocatoria.entity.FolderDirectoryEntity;
import csg.gob.mx.convocatoria.entity.FolderEntity;
import csg.gob.mx.convocatoria.entity.FolderFileEntity;
import csg.gob.mx.convocatoria.repository.FolderDirectoryRepository;
import csg.gob.mx.convocatoria.repository.FolderFileRepository;
import csg.gob.mx.convocatoria.repository.FolderRepository;

/** Subcarpetas: registro explícito (carpetas vacías), renombrado y eliminación. */
@Service
public class FolderDirectoryService {

    private static final Logger log = LoggerFactory.getLogger(FolderDirectoryService.class);

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
    public List<DirectoryResponse> list(UUID folderId) {
        getFolder(folderId);
        return directories(folderId);
    }

    /** Lista las rutas registradas; también la usa la vista pública. */
    @Transactional(readOnly = true)
    public List<DirectoryResponse> directories(UUID folderId) {
        return directoryRepository.findByFolderIdOrderByPathAsc(folderId).stream()
                .map(d -> new DirectoryResponse(d.getPath(), d.getColor()))
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
            throw new UncheckedIOException("No se pudo crear la subcarpeta en disco.", e);
        }

        if (!directoryRepository.existsByFolderIdAndPath(folderId, path)) {
            FolderDirectoryEntity entity = new FolderDirectoryEntity();
            entity.setFolder(folder);
            entity.setPath(path);
            directoryRepository.save(entity);
        }
        return path;
    }


    /**
     * Asigna o quita (null) el color de una subcarpeta. Si la subcarpeta solo existe por sus archivos (no tiene
     * registro propio), se crea el registro para poder guardar el color.
     */
    @Transactional
    public DirectoryResponse setColor(UUID folderId, String rawPath, String color) {
        getFolder(folderId);
        String path = requirePath(rawPath);
        String prefix = path + "/";

        FolderDirectoryEntity entity = directoryRepository.findByFolderIdOrderByPathAsc(folderId).stream()
                .filter(d -> d.getPath().equals(path)).findFirst().orElse(null);
        if (entity == null) {
            boolean existsByFiles = fileRepository.findByFolderId(folderId).stream()
                    .anyMatch(f -> f.getRelativePath().equals(path) || f.getRelativePath().startsWith(prefix));
            if (!existsByFiles) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Carpeta no encontrada.");
            }
            entity = new FolderDirectoryEntity();
            entity.setFolder(getFolder(folderId));
            entity.setPath(path);
        }
        entity.setColor(color == null ? null : color.toLowerCase());
        FolderDirectoryEntity saved = directoryRepository.save(entity);
        return new DirectoryResponse(saved.getPath(), saved.getColor());
    }
    /**
     * Elimina una subcarpeta. Si contiene archivos solo se elimina con `recursive`, que borra también sus
     * archivos (BD y disco) y sus subcarpetas.
     */
    @Transactional
    public void delete(UUID folderId, String rawPath, boolean recursive) {
        FolderEntity folder = getFolder(folderId);
        String path = requirePath(rawPath);
        String prefix = path + "/";

        // La comparación se hace en Java (distingue mayúsculas, como el disco en Linux)
        List<FolderFileEntity> files = fileRepository.findByFolderId(folderId).stream()
                .filter(f -> f.getRelativePath().equals(path) || f.getRelativePath().startsWith(prefix))
                .toList();
        if (!files.isEmpty() && !recursive) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "La carpeta contiene archivos; elimínalos primero.");
        }

        List<FolderDirectoryEntity> dirs = directoryRepository.findByFolderIdOrderByPathAsc(folderId).stream()
                .filter(d -> d.getPath().equals(path) || d.getPath().startsWith(prefix))
                .toList();

        fileRepository.deleteAll(files);
        directoryRepository.deleteAll(dirs);
        fileRepository.flush();
        directoryRepository.flush();

        // El disco se limpia solo si la transacción se confirma
        Path onDisk = resolveOnDisk(folder, path);
        afterCommit(() -> deleteTree(onDisk));
    }

    /**
     * Renombra una subcarpeta (cambia su último segmento) actualizando archivos y subcarpetas en BD y moviendo el
     * directorio en disco. Los links directos de los archivos de su interior cambian porque incluyen la ruta.
     */
    @Transactional
    public String rename(UUID folderId, String rawPath, String rawName) {
        FolderEntity folder = getFolder(folderId);
        String oldPath = requirePath(rawPath);

        String name = rawName == null ? "" : rawName.trim();
        if (name.isEmpty() || name.contains("/") || name.contains("\\") || !PathSanitizer.relativePath(name).equals(name)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Nombre de carpeta no válido.");
        }

        int cut = oldPath.lastIndexOf('/');
        String parent = cut < 0 ? "" : oldPath.substring(0, cut);
        String newPath = parent.isEmpty() ? name : parent + "/" + name;
        if (newPath.equals(oldPath)) return newPath;
        PathSanitizer.relativePath(newPath); // valida la longitud total

        String oldPrefix = oldPath + "/";
        List<FolderFileEntity> allFiles = fileRepository.findByFolderId(folderId);
        List<FolderDirectoryEntity> allDirs = directoryRepository.findByFolderIdOrderByPathAsc(folderId);

        List<FolderFileEntity> files = allFiles.stream()
                .filter(f -> f.getRelativePath().equals(oldPath) || f.getRelativePath().startsWith(oldPrefix)).toList();
        List<FolderDirectoryEntity> dirs = allDirs.stream()
                .filter(d -> d.getPath().equals(oldPath) || d.getPath().startsWith(oldPrefix)).toList();
        if (files.isEmpty() && dirs.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Carpeta no encontrada.");
        }

        Path oldDisk = resolveOnDisk(folder, oldPath);
        Path newDisk = resolveOnDisk(folder, newPath);

        // Conflicto: ya existe algo en el destino (sin distinguir mayúsculas, para ser conservadores)
        if (!newPath.equalsIgnoreCase(oldPath)) {
            boolean taken = allFiles.stream().anyMatch(f -> isSameOrInside(f.getRelativePath(), newPath))
                    || allDirs.stream().anyMatch(d -> isSameOrInside(d.getPath(), newPath))
                    || Files.exists(newDisk);
            if (taken) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Ya existe una carpeta con ese nombre.");
            }
        }

        // La ruta resultante de cualquier archivo o subcarpeta debe caber en la columna
        files.forEach(f -> PathSanitizer.relativePath(replacePrefix(f.getRelativePath(), oldPath, newPath)));
        dirs.forEach(d -> PathSanitizer.relativePath(replacePrefix(d.getPath(), oldPath, newPath)));

        boolean moved = false;
        try {
            if (Files.isDirectory(oldDisk)) {
                Files.createDirectories(newDisk.getParent());
                Files.move(oldDisk, newDisk, StandardCopyOption.ATOMIC_MOVE);
                moved = true;
            } else {
                Files.createDirectories(newDisk);
            }

            files.forEach(f -> f.setRelativePath(replacePrefix(f.getRelativePath(), oldPath, newPath)));
            dirs.forEach(d -> d.setPath(replacePrefix(d.getPath(), oldPath, newPath)));
            fileRepository.saveAll(files);
            directoryRepository.saveAll(dirs);
            fileRepository.flush();
            directoryRepository.flush();
        } catch (IOException e) {
            throw new UncheckedIOException("No se pudo renombrar la carpeta en disco.", e);
        } catch (RuntimeException e) {
            if (moved) {
                try {
                    Files.move(newDisk, oldDisk, StandardCopyOption.ATOMIC_MOVE); // deshace el movimiento en disco
                } catch (IOException ex) {
                    log.error("No se pudo revertir el renombrado en disco de {} a {}", newDisk, oldDisk, ex);
                }
            }
            throw e;
        }
        return newPath;
    }

    // ---------- helpers ----------

    private FolderEntity getFolder(UUID id) {
        return folderRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Carpeta no encontrada."));
    }

    private String requirePath(String rawPath) {
        String path = PathSanitizer.relativePath(rawPath);
        if (path.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Ruta de carpeta no válida.");
        }
        return path;
    }

    private boolean isSameOrInside(String candidate, String path) {
        return candidate.equalsIgnoreCase(path)
                || candidate.regionMatches(true, 0, path + "/", 0, path.length() + 1);
    }

    /** Sustituye el prefijo `oldPath` por `newPath` (el valor es igual a oldPath o empieza por oldPath + "/"). */
    private String replacePrefix(String value, String oldPath, String newPath) {
        return newPath + value.substring(oldPath.length());
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

    private void afterCommit(Runnable action) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    action.run();
                }
            });
        } else {
            action.run();
        }
    }

    /** Borra el directorio con todo su contenido (sin seguir enlaces simbólicos). */
    private void deleteTree(Path dir) {
        if (!Files.isDirectory(dir)) return;
        try (Stream<Path> walk = Files.walk(dir)) {
            walk.sorted(Comparator.reverseOrder()).forEach(p -> {
                try {
                    Files.deleteIfExists(p);
                } catch (IOException e) {
                    log.warn("No se pudo eliminar {}: {}", p, e.getMessage());
                }
            });
        } catch (IOException e) {
            log.warn("No se pudo recorrer {} para eliminarlo: {}", dir, e.getMessage());
        }
    }
}
