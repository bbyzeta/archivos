package csg.gob.mx.convocatoria.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import csg.gob.mx.convocatoria.entity.FolderFileEntity;

public interface FolderFileRepository extends JpaRepository<FolderFileEntity, UUID> {
    List<FolderFileEntity> findByFolderId(UUID folderId);

    List<FolderFileEntity> findByFolderIdOrderByUploadedAtDesc(UUID folderId);

    Optional<FolderFileEntity> findByIdAndFolderId(UUID id, UUID folderId);

    Optional<FolderFileEntity> findByFolderIdAndRelativePathAndFilename(UUID folderId, String relativePath, String filename);

    boolean existsByFolderIdAndRelativePath(UUID folderId, String relativePath);

    boolean existsByFolderIdAndRelativePathStartingWith(UUID folderId, String prefix);

    boolean existsByFolderIdAndRelativePathAndFilename(UUID folderId, String relativePath, String filename);
}
