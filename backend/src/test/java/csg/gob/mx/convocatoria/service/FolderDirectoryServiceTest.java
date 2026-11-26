package csg.gob.mx.convocatoria.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyIterable;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import csg.gob.mx.convocatoria.entity.FolderDirectoryEntity;
import csg.gob.mx.convocatoria.entity.FolderEntity;
import csg.gob.mx.convocatoria.entity.FolderFileEntity;
import csg.gob.mx.convocatoria.repository.FolderDirectoryRepository;
import csg.gob.mx.convocatoria.repository.FolderFileRepository;
import csg.gob.mx.convocatoria.repository.FolderRepository;

class FolderDirectoryServiceTest {

    @TempDir
    Path storage;

    private FolderRepository folderRepository;
    private FolderDirectoryRepository directoryRepository;
    private FolderFileRepository fileRepository;
    private FolderDirectoryService service;
    private FolderEntity folder;
    private Path folderDir;

    // "Base de datos" en memoria que devuelven los repositorios simulados
    private final List<FolderFileEntity> files = new ArrayList<>();
    private final List<FolderDirectoryEntity> dirs = new ArrayList<>();

    @BeforeEach
    void setUp() throws IOException {
        folderRepository = mock(FolderRepository.class);
        directoryRepository = mock(FolderDirectoryRepository.class);
        fileRepository = mock(FolderFileRepository.class);
        service = new FolderDirectoryService(folderRepository, directoryRepository, fileRepository);

        folderDir = Files.createDirectories(storage.resolve("abc123"));
        folder = new FolderEntity();
        folder.setId(UUID.randomUUID());
        folder.setPublicSlug("abc123");
        folder.setDiskPath(folderDir.toString());

        when(folderRepository.findById(folder.getId())).thenReturn(Optional.of(folder));
        when(fileRepository.findByFolderId(folder.getId())).thenAnswer(inv -> List.copyOf(files));
        when(directoryRepository.findByFolderIdOrderByPathAsc(folder.getId())).thenAnswer(inv -> List.copyOf(dirs));
        when(directoryRepository.existsByFolderIdAndPath(any(), any()))
                .thenAnswer(inv -> dirs.stream().anyMatch(d -> d.getPath().equals(inv.getArgument(1))));
    }

    private void assertStatus(Throwable t, HttpStatus expected) {
        assertThat(t).isInstanceOf(ResponseStatusException.class);
        assertThat(((ResponseStatusException) t).getStatusCode()).isEqualTo(expected);
    }

    private FolderFileEntity addFile(String relativePath, String filename) throws IOException {
        Path dir = relativePath.isEmpty() ? folderDir : Files.createDirectories(folderDir.resolve(relativePath));
        Files.writeString(dir.resolve(filename), "contenido");
        FolderFileEntity f = new FolderFileEntity();
        f.setId(UUID.randomUUID());
        f.setFolder(folder);
        f.setRelativePath(relativePath);
        f.setFilename(filename);
        files.add(f);
        return f;
    }

    private FolderDirectoryEntity addDir(String path) throws IOException {
        Files.createDirectories(folderDir.resolve(path));
        FolderDirectoryEntity d = new FolderDirectoryEntity();
        d.setId(UUID.randomUUID());
        d.setFolder(folder);
        d.setPath(path);
        dirs.add(d);
        return d;
    }

    // ---------- crear ----------

    @Test
    void creaLaSubcarpetaEnDiscoYLaRegistra() {
        String path = service.create(folder.getId(), "Docs\\Vacía/");

        assertThat(path).isEqualTo("Docs/Vacía");
        assertThat(folderDir.resolve("Docs/Vacía")).isDirectory();
        ArgumentCaptor<FolderDirectoryEntity> saved = ArgumentCaptor.forClass(FolderDirectoryEntity.class);
        verify(directoryRepository).save(saved.capture());
        assertThat(saved.getValue().getPath()).isEqualTo("Docs/Vacía");
    }

    @Test
    void crearEsIdempotente() throws IOException {
        addDir("Docs");

        service.create(folder.getId(), "Docs");

        verify(directoryRepository, never()).save(any());
    }

    @Test
    void crearRechazaTraversalYRutasVacias() {
        assertThatThrownBy(() -> service.create(folder.getId(), "../fuera"))
                .satisfies(t -> assertStatus(t, HttpStatus.BAD_REQUEST));
        assertThatThrownBy(() -> service.create(folder.getId(), "  "))
                .satisfies(t -> assertStatus(t, HttpStatus.BAD_REQUEST));

        assertThat(storage.resolve("fuera")).doesNotExist();
    }

