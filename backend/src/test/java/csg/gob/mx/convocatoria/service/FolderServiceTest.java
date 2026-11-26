package csg.gob.mx.convocatoria.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.server.ResponseStatusException;

import csg.gob.mx.convocatoria.dto.FolderRequest;
import csg.gob.mx.convocatoria.dto.FolderResponse;
import csg.gob.mx.convocatoria.entity.FolderEntity;
import csg.gob.mx.convocatoria.entity.UserEntity;
import csg.gob.mx.convocatoria.repository.FolderRepository;
import csg.gob.mx.convocatoria.repository.UserRepository;

class FolderServiceTest {

    @TempDir
    Path storage;

    @TempDir
    Path outside;

    private FolderRepository folderRepository;
    private UserRepository userRepository;
    private FolderService service;

    @BeforeEach
    void setUp() {
        folderRepository = mock(FolderRepository.class);
        userRepository = mock(UserRepository.class);
        service = new FolderService(folderRepository, userRepository, storage.toString(), "https://archivos.test/");
        when(folderRepository.save(any(FolderEntity.class))).thenAnswer(inv -> {
            FolderEntity f = inv.getArgument(0);
            if (f.getId() == null) f.setId(UUID.randomUUID());
            if (f.getCreatedAt() == null) f.setCreatedAt(java.time.LocalDateTime.now());
            return f;
        });
    }

