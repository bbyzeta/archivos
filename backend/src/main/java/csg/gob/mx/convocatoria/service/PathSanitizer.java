package csg.gob.mx.convocatoria.service;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

/** Saneamiento de rutas relativas dentro de una carpeta (compartido por archivos y subcarpetas). */
final class PathSanitizer {

    /** Caracteres no válidos en nombres de archivo (Windows/Linux) y de control. */
    static final Pattern INVALID_NAME_CHARS = Pattern.compile("[<>:\"|?*\\x00-\\x1f]");
    static final int MAX_RELATIVE_PATH = 300;

    private PathSanitizer() {}

    /** Devuelve la ruta normalizada con '/', o "" para la raíz. Rechaza ".." y caracteres inválidos. */
    static String relativePath(String raw) {
        if (raw == null || raw.isBlank()) return "";
        List<String> segments = new ArrayList<>();
        for (String segment : raw.replace('\\', '/').split("/")) {
            String s = segment.trim();
            if (s.isEmpty() || s.equals(".")) continue;
            if (s.equals("..") || INVALID_NAME_CHARS.matcher(s).find()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Ruta relativa no permitida.");
            }
            segments.add(s);
        }
        String joined = String.join("/", segments);
        if (joined.length() > MAX_RELATIVE_PATH) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La ruta relativa es demasiado larga.");
        }
        return joined;
    }
}
