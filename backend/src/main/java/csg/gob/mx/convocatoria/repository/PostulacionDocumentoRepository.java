package csg.gob.mx.convocatoria.repository;

import csg.gob.mx.convocatoria.entity.PostulacionDocumentoEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PostulacionDocumentoRepository extends JpaRepository<PostulacionDocumentoEntity, Long> {

    @Query("SELECT d FROM PostulacionDocumentoEntity d " +
           "WHERE d.postulacion.id = :idPostulacion AND d.requisito.idRequisito = :idRequisito")
    Optional<PostulacionDocumentoEntity> findByPostulacionIdAndRequisitoId(
            @Param("idPostulacion") Long idPostulacion, 
            @Param("idRequisito") Long idRequisito
    );
    
    @Query("""
    	    SELECT CASE WHEN (
    	        (SELECT COUNT(d) FROM PostulacionDocumentoEntity d 
    	         WHERE d.postulacion.id = :idPostulacion 
    	           AND LOWER(TRIM(d.estadoRevision)) = 'con_observaciones') > 0
    	        OR
    	        (SELECT COUNT(a) FROM PostulacionAnexoEntity a 
    	         WHERE a.postulacion.id = :idPostulacion 
    	           AND LOWER(TRIM(a.estadoRevision)) = 'con_observaciones') > 0
    	    ) THEN true ELSE false END
    	""")
    	boolean tieneRevisionesRealizadas(@Param("idPostulacion") Long idPostulacion);
}