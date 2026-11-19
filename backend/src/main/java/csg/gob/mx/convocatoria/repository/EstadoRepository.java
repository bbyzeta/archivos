package csg.gob.mx.convocatoria.repository;


import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import csg.gob.mx.convocatoria.entity.EstadoEntity;

@Repository
public interface EstadoRepository extends JpaRepository<EstadoEntity, Long> {
    
    List<EstadoEntity> findAllByOrderByNombreAsc();
}