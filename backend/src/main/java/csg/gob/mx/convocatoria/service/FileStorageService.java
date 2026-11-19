package csg.gob.mx.convocatoria.service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class FileStorageService {

    private String getBaseDirectory() {
        String os = System.getProperty("os.name").toLowerCase();
        if (os.contains("win")) {
            return "C:/convocatoria_docs";
        } else {
            return "/var/convocatoria_docs";
        }
    }

    public String guardarArchivo(MultipartFile file, String subcarpeta) throws IOException {
        if (file == null || file.isEmpty()) {
            return null;
        }

        String basePath = getBaseDirectory();
        Path targetFolder = Paths.get(basePath, subcarpeta);

        if (!Files.exists(targetFolder)) {
            Files.createDirectories(targetFolder);
        }

        String originalFilename = file.getOriginalFilename();
        String extension = "";
        if (originalFilename != null && originalFilename.contains(".")) {
            extension = originalFilename.substring(originalFilename.lastIndexOf("."));
        }
        
        String nombreUnico = UUID.randomUUID().toString() + extension;
        Path targetPath = targetFolder.resolve(nombreUnico);

        Files.copy(file.getInputStream(), targetPath, StandardCopyOption.REPLACE_EXISTING);

        return nombreUnico;
    }
}