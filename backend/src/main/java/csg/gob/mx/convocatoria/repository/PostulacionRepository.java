package csg.gob.mx.convocatoria.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import csg.gob.mx.convocatoria.entity.PostulacionExpedienteEntity;

@Repository
public interface PostulacionRepository extends JpaRepository<PostulacionExpedienteEntity, Long> {

    Optional<PostulacionExpedienteEntity> findByIdConvocatoriaAndIdUsuario(Long idConvocatoria, Long idUsuario);

    @Query("SELECT p FROM PostulacionExpedienteEntity p " +
           "JOIN ConvocatoriaEntity c ON c.id = p.idConvocatoria " +
           "LEFT JOIN ParticipanteEntity part ON part.usuario.idUsuario = p.idUsuario " +
           "WHERE c.activa = true " +
           "AND c.periodo = :anio " +
           "AND p.activo = true " +
           "AND (:idConvocatoria IS NULL OR p.idConvocatoria = :idConvocatoria) " +
           "AND (:estado IS NULL OR p.estado = :estado) " +
           "AND (:busqueda IS NULL OR LOWER(p.folio) LIKE LOWER(CONCAT('%', :busqueda, '%')) " +
           "    OR LOWER(p.tituloProyecto) LIKE LOWER(CONCAT('%', :busqueda, '%')) " +
           "    OR LOWER(part.nombres) LIKE LOWER(CONCAT('%', :busqueda, '%')) " +
           "    OR LOWER(part.apellidos) LIKE LOWER(CONCAT('%', :busqueda, '%')) " +
           "    OR LOWER(part.curp) LIKE LOWER(CONCAT('%', :busqueda, '%')))")
    Page<PostulacionExpedienteEntity> buscarExpedientesAuditor(
            @Param("busqueda") String busqueda,
            @Param("idConvocatoria") Long idConvocatoria,
            @Param("estado") String estado,
            @Param("anio") Integer anio,
            Pageable pageable
    );
    
    @Query("SELECT p FROM PostulacionExpedienteEntity p " +
 	       "JOIN ConvocatoriaEntity c ON c.id = p.idConvocatoria " +
 	       "LEFT JOIN ParticipanteEntity part ON part.usuario.idUsuario = p.idUsuario " +
 	       "WHERE c.activa = true " +
 	       "AND c.periodo = :anio " +
 	       "AND p.activo = true " +
 	       "AND (:idConvocatoria IS NULL OR p.idConvocatoria = :idConvocatoria) " +
 	       "AND (COALESCE(:idsCategorias, NULL) IS NULL OR p.idConvocatoria IN :idsCategorias) " +
 	       "AND (:estado IS NULL OR p.estado = :estado) " +
 	       "AND (:busqueda IS NULL OR LOWER(p.folio) LIKE LOWER(CONCAT('%', :busqueda, '%')) " +
 	       "    OR LOWER(p.tituloProyecto) LIKE LOWER(CONCAT('%', :busqueda, '%')) " +
 	       "    OR LOWER(part.nombres) LIKE LOWER(CONCAT('%', :busqueda, '%')) " +
 	       "    OR LOWER(part.apellidos) LIKE LOWER(CONCAT('%', :busqueda, '%')) " +
 	       "    OR LOWER(part.curp) LIKE LOWER(CONCAT('%', :busqueda, '%')))")
 	Page<PostulacionExpedienteEntity> buscarExpedientesJuradoAuditor(
 	        @Param("busqueda") String busqueda,
 	        @Param("idConvocatoria") Long idConvocatoria,
 	        @Param("idsCategorias") List<Integer> idsCategorias,
 	        @Param("estado") String estado,
 	        @Param("anio") Integer anio,
 	        Pageable pageable
 	);
    
    @Query(value = """
    	    SELECT COALESCE(SUM(ep.porcentaje_asignado), 0) 
    	    FROM criterios_evaluacion ce 
    	    INNER JOIN evaluacion_postulante ep ON ep.id_criterio = ce.id_criterio 
    	    WHERE ep.id_usuario = :idUsuario
    	      AND ce.id_convocatoria = :idConvocatoria
    	""", nativeQuery = true)
    	Integer obtenerTotalPorcentajeAsignado(
    	    @Param("idUsuario") Integer idUsuario,
    	    @Param("idConvocatoria") Integer idConvocatoria
    	);
    
    @Query(value = """
    	    SELECT CASE 
    	        WHEN (
    	            SELECT COUNT(1) 
    	            FROM argumentos_postulante ap 
    	            WHERE ap.id_convocatoria = :idConvocatoria 
    	              AND ap.id_usuario = :idUsuario 
    	              AND ap.id_juez = :idJuez
    	        ) > 0 THEN 2

    	        WHEN (
    	            SELECT COUNT(1) 
    	            FROM evaluacion_postulante ep
    	            INNER JOIN criterios_evaluacion ce ON ce.id_criterio = ep.id_criterio
    	            WHERE ce.id_convocatoria = :idConvocatoria 
    	              AND ep.id_usuario = :idUsuario
    	        ) > 0 THEN 1

    	        ELSE 0
    	    END
    	""", nativeQuery = true)
    	int esPostulantePropuesto(
    	    @Param("idConvocatoria") Integer idConvocatoria,
    	    @Param("idUsuario") Integer idUsuario,
    	    @Param("idJuez") Integer idJuez
    	);
}