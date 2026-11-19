package csg.gob.mx.convocatoria.repository;

import csg.gob.mx.convocatoria.entity.PerfilJuradoEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PerfilJuradoRepository extends JpaRepository<PerfilJuradoEntity, Integer> {

    Optional<PerfilJuradoEntity> findByUsuario_IdUsuario(Long idUsuario);

    @Query("""
        SELECT pj FROM PerfilJuradoEntity pj
        JOIN pj.usuario u
        WHERE (:filtro IS NULL OR :filtro = '' OR
               LOWER(pj.nombres) LIKE LOWER(CONCAT('%', :filtro, '%')) OR
               LOWER(pj.apellidos) LIKE LOWER(CONCAT('%', :filtro, '%')) OR
               LOWER(u.username) LIKE LOWER(CONCAT('%', :filtro, '%')) OR
               LOWER(pj.institucionAdscripcion) LIKE LOWER(CONCAT('%', :filtro, '%')) OR
               LOWER(pj.areaConocimiento) LIKE LOWER(CONCAT('%', :filtro, '%')) OR
               LOWER(pj.rfc) LIKE LOWER(CONCAT('%', :filtro, '%')))
    """)
    Page<PerfilJuradoEntity> buscarConFiltroPaginado(@Param("filtro") String filtro, Pageable pageable);
}