    // ---------- eliminar ----------

    @Test
    void eliminarUnaCarpetaVaciaBorraRegistroYDirectorio() throws IOException {
        addDir("Vacia");

        service.delete(folder.getId(), "Vacia", false);

        verify(directoryRepository).deleteAll(anyIterable());
        assertThat(folderDir.resolve("Vacia")).doesNotExist();
        assertThat(folderDir).isDirectory(); // la carpeta raíz nunca se toca
    }

    @Test
    void sinRecursiveNoEliminaCarpetasConArchivos() throws IOException {
        addFile("Docs", "a.txt");

        assertThatThrownBy(() -> service.delete(folder.getId(), "Docs", false))
                .satisfies(t -> assertStatus(t, HttpStatus.CONFLICT));

        assertThat(folderDir.resolve("Docs/a.txt")).exists();
        verify(fileRepository, never()).deleteAll(anyIterable());
    }

    @Test
    void recursiveEliminaArchivosSubcarpetasYDirectorioSinTocarHermanas() throws IOException {
        addFile("Docs", "a.txt");
        addFile("Docs/2026", "b.txt");
        addDir("Docs/Vacia");
        FolderFileEntity hermano = addFile("Docs2", "c.txt"); // comparte prefijo textual con "Docs"

        service.delete(folder.getId(), "Docs", true);

        ArgumentCaptor<List<FolderFileEntity>> deleted = ArgumentCaptor.forClass(List.class);
        verify(fileRepository).deleteAll(deleted.capture());
        assertThat(deleted.getValue()).extracting(FolderFileEntity::getFilename).containsExactlyInAnyOrder("a.txt", "b.txt");
        assertThat(deleted.getValue()).doesNotContain(hermano);
        assertThat(folderDir.resolve("Docs")).doesNotExist();
        assertThat(folderDir.resolve("Docs2/c.txt")).exists();
    }

    @Test
    void eliminarRechazaTraversalYLaRaiz() {
        assertThatThrownBy(() -> service.delete(folder.getId(), "../abc123", true))
                .satisfies(t -> assertStatus(t, HttpStatus.BAD_REQUEST));
        assertThatThrownBy(() -> service.delete(folder.getId(), "", true))
                .satisfies(t -> assertStatus(t, HttpStatus.BAD_REQUEST));
        assertThat(folderDir).isDirectory();
    }

    // ---------- renombrar ----------

    @Test
    void renombrarMueveElDirectorioYActualizaRutasDeArchivosYSubcarpetas() throws IOException {
        FolderFileEntity enRaiz = addFile("Docs", "a.txt");
        FolderFileEntity anidado = addFile("Docs/2026", "b.txt");
        FolderDirectoryEntity vacia = addDir("Docs/Vacia");
        FolderFileEntity hermano = addFile("Docs2", "c.txt");

        String newPath = service.rename(folder.getId(), "Docs", "Informes");

        assertThat(newPath).isEqualTo("Informes");
        assertThat(folderDir.resolve("Informes/a.txt")).exists();
        assertThat(folderDir.resolve("Informes/2026/b.txt")).exists();
        assertThat(folderDir.resolve("Informes/Vacia")).isDirectory();
        assertThat(folderDir.resolve("Docs")).doesNotExist();
        assertThat(enRaiz.getRelativePath()).isEqualTo("Informes");
        assertThat(anidado.getRelativePath()).isEqualTo("Informes/2026");
        assertThat(vacia.getPath()).isEqualTo("Informes/Vacia");
        assertThat(hermano.getRelativePath()).isEqualTo("Docs2"); // no se toca
        assertThat(folderDir.resolve("Docs2/c.txt")).exists();
    }

    @Test
    void renombrarUnaSubcarpetaAnidadaConservaSuPadre() throws IOException {
        FolderFileEntity f = addFile("Docs/2026", "b.txt");

        String newPath = service.rename(folder.getId(), "Docs/2026", "2027");

        assertThat(newPath).isEqualTo("Docs/2027");
        assertThat(f.getRelativePath()).isEqualTo("Docs/2027");
        assertThat(folderDir.resolve("Docs/2027/b.txt")).exists();
    }

    @Test
    void renombrarAUnNombreExistenteDevuelve409YNoMueveNada() throws IOException {
        FolderFileEntity f = addFile("Docs", "a.txt");
        addFile("Informes", "x.txt");

        assertThatThrownBy(() -> service.rename(folder.getId(), "Docs", "informes")) // sin distinguir mayúsculas
                .satisfies(t -> assertStatus(t, HttpStatus.CONFLICT));

        assertThat(f.getRelativePath()).isEqualTo("Docs");
        assertThat(folderDir.resolve("Docs/a.txt")).exists();
    }