    @AfterEach
    void clearSynchronization() {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.clearSynchronization();
        }
    }

    private FolderEntity existingFolder(Path diskPath) {
        FolderEntity f = new FolderEntity();
        f.setId(UUID.randomUUID());
        f.setName("Original");
        f.setPublicSlug("slug-original");
        f.setDiskPath(diskPath.toString());
        f.setCreatedAt(java.time.LocalDateTime.now());
        when(folderRepository.findById(f.getId())).thenReturn(Optional.of(f));
        return f;
    }

    /** Simula el commit de la transacción para ejecutar la limpieza de disco. */
    private void commit() {
        TransactionSynchronizationManager.getSynchronizations().forEach(TransactionSynchronization::afterCommit);
    }

    // ---------- crear ----------

    @Test
    void creaLaCarpetaConSlugAleatorioYDirectorioDentroDeLaRutaBase() {
        UserEntity admin = new UserEntity();
        admin.setUsername("admin");
        when(userRepository.findByUsername("admin")).thenReturn(Optional.of(admin));

        FolderResponse response = service.create(new FolderRequest("  Documentos  "), "admin");

        assertThat(response.name()).isEqualTo("Documentos");
        assertThat(response.publicSlug()).matches("[A-Za-z0-9_-]{12}");
        assertThat(response.publicUrl()).isEqualTo("https://archivos.test/api/public/" + response.publicSlug());
        assertThat(storage.resolve(response.publicSlug())).isDirectory();
    }

    @Test
    void dosCarpetasNuncaComparteSlug() {
        UserEntity admin = new UserEntity();
        when(userRepository.findByUsername("admin")).thenReturn(Optional.of(admin));

        String a = service.create(new FolderRequest("A"), "admin").publicSlug();
        String b = service.create(new FolderRequest("B"), "admin").publicSlug();

        assertThat(a).isNotEqualTo(b);
    }

    @Test
    void crearConUsuarioInexistenteDevuelve401() {
        when(userRepository.findByUsername("fantasma")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.create(new FolderRequest("X"), "fantasma"))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(e -> ((ResponseStatusException) e).getStatusCode())
                .isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    // ---------- renombrar ----------

    @Test
    void renombrarConservaSlugYRutaEnDisco() throws IOException {
        FolderEntity folder = existingFolder(Files.createDirectories(storage.resolve("slug-original")));

        FolderResponse response = service.rename(folder.getId(), new FolderRequest(" Nuevo nombre "));

        assertThat(response.name()).isEqualTo("Nuevo nombre");
        assertThat(response.publicSlug()).isEqualTo("slug-original");
        assertThat(folder.getDiskPath()).isEqualTo(storage.resolve("slug-original").toString());
        assertThat(storage.resolve("slug-original")).isDirectory();
    }

    @Test
    void renombrarUnaCarpetaInexistenteDevuelve404() {
        UUID unknown = UUID.randomUUID();
        when(folderRepository.findById(unknown)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.rename(unknown, new FolderRequest("X")))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(e -> ((ResponseStatusException) e).getStatusCode())
                .isEqualTo(HttpStatus.NOT_FOUND);
    }

    // ---------- eliminar ----------

    @Test
    void eliminarBorraElDirectorioCompletoSoloTrasElCommit() throws IOException {
        Path dir = Files.createDirectories(storage.resolve("slug-original/Docs/2026"));
        Files.writeString(dir.resolve("a.txt"), "x");
        Files.writeString(storage.resolve("slug-original/b.txt"), "x");
        FolderEntity folder = existingFolder(storage.resolve("slug-original"));
        Files.createDirectories(storage.resolve("otra")); // carpeta vecina que no debe tocarse

        TransactionSynchronizationManager.initSynchronization();
        service.delete(folder.getId());

        verify(folderRepository).delete(folder);
        assertThat(storage.resolve("slug-original")).exists(); // aún no se confirmó la transacción
        commit();
        assertThat(storage.resolve("slug-original")).doesNotExist();
        assertThat(storage.resolve("otra")).isDirectory();
        assertThat(storage).isDirectory();
    }

    @Test
    void siLaTransaccionNoSeConfirmaElDiscoNoSeToca() throws IOException {
        Files.writeString(Files.createDirectories(storage.resolve("slug-original")).resolve("a.txt"), "x");
        FolderEntity folder = existingFolder(storage.resolve("slug-original"));

        TransactionSynchronizationManager.initSynchronization();
        service.delete(folder.getId());
        // rollback: no se invoca afterCommit

        assertThat(storage.resolve("slug-original/a.txt")).exists();
    }

    @Test
    void nuncaBorraRutasFueraDeLaRutaBase() throws IOException {
        Files.writeString(Files.createDirectories(outside.resolve("importante")).resolve("a.txt"), "x");
        FolderEntity folder = existingFolder(outside.resolve("importante"));

        TransactionSynchronizationManager.initSynchronization();
        service.delete(folder.getId());
        commit();

        assertThat(outside.resolve("importante/a.txt")).exists();
    }

    @Test
    void nuncaBorraLaPropiaRutaBase() throws IOException {
        Files.writeString(storage.resolve("a.txt"), "x");
        FolderEntity folder = existingFolder(storage);

        TransactionSynchronizationManager.initSynchronization();
        service.delete(folder.getId());
        commit();

        assertThat(storage.resolve("a.txt")).exists();
    }

    @Test
    void unDiskPathConTraversalNoPuedeEscaparDeLaRutaBase() throws IOException {
        Files.writeString(Files.createDirectories(outside.resolve("importante")).resolve("a.txt"), "x");
        FolderEntity folder = existingFolder(storage.resolve("../" + outside.getFileName() + "/importante"));

        TransactionSynchronizationManager.initSynchronization();
        service.delete(folder.getId());
        commit();

        assertThat(outside.resolve("importante/a.txt")).exists();
    }

    @Test
    void eliminarUnaCarpetaInexistenteDevuelve404() {
        UUID unknown = UUID.randomUUID();
        when(folderRepository.findById(unknown)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.delete(unknown))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(e -> ((ResponseStatusException) e).getStatusCode())
                .isEqualTo(HttpStatus.NOT_FOUND);
    }

    // ---------- color ----------

    @Test
    void asignaYQuitaElColorDeLaCarpeta() throws IOException {
        FolderEntity folder = existingFolder(Files.createDirectories(storage.resolve("slug-original")));

        assertThat(service.setColor(folder.getId(), "#9B2247").color()).isEqualTo("#9b2247");
        assertThat(service.setColor(folder.getId(), null).color()).isNull();
    }

    @Test
    void asignarColorAUnaCarpetaInexistenteDevuelve404() {
        UUID unknown = UUID.randomUUID();
        when(folderRepository.findById(unknown)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.setColor(unknown, "#9b2247"))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(e -> ((ResponseStatusException) e).getStatusCode())
                .isEqualTo(HttpStatus.NOT_FOUND);
    }
}
