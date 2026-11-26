package csg.gob.mx.convocatoria.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.server.ResponseStatusException;

import csg.gob.mx.convocatoria.dto.FolderFileResponse;
import csg.gob.mx.convocatoria.entity.FolderEntity;
import csg.gob.mx.convocatoria.entity.FolderFileEntity;
import csg.gob.mx.convocatoria.repository.FolderFileRepository;
import csg.gob.mx.convocatoria.repository.FolderRepository;

class FolderFileServiceTest {

    @TempDir
    Path storage;

    private FolderRepository folderRepository;
    private FolderFileRepository fileRepository;
    private FolderFileService service;
    private FolderEntity folder;
    private Path folderDir;

    @BeforeEach
    void setUp() throws IOException {
        folderRepository = mock(FolderRepository.class);
        fileRepository = mock(FolderFileRepository.class);
        service = new FolderFileService(folderRepository, fileRepository, mock(FolderDirectoryService.class),
                "http://localhost:8081/");

        folderDir = Files.createDirectories(storage.resolve("abc123"));
        folder = new FolderEntity();
        folder.setId(UUID.randomUUID());
        folder.setName("Documentos");
        folder.setPublicSlug("abc123");
        folder.setDiskPath(folderDir.toString());
        when(folderRepository.findById(folder.getId())).thenReturn(Optional.of(folder));
        when(fileRepository.saveAndFlush(any(FolderFileEntity.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    private MockMultipartFile file(String name, String content) {
        return new MockMultipartFile("file", name, "text/plain", content.getBytes(StandardCharsets.UTF_8));
    }

    private void assertStatus(Throwable t, HttpStatus expected) {
        assertThat(t).isInstanceOf(ResponseStatusException.class);
        assertThat(((ResponseStatusException) t).getStatusCode()).isEqualTo(expected);
    }

    // ---------- subida ----------

    @Test
    void guardaElArchivoDentroDeLaCarpetaYLoRegistra() throws IOException {
        FolderFileResponse response = service.upload(folder.getId(), file("informe.txt", "hola"), null);

        assertThat(Files.readString(folderDir.resolve("informe.txt"))).isEqualTo("hola");
        assertThat(response.filename()).isEqualTo("informe.txt");
        assertThat(response.relativePath()).isEmpty();
        assertThat(response.sizeBytes()).isEqualTo(4);
        assertThat(response.publicUrl()).isEqualTo("http://localhost:8081/api/public/abc123/file/informe.txt");
    }

    @Test
    void creaSubdirectoriosYSaneaLaRutaRelativa() throws IOException {
        FolderFileResponse response = service.upload(folder.getId(), file("a.txt", "x"), "Docs\\./2026//Q1");

        assertThat(response.relativePath()).isEqualTo("Docs/2026/Q1");
        assertThat(folderDir.resolve("Docs/2026/Q1/a.txt")).exists();
        assertThat(response.publicUrl()).endsWith("/file/Docs/2026/Q1/a.txt");
    }

    @Test
    void codificaEnElLinkLosNombresConEspaciosYAcentos() {
        FolderFileResponse response = service.upload(folder.getId(), file("Constancia año 1.txt", "x"), "Mis docs");

        assertThat(response.publicUrl()).endsWith("/file/Mis%20docs/Constancia%20a%C3%B1o%201.txt");
    }

    @Test
    void ignoraRutasIncluidasEnElNombreDelArchivo() throws IOException {
        FolderFileResponse response = service.upload(folder.getId(), file("../../evil.txt", "x"), null);

        assertThat(response.filename()).isEqualTo("evil.txt");
        assertThat(folderDir.resolve("evil.txt")).exists();
        assertThat(storage.resolve("evil.txt")).doesNotExist();
        assertThat(storage.getParent().resolve("evil.txt")).doesNotExist();
    }

    @Test
    void rechazaPathTraversalEnLaRutaRelativaSinEscribirNada() {
        assertThatThrownBy(() -> service.upload(folder.getId(), file("a.txt", "x"), "../fuera"))
                .satisfies(t -> assertStatus(t, HttpStatus.BAD_REQUEST));
        assertThatThrownBy(() -> service.upload(folder.getId(), file("a.txt", "x"), "a/../../fuera"))
                .satisfies(t -> assertStatus(t, HttpStatus.BAD_REQUEST));

        assertThat(storage.resolve("fuera")).doesNotExist();
        verify(fileRepository, never()).saveAndFlush(any());
    }

    @Test
    void rechazaNombresDeArchivoNoValidos() {
        for (String name : new String[] {"..", ".", "a:b.txt", "a|b.txt", "con*.txt", " "}) {
            assertThatThrownBy(() -> service.upload(folder.getId(), file(name, "x"), null))
                    .as("nombre: '%s'", name)
                    .satisfies(t -> assertStatus(t, HttpStatus.BAD_REQUEST));
        }
        verify(fileRepository, never()).saveAndFlush(any());
    }

    @Test
    void aceptaArchivosVacios() throws IOException {
        FolderFileResponse response = service.upload(folder.getId(), file("vacio.txt", ""), null);

        assertThat(response.sizeBytes()).isZero();
        assertThat(folderDir.resolve("vacio.txt")).exists().isEmptyFile();
    }

    @Test
    void rechazaDuplicadosPorCarpetaRutaYNombre() {
        when(fileRepository.existsByFolderIdAndRelativePathAndFilename(folder.getId(), "Docs", "a.txt")).thenReturn(true);

        assertThatThrownBy(() -> service.upload(folder.getId(), file("a.txt", "x"), "Docs"))
                .satisfies(t -> assertStatus(t, HttpStatus.CONFLICT));
        assertThat(folderDir.resolve("Docs/a.txt")).doesNotExist();
        verify(fileRepository, never()).saveAndFlush(any());
    }

    @Test
    void nuncaSobrescribeUnArchivoYaExistenteEnDisco() throws IOException {
        Files.writeString(folderDir.resolve("a.txt"), "original");

        assertThatThrownBy(() -> service.upload(folder.getId(), file("a.txt", "nuevo"), null))
                .satisfies(t -> assertStatus(t, HttpStatus.CONFLICT));
        assertThat(Files.readString(folderDir.resolve("a.txt"))).isEqualTo("original");
    }

    @Test
    void siFallaLaBaseDeDatosNoDejaElArchivoHuerfano() {
        when(fileRepository.saveAndFlush(any(FolderFileEntity.class))).thenThrow(new IllegalStateException("BD caída"));

        assertThatThrownBy(() -> service.upload(folder.getId(), file("a.txt", "x"), null))
                .isInstanceOf(IllegalStateException.class);
        assertThat(folderDir.resolve("a.txt")).doesNotExist();
    }

    @Test
    void unaCarpetaInexistenteDevuelve404() {
        UUID unknown = UUID.randomUUID();
        when(folderRepository.findById(unknown)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.upload(unknown, file("a.txt", "x"), null))
                .satisfies(t -> assertStatus(t, HttpStatus.NOT_FOUND));
    }

    // ---------- eliminación ----------

    @Test
    void eliminarQuitaElRegistroYElArchivoEnDisco() throws IOException {
        Path onDisk = Files.createDirectories(folderDir.resolve("Docs")).resolve("a.txt");
        Files.writeString(onDisk, "x");
        FolderFileEntity entity = entity("Docs", "a.txt");
        when(fileRepository.findByIdAndFolderId(entity.getId(), folder.getId())).thenReturn(Optional.of(entity));

        service.delete(folder.getId(), entity.getId());

        verify(fileRepository).delete(entity);
        assertThat(onDisk).doesNotExist();
    }

    @Test
    void eliminarUnArchivoDeOtraCarpetaDevuelve404() {
        UUID fileId = UUID.randomUUID();
        when(fileRepository.findByIdAndFolderId(fileId, folder.getId())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.delete(folder.getId(), fileId))
                .satisfies(t -> assertStatus(t, HttpStatus.NOT_FOUND));
    }

    // ---------- descarga pública ----------

    @Test
    void sirveUnArchivoRegistradoPorSuRutaCompleta() throws IOException {
        Files.writeString(Files.createDirectories(folderDir.resolve("Docs")).resolve("a.txt"), "x");
        when(folderRepository.findByPublicSlug("abc123")).thenReturn(Optional.of(folder));
        when(fileRepository.findByFolderIdAndRelativePathAndFilename(folder.getId(), "Docs", "a.txt"))
                .thenReturn(Optional.of(entity("Docs", "a.txt")));

        FolderFileService.PublicFile file = service.getPublicFile("abc123", "Docs/a.txt");

        assertThat(file.filename()).isEqualTo("a.txt");
        assertThat(file.mimeType()).isEqualTo("text/plain");
        assertThat(file.resource().exists()).isTrue();
    }

    @Test
    void noSirveArchivosQueNoEstenRegistradosEnBd() throws IOException {
        Files.writeString(folderDir.resolve("secreto.txt"), "x");
        when(folderRepository.findByPublicSlug("abc123")).thenReturn(Optional.of(folder));
        when(fileRepository.findByFolderIdAndRelativePathAndFilename(eq(folder.getId()), anyString(), anyString()))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getPublicFile("abc123", "secreto.txt"))
                .satisfies(t -> assertStatus(t, HttpStatus.NOT_FOUND));
        assertThatThrownBy(() -> service.getPublicFile("abc123", "../abc123/secreto.txt"))
                .satisfies(t -> assertStatus(t, HttpStatus.NOT_FOUND));
    }

    @Test
    void unRegistroCorruptoConRutaFueraDeLaCarpetaNoSeSirve() throws IOException {
        Files.writeString(storage.resolve("fuera.txt"), "privado");
        when(folderRepository.findByPublicSlug("abc123")).thenReturn(Optional.of(folder));
        when(fileRepository.findByFolderIdAndRelativePathAndFilename(folder.getId(), "..", "fuera.txt"))
                .thenReturn(Optional.of(entity("..", "fuera.txt")));

        assertThatThrownBy(() -> service.getPublicFile("abc123", "../fuera.txt"))
                .satisfies(t -> assertStatus(t, HttpStatus.NOT_FOUND));
    }

    @Test
    void siElArchivoRegistradoYaNoEstaEnDiscoDevuelve404() {
        when(folderRepository.findByPublicSlug("abc123")).thenReturn(Optional.of(folder));
        when(fileRepository.findByFolderIdAndRelativePathAndFilename(folder.getId(), "", "a.txt"))
                .thenReturn(Optional.of(entity("", "a.txt")));

        assertThatThrownBy(() -> service.getPublicFile("abc123", "a.txt"))
                .satisfies(t -> assertStatus(t, HttpStatus.NOT_FOUND));
    }

    @Test
    void unSlugInexistenteDevuelve404() {
        when(folderRepository.findByPublicSlug("nada")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getPublicFolder("nada"))
                .satisfies(t -> assertStatus(t, HttpStatus.NOT_FOUND));
    }

    private FolderFileEntity entity(String relativePath, String filename) {
        FolderFileEntity e = new FolderFileEntity();
        e.setId(UUID.randomUUID());
        e.setFolder(folder);
        e.setRelativePath(relativePath);
        e.setFilename(filename);
        e.setMimeType("text/plain");
        e.setSizeBytes(1);
        return e;
    }
}
