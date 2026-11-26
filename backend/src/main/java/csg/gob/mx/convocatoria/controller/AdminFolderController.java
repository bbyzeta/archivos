package csg.gob.mx.convocatoria.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import csg.gob.mx.convocatoria.dto.ColorRequest;
import csg.gob.mx.convocatoria.dto.DirectoryColorRequest;
import csg.gob.mx.convocatoria.dto.DirectoryRequest;
import csg.gob.mx.convocatoria.dto.DirectoryResponse;
import csg.gob.mx.convocatoria.dto.FolderFileResponse;
import csg.gob.mx.convocatoria.dto.FolderRequest;
import csg.gob.mx.convocatoria.dto.FolderResponse;
import csg.gob.mx.convocatoria.dto.RenameDirectoryRequest;
import csg.gob.mx.convocatoria.service.FolderDirectoryService;
import csg.gob.mx.convocatoria.service.FolderFileService;
import csg.gob.mx.convocatoria.service.FolderService;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/admin/folders")
public class AdminFolderController {

    private final FolderService folderService;
    private final FolderFileService folderFileService;
    private final FolderDirectoryService folderDirectoryService;

    public AdminFolderController(FolderService folderService, FolderFileService folderFileService,
            FolderDirectoryService folderDirectoryService) {
        this.folderService = folderService;
        this.folderFileService = folderFileService;
        this.folderDirectoryService = folderDirectoryService;
    }

    @GetMapping
    public List<FolderResponse> list() {
        return folderService.findAll();
    }

    @GetMapping("/{id}")
    public FolderResponse get(@PathVariable UUID id) {
        return folderService.findById(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public FolderResponse create(@Valid @RequestBody FolderRequest request, Authentication authentication) {
        return folderService.create(request, authentication.getName());
    }

    @PutMapping("/{id}")
    public FolderResponse rename(@PathVariable UUID id, @Valid @RequestBody FolderRequest request) {
        return folderService.rename(id, request);
    }

    @PatchMapping("/{id}/color")
    public FolderResponse setColor(@PathVariable UUID id, @Valid @RequestBody ColorRequest request) {
        return folderService.setColor(id, request.color());
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID id) {
        folderService.delete(id);
    }

    @GetMapping("/{id}/files")
    public List<FolderFileResponse> listFiles(@PathVariable UUID id) {
        return folderFileService.list(id);
    }

    @PostMapping(path = "/{id}/files", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public FolderFileResponse upload(@PathVariable UUID id, @RequestParam("file") MultipartFile file,
            @RequestParam(value = "relativePath", required = false) String relativePath) {
        return folderFileService.upload(id, file, relativePath);
    }

    @GetMapping("/{id}/directories")
    public List<DirectoryResponse> listDirectories(@PathVariable UUID id) {
        return folderDirectoryService.list(id);
    }

    @PostMapping("/{id}/directories")
    @ResponseStatus(HttpStatus.CREATED)
    public DirectoryRequest createDirectory(@PathVariable UUID id, @Valid @RequestBody DirectoryRequest request) {
        return new DirectoryRequest(folderDirectoryService.create(id, request.path()));
    }

    @PutMapping("/{id}/directories")
    public DirectoryRequest renameDirectory(@PathVariable UUID id, @Valid @RequestBody RenameDirectoryRequest request) {
        return new DirectoryRequest(folderDirectoryService.rename(id, request.path(), request.name()));
    }

    @PutMapping("/{id}/directories/color")
    public DirectoryResponse setDirectoryColor(@PathVariable UUID id, @Valid @RequestBody DirectoryColorRequest request) {
        return folderDirectoryService.setColor(id, request.path(), request.color());
    }

    @DeleteMapping("/{id}/directories")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteDirectory(@PathVariable UUID id, @RequestParam("path") String path,
            @RequestParam(value = "recursive", defaultValue = "false") boolean recursive) {
        folderDirectoryService.delete(id, path, recursive);
    }

    @DeleteMapping("/{id}/files/{fileId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteFile(@PathVariable UUID id, @PathVariable UUID fileId) {
        folderFileService.delete(id, fileId);
    }
}
