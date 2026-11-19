package csg.gob.mx.convocatoria.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import csg.gob.mx.convocatoria.entity.EvaluacionPostulanteEntity;

@Repository
public interface EvaluacionPostulanteRepository extends JpaRepository<EvaluacionPostulanteEntity, Integer> {

   
}