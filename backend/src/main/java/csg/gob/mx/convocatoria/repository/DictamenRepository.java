package csg.gob.mx.convocatoria.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import csg.gob.mx.convocatoria.dto.CriterioEvaluadoDTO;
import csg.gob.mx.convocatoria.entity.ConvocatoriaEntity;

@Repository
public interface DictamenRepository extends JpaRepository<ConvocatoriaEntity, Long> {

   
	@Query(value = """
	        SELECT DISTINCT 
	            c.id_convocatoria AS idConvocatoria,
	            c.titulo AS tituloConvocatoria,
	            c.periodo AS periodo,
	            pj.usuario_id AS idJuez,
	            CONCAT(pj.grado_academico, ' ', pj.nombres, ' ', pj.apellidos) AS nombreJuez,
	            p.id_usuario AS idPostulante,
	            CONCAT(p.nombres, ' ', p.apellidos) AS nombrePostulante,
	            p.curp AS curpPostulante,
	            COALESCE((
	                SELECT SUM(ep_sub.porcentaje_asignado)
	                FROM evaluacion_postulante ep_sub
	                INNER JOIN criterios_evaluacion ce_sub ON ce_sub.id_criterio = ep_sub.id_criterio
	                WHERE ce_sub.id_convocatoria = c.id_convocatoria 
	                  AND ep_sub.id_usuario = p.id_usuario
	            ), 0) AS porcentajeTotal,
	            2 AS estadoEvaluacion,
	            MAX(ap.argumento) AS argumentoGeneral
	        FROM convocatorias c
	        INNER JOIN argumentos_postulante ap ON ap.id_convocatoria = c.id_convocatoria
	        INNER JOIN participantes p ON p.id_usuario = ap.id_usuario
	        LEFT JOIN criterios_evaluacion ce ON ce.id_convocatoria = c.id_convocatoria
	        LEFT JOIN evaluacion_postulante ep ON ep.id_criterio = ce.id_criterio AND ep.id_usuario = p.id_usuario
	        LEFT JOIN perfil_jurado pj ON pj.usuario_id = ap.id_juez
	        WHERE (:idConvocatoria IS NULL OR c.id_convocatoria = :idConvocatoria)
	          AND (:idJuez IS NULL OR ap.id_juez = :idJuez)
	          AND (:termino IS NULL OR :termino = '' OR 
	               LOWER(p.nombres) LIKE LOWER(CONCAT('%', :termino, '%')) OR 
	               LOWER(p.apellidos) LIKE LOWER(CONCAT('%', :termino, '%')) OR 
	               LOWER(p.curp) LIKE LOWER(CONCAT('%', :termino, '%')) OR
	               LOWER(c.titulo) LIKE LOWER(CONCAT('%', :termino, '%')))
	        GROUP BY c.id_convocatoria, c.titulo, c.periodo, pj.usuario_id, pj.grado_academico, 
	                 pj.nombres, pj.apellidos, p.id_usuario, p.nombres, p.apellidos, p.curp
	    """, nativeQuery = true)
	    List<Object[]> obtenerCabeceraDictamenesRaw(
	        @Param("idConvocatoria") Long idConvocatoria,
	        @Param("idJuez") Long idJuez,
	        @Param("termino") String termino
	    );

	    @Query("SELECT new csg.gob.mx.convocatoria.dto.CriterioEvaluadoDTO(" +
	    	       "ce.idCriterio, " +
	    	       "ce.nombreCriterio, " +
	    	       "ce.factoresConsiderar, " +
	    	       "ce.porcentajeMaximo, " +
	    	       "COALESCE(ep.porcentajeAsignado, 0), " +
	    	       "(SELECT ap.argumento FROM ArgumentoPostulanteEntity ap " +
	    	       " WHERE ap.idCriterio = ce.idCriterio " +
	    	       "   AND ap.idUsuario = :idPostulante " +
	    	       "   AND ap.idConvocatoria = :idConvocatoria)) " + 
	    	       "FROM CriterioEvaluacionEntity ce " +
	    	       "LEFT JOIN EvaluacionPostulanteEntity ep " +
	    	       "  ON ep.idCriterio = ce.idCriterio " +
	    	       "  AND ep.idUsuario = :idPostulante " +
	    	       "WHERE ce.idConvocatoria = :idConvocatoria")
	    	List<CriterioEvaluadoDTO> obtenerCriteriosPorPostulante(
	    	    @Param("idPostulante") Long idPostulante,
	    	    @Param("idConvocatoria") Long idConvocatoria
	    	);
}