package csg.gob.mx.convocatoria.controller;

import java.nio.charset.StandardCharsets;
import java.util.Set;

import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.HandlerMapping;
import org.springframework.web.util.UriUtils;

import csg.gob.mx.convocatoria.dto.PublicFolderResponse;
import csg.gob.mx.convocatoria.service.FolderFileService;
import csg.gob.mx.convocatoria.service.FolderFileService.PublicFile;
import jakarta.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("/api/public/{slug}")
public class PublicFolderController {

    /** Tipos que el navegador puede mostrar sin riesgo; el resto se fuerza como descarga. */
    private static final Set<String> INLINE_PREFIXES = Set.of("image/", "audio/", "video/", "text/plain");
    private static final String FILE_PATTERN = "/api/public/{slug}/file/**";

    private final FolderFileService folderFileService;

    public PublicFolderController(FolderFileService folderFileService) {
        this.folderFileService = folderFileService;
    }

    @GetMapping
    public PublicFolderResponse get(@PathVariable String slug) {
        return folderFileService.getPublicFolder(slug);
    }

    @GetMapping("/file/**")
    public ResponseEntity<Resource> file(@PathVariable String slug, @RequestParam(defaultValue = "false") boolean download,
            HttpServletRequest request) {
        String path = (String) request.getAttribute(HandlerMapping.PATH_WITHIN_HANDLER_MAPPING_ATTRIBUTE);
        String encoded = new AntPathMatcher().extractPathWithinPattern(FILE_PATTERN, path);
        String fullPath = UriUtils.decode(encoded, StandardCharsets.UTF_8);

        PublicFile file = folderFileService.getPublicFile(slug, fullPath);

        String mime = file.mimeType();
        boolean inline = !download && (mime.equals("application/pdf")
                || INLINE_PREFIXES.stream().anyMatch(mime::startsWith));
        ContentDisposition disposition = (inline ? ContentDisposition.inline() : ContentDisposition.attachment())
                .filename(file.filename(), StandardCharsets.UTF_8).build();

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString())
                .contentType(MediaType.parseMediaType(mime))
                .body(file.resource());
    }
}
