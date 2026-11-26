package csg.gob.mx.convocatoria.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import csg.gob.mx.convocatoria.entity.FolderEntity;

public interface FolderRepository extends JpaRepository<FolderEntity, UUID> {
    Optional<FolderEntity> findByPublicSlug(String publicSlug);
}
