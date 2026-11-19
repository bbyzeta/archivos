package csg.gob.mx.convocatoria.repository;


import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import csg.gob.mx.convocatoria.entity.ConvocatoriaEntity;

@Repository
public interface ConvocatoriaRepository extends JpaRepository<ConvocatoriaEntity, Long> {
    
	@Modifying(clearAutomatically = true)
    @Query("UPDATE ConvocatoriaEntity c SET c.activa = false WHERE c.activa = true")
    void desactivarTodasLasConvocatorias();
	
	@Query("SELECT c FROM ConvocatoriaEntity c WHERE " +
		       "(:termino IS NULL OR " +
		       "LOWER(c.titulo) LIKE LOWER(CONCAT('%', :termino, '%')) OR " +
		       "LOWER(c.tituloBlog) LIKE LOWER(CONCAT('%', :termino, '%')) OR " +
		       "CAST(c.periodo as string) LIKE CONCAT('%', :termino, '%')) " +
		       "ORDER BY c.id DESC")
	Page<ConvocatoriaEntity> buscarPaginado(@Param("termino") String termino, Pageable pageable);

	Optional<ConvocatoriaEntity> findByActivaTrue();
	
	List<ConvocatoriaEntity> findByIdConvocatoriaGeneralAndActivaAndPeriodo(Integer idCategoria,Boolean activa, Integer periodo);
	
	
	List<ConvocatoriaEntity> findByIdInAndPeriodo(List<Integer> idsConvocatoria, Integer periodo);
	   
}
