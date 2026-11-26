package csg.gob.mx.convocatoria.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import csg.gob.mx.convocatoria.entity.FolderDirectoryEntity;

public interface FolderDirectoryRepository extends JpaRepository<FolderDirectoryEntity, UUID> {
    List<FolderDirectoryEntity> findByFolderIdOrderByPathAsc(UUID folderId);

    boolean existsByFolderIdAndPath(UUID folderId, String path);
}
