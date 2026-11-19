package csg.gob.mx.convocatoria.repository;

import csg.gob.mx.convocatoria.entity.PostulacionAnexoEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PostulacionAnexoRepository extends JpaRepository<PostulacionAnexoEntity, Long> {
    List<PostulacionAnexoEntity> findByPostulacionId(Long idPostulacion);
    long countByPostulacionId(Long idPostulacion);
}