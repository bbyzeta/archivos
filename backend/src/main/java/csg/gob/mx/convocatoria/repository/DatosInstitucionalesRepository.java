package csg.gob.mx.convocatoria.repository;


import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import csg.gob.mx.convocatoria.entity.DatosInstitucionalesEntity;

@Repository
public interface DatosInstitucionalesRepository extends JpaRepository<DatosInstitucionalesEntity, Long> {
    Optional<DatosInstitucionalesEntity> findByParticipanteIdParticipante(Long idParticipante);
}