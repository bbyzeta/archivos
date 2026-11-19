package csg.gob.mx.convocatoria.controller;

import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@RestController
@RequestMapping("/api/archivos")
public class FileViewController {

    private String getBaseDirectory() {
        String os = System.getProperty("os.name").toLowerCase();
        if (os.contains("win")) {
            return "C:/convocatoria_docs";
        } else {
            return "/var/convocatoria_docs";
        }
    }

    @GetMapping("/view/{tipo}/{nombreArchivo}")
    public ResponseEntity<Resource> verArchivo(
            @PathVariable("tipo") String tipo, 
            @PathVariable("nombreArchivo") String nombreArchivo
    ) {
        try {
            Path rutaArchivo = Paths.get(getBaseDirectory(), tipo).resolve(nombreArchivo).normalize();
            Resource recurso = new UrlResource(rutaArchivo.toUri());

            if (!recurso.exists() || !recurso.isReadable()) {
                return ResponseEntity.notFound().build();
            }

            String contentType = Files.probeContentType(rutaArchivo);
            if (contentType == null) {
                contentType = "application/octet-stream";
            }

            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(contentType))
                    .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + recurso.getFilename() + "\"")
                    .body(recurso);

        } catch (MalformedURLException e) {
            return ResponseEntity.badRequest().build();
        } catch (IOException e) {
            return ResponseEntity.internalServerError().build();
        }
    }
}