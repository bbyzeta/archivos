package csg.gob.mx.convocatoria.service;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.server.ResponseStatusException;

import csg.gob.mx.convocatoria.dto.FolderRequest;
import csg.gob.mx.convocatoria.dto.FolderResponse;
import csg.gob.mx.convocatoria.entity.FolderEntity;
import csg.gob.mx.convocatoria.entity.UserEntity;
import csg.gob.mx.convocatoria.repository.FolderRepository;
import csg.gob.mx.convocatoria.repository.UserRepository;

@Service
public class FolderService {

    private static final Logger log = LoggerFactory.getLogger(FolderService.class);
    private static final SecureRandom RANDOM = new SecureRandom();

    private final FolderRepository folderRepository;
    private final UserRepository userRepository;
    private final Path storageBasePath;
    private final String baseUrl;

    public FolderService(FolderRepository folderRepository, UserRepository userRepository,
            @Value("${app.storage.base-path}") String storageBasePath,
            @Value("${app.base-url}") String baseUrl) {
        this.folderRepository = folderRepository;
        this.userRepository = userRepository;
        this.storageBasePath = Path.of(storageBasePath).toAbsolutePath().normalize();
        this.baseUrl = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
    }

    @Transactional(readOnly = true)
    public List<FolderResponse> findAll() {
        return folderRepository.findAll().stream()
                .sorted((a, b) -> b.getCreatedAt().compareTo(a.getCreatedAt()))
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public FolderResponse findById(UUID id) {
        return folderRepository.findById(id)
                .map(this::toResponse)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Carpeta no encontrada."));
    }

    @Transactional
    public FolderResponse create(FolderRequest request, String username) {
        UserEntity creator = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));

        // El slug aleatorio hace el link público no adivinable y define el directorio en disco
        String slug = generateSlug();
        Path folderPath = storageBasePath.resolve(slug).normalize();
        if (!folderPath.startsWith(storageBasePath)) {
            throw new IllegalStateException("Ruta de carpeta fuera del almacenamiento.");
        }

        try {
            Files.createDirectories(folderPath);
        } catch (IOException e) {
            throw new UncheckedIOException("No se pudo crear la carpeta en disco.", e);
        }

        FolderEntity folder = new FolderEntity();
        folder.setName(request.name().trim());
        folder.setPublicSlug(slug);
        folder.setDiskPath(folderPath.toString());
        folder.setCreatedBy(creator);
        return toResponse(folderRepository.save(folder));
    }

    /** Renombra la carpeta. El slug y la ruta en disco no cambian, por lo que los links públicos siguen válidos. */
    @Transactional
    public FolderResponse rename(UUID id, FolderRequest request) {
        FolderEntity folder = folderRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Carpeta no encontrada."));
        folder.setName(request.name().trim());
        return toResponse(folderRepository.save(folder));
    }

    /** Elimina la carpeta: registros en BD (archivos y subcarpetas por cascada) y su directorio en disco. */
    @Transactional
    public void delete(UUID id) {
        FolderEntity folder = folderRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Carpeta no encontrada."));
        Path folderPath = Path.of(folder.getDiskPath()).toAbsolutePath().normalize();

        folderRepository.delete(folder);
        folderRepository.flush();

        // El disco se limpia solo si la transacción se confirma, para no perder archivos si la BD falla
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                deleteFromDisk(folderPath);
            }
        });
    }

    /** Borra el directorio de forma recursiva, solo si está dentro de la ruta base de almacenamiento. */
    private void deleteFromDisk(Path folderPath) {
        if (!folderPath.startsWith(storageBasePath) || folderPath.equals(storageBasePath)) {
            log.warn("No se eliminó del disco {}: queda fuera de la ruta base {}", folderPath, storageBasePath);
            return;
        }
        if (!Files.exists(folderPath)) return;

        try (Stream<Path> walk = Files.walk(folderPath)) { // no sigue enlaces simbólicos
            walk.sorted(Comparator.reverseOrder()).forEach(p -> {
                try {
                    Files.deleteIfExists(p);
                } catch (IOException e) {
                    log.warn("No se pudo eliminar {}: {}", p, e.getMessage());
                }
            });
        } catch (IOException e) {
            log.warn("No se pudo recorrer {} para eliminarlo: {}", folderPath, e.getMessage());
        }
    }

    private FolderResponse toResponse(FolderEntity folder) {
        return new FolderResponse(folder.getId(), folder.getName(), folder.getPublicSlug(),
                baseUrl + "/api/public/" + folder.getPublicSlug(), folder.getCreatedAt());
    }

    private String generateSlug() {
        byte[] bytes = new byte[9];
        RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes); // 12 caracteres
    }
}
