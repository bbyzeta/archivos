package csg.gob.mx.convocatoria.repository;


import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import csg.gob.mx.convocatoria.entity.ArgumentoPostulanteEntity;

@Repository
public interface ArgumentoPostulanteRepository extends JpaRepository<ArgumentoPostulanteEntity, Long> {

    List<ArgumentoPostulanteEntity> findByIdUsuarioAndIdJuez(Long idUsuario, Long idJuez);

    Optional<ArgumentoPostulanteEntity> findByIdConvocatoriaAndIdUsuarioAndIdJuezAndIdCriterio(
            Long idConvocatoria, Long idUsuario, Long idJuez, Integer idCriterio
    );
    
    Optional<ArgumentoPostulanteEntity> findByIdConvocatoriaAndIdUsuarioAndIdJuez(
            Long idConvocatoria, Long idUsuario, Long idJuez
    );
    
    List<ArgumentoPostulanteEntity> findByIdJuezAndIdConvocatoria(Long idJuez, Long idConvocatoria);
    
    @Query(value = """
    	    SELECT CASE 
    	        WHEN COUNT(DISTINCT ap.id_usuario) >= 3 THEN 1
    	        WHEN COUNT(CASE WHEN ap.id_usuario = :idUsuario THEN 1 END) > 0 THEN 1
    	        ELSE 0
    	    END 
    	    FROM argumentos_postulante ap
    	    WHERE ap.id_convocatoria = :idConvocatoria
    	      AND ap.id_juez = :idJuez
    	""", nativeQuery = true)
    	int existeUsuarioOLimiteAlcanzado(
    	    @Param("idConvocatoria") Long idConvocatoria,
    	    @Param("idJuez") Long idJuez,
    	    @Param("idUsuario") Long idUsuario
    	);
}