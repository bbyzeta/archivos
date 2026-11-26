package csg.gob.mx.convocatoria.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

class PathSanitizerTest {

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   ", "/", "//", "./", "."})
    void rutasVaciasSeNormalizanALaRaiz(String raw) {
        assertThat(PathSanitizer.relativePath(raw)).isEmpty();
    }

    @Test
    void normalizaSeparadoresYSegmentosRedundantes() {
        assertThat(PathSanitizer.relativePath("Docs/2026")).isEqualTo("Docs/2026");
        assertThat(PathSanitizer.relativePath("\\Docs\\\\2026\\")).isEqualTo("Docs/2026");
        assertThat(PathSanitizer.relativePath("a/./b//c")).isEqualTo("a/b/c");
        assertThat(PathSanitizer.relativePath("  a / b ")).isEqualTo("a/b");
    }

    @ParameterizedTest
    @ValueSource(strings = {"..", "../etc", "a/../b", "a/..", "..\\windows", "a\\..\\..\\b", "/../x"})
    void rechazaPathTraversal(String raw) {
        assertThatThrownBy(() -> PathSanitizer.relativePath(raw))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(e -> ((ResponseStatusException) e).getStatusCode())
                .isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @ParameterizedTest
    @ValueSource(strings = {"C:\\temp", "a:b", "a<b", "a>b", "a|b", "a?b", "a*b", "a\"b", "a\u0000b", "a\u001fb"})
    void rechazaCaracteresNoValidos(String raw) {
        assertThatThrownBy(() -> PathSanitizer.relativePath(raw))
                .isInstanceOf(ResponseStatusException.class);
    }

    @Test
    void aceptaAcentosEspaciosYPuntosEnNombres() {
        assertThat(PathSanitizer.relativePath("Informes 2026/Año nuevo/v1.2")).isEqualTo("Informes 2026/Año nuevo/v1.2");
        assertThat(PathSanitizer.relativePath(".config/archivo")).isEqualTo(".config/archivo");
    }

    @Test
    void respetaElLimiteDeLongitud() {
        String ok = "a".repeat(PathSanitizer.MAX_RELATIVE_PATH);
        assertThat(PathSanitizer.relativePath(ok)).hasSize(PathSanitizer.MAX_RELATIVE_PATH);

        String tooLong = "a".repeat(PathSanitizer.MAX_RELATIVE_PATH + 1);
        assertThatThrownBy(() -> PathSanitizer.relativePath(tooLong)).isInstanceOf(ResponseStatusException.class);
    }
}