    @Test
    void renombrarRechazaNombresConSeparadoresOTraversal() throws IOException {
        addFile("Docs", "a.txt");

        for (String name : new String[] {"a/b", "a\\b", "..", ".", "", "  ", "x:y"}) {
            assertThatThrownBy(() -> service.rename(folder.getId(), "Docs", name))
                    .as("nombre: '%s'", name)
                    .satisfies(t -> assertStatus(t, HttpStatus.BAD_REQUEST));
        }
        assertThat(folderDir.resolve("Docs/a.txt")).exists();
    }

    @Test
    void renombrarUnaCarpetaInexistenteDevuelve404() {
        assertThatThrownBy(() -> service.rename(folder.getId(), "NoExiste", "Nuevo"))
                .satisfies(t -> assertStatus(t, HttpStatus.NOT_FOUND));
    }

    @Test
    void siFallaLaBdDespuesDeMoverSeRevierteElMovimientoEnDisco() throws IOException {
        FolderFileEntity f = addFile("Docs", "a.txt");
        doThrow(new IllegalStateException("BD caída")).when(fileRepository).flush();

        assertThatThrownBy(() -> service.rename(folder.getId(), "Docs", "Informes"))
                .isInstanceOf(IllegalStateException.class);

        assertThat(folderDir.resolve("Docs/a.txt")).exists();
        assertThat(folderDir.resolve("Informes")).doesNotExist();
        assertThat(f.getFilename()).isEqualTo("a.txt");
    }

    @Test
    void renombrarConElMismoNombreNoHaceNada() throws IOException {
        addFile("Docs", "a.txt");

        assertThat(service.rename(folder.getId(), "Docs", "Docs")).isEqualTo("Docs");
        assertThat(folderDir.resolve("Docs/a.txt")).exists();
    }

    // ---------- color ----------

    @Test
    void asignarColorAUnaSubcarpetaRegistradaActualizaSuRegistro() throws IOException {
        FolderDirectoryEntity vacia = addDir("Vacia");
        when(directoryRepository.save(any(FolderDirectoryEntity.class))).thenAnswer(inv -> inv.getArgument(0));

        var response = service.setColor(folder.getId(), "Vacia", "#1E5B4F");

        assertThat(response.color()).isEqualTo("#1e5b4f");
        assertThat(vacia.getColor()).isEqualTo("#1e5b4f");
    }

    @Test
    void asignarColorAUnaSubcarpetaQueSoloExistePorSusArchivosCreaElRegistro() throws IOException {
        addFile("Docs/2026", "a.txt");
        when(directoryRepository.save(any(FolderDirectoryEntity.class))).thenAnswer(inv -> inv.getArgument(0));

        var response = service.setColor(folder.getId(), "Docs", "#a57f2c");

        ArgumentCaptor<FolderDirectoryEntity> saved = ArgumentCaptor.forClass(FolderDirectoryEntity.class);
        verify(directoryRepository).save(saved.capture());
        assertThat(saved.getValue().getPath()).isEqualTo("Docs");
        assertThat(response.color()).isEqualTo("#a57f2c");
    }

    @Test
    void asignarColorAUnaRutaInexistenteDevuelve404YRechazaTraversal() {
        assertThatThrownBy(() -> service.setColor(folder.getId(), "NoExiste", "#a57f2c"))
                .satisfies(t -> assertStatus(t, HttpStatus.NOT_FOUND));
        assertThatThrownBy(() -> service.setColor(folder.getId(), "../x", "#a57f2c"))
                .satisfies(t -> assertStatus(t, HttpStatus.BAD_REQUEST));
        verify(directoryRepository, never()).save(any());
    }

    @Test
    void quitarElColorConNullLoDejaSinColor() throws IOException {
        FolderDirectoryEntity d = addDir("Vacia");
        d.setColor("#9b2247");
        when(directoryRepository.save(any(FolderDirectoryEntity.class))).thenAnswer(inv -> inv.getArgument(0));

        assertThat(service.setColor(folder.getId(), "Vacia", null).color()).isNull();
    }

    @Test
    void renombrarConservaElColorDeLaSubcarpeta() throws IOException {
        addFile("Docs", "a.txt");
        FolderDirectoryEntity d = addDir("Docs/Sub");
        d.setColor("#9b2247");

        service.rename(folder.getId(), "Docs", "Informes");

        assertThat(d.getPath()).isEqualTo("Informes/Sub");
        assertThat(d.getColor()).isEqualTo("#9b2247");
    }
}
