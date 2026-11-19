package csg.gob.mx.convocatoria.repository;


import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import csg.gob.mx.convocatoria.entity.ParticipanteEntity;

@Repository
public interface ParticipanteRepository extends JpaRepository<ParticipanteEntity, Long> {

    boolean existsByCurp(String curp);

    Optional<ParticipanteEntity> findByUsuario_IdUsuario(Long idUsuario);

    Optional<ParticipanteEntity> findByCurp(String curp);
    
    @Query(value = "SELECT p FROM ParticipanteEntity p JOIN p.usuario u WHERE " +
            "LOWER(p.nombres) LIKE LOWER(CONCAT('%', :filtro, '%')) OR " +
            "LOWER(p.apellidos) LIKE LOWER(CONCAT('%', :filtro, '%')) OR " +
            "LOWER(p.curp) LIKE LOWER(CONCAT('%', :filtro, '%')) OR " +
            "LOWER(u.username) LIKE LOWER(CONCAT('%', :filtro, '%'))",
            countQuery = "SELECT COUNT(p) FROM ParticipanteEntity p JOIN p.usuario u WHERE " +
                         "LOWER(p.nombres) LIKE LOWER(CONCAT('%', :filtro, '%')) OR " +
                         "LOWER(p.apellidos) LIKE LOWER(CONCAT('%', :filtro, '%')) OR " +
                         "LOWER(p.curp) LIKE LOWER(CONCAT('%', :filtro, '%')) OR " +
                         "LOWER(u.username) LIKE LOWER(CONCAT('%', :filtro, '%'))")
     Page<ParticipanteEntity> buscarConFiltro(@Param("filtro") String filtro, Pageable pageable);
}