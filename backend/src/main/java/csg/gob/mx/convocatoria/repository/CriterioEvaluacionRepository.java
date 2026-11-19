package csg.gob.mx.convocatoria.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import csg.gob.mx.convocatoria.dto.CriterioEvaluacionProjection;
import csg.gob.mx.convocatoria.entity.CriterioEvaluacionEntity;

@Repository
public interface CriterioEvaluacionRepository extends JpaRepository<CriterioEvaluacionEntity, Integer> {

    List<CriterioEvaluacionEntity> findByIdConvocatoriaAndActivaTrue(Long idConvocatoria);

    List<CriterioEvaluacionEntity> findByIdConvocatoria(Long idConvocatoria);
    
    @Query("""
    	    SELECT 
    	        c.idCriterio AS idCriterio,
    	        c.idConvocatoria AS idConvocatoria,
    	        c.nombreCriterio AS nombreCriterio,
    	        c.factoresConsiderar AS factoresConsiderar,
    	        c.porcentajeMaximo AS porcentajeMaximo,
    	        c.periodo AS periodo,
    	        c.activa AS activa,
    	        e.porcentajeAsignado AS porcentajeAsignado,
    	        e.criterioEvaluado AS criterioEvaluado
    	    FROM CriterioEvaluacionEntity c
    	    LEFT JOIN EvaluacionPostulanteEntity e 
    	        ON c.idCriterio = e.idCriterio     	      
    	       AND e.idUsuario = :idUsuario
    	    WHERE c.idConvocatoria = :idConvocatoria 
    	      AND c.activa = true
    	""")
    	List<CriterioEvaluacionProjection> findCriteriosConEvaluacion(
    	        @Param("idConvocatoria") Long idConvocatoria,    	        
    	        @Param("idUsuario") Long idUsuario
    	);
}