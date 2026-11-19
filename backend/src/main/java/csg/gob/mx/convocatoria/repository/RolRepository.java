package csg.gob.mx.convocatoria.repository;


import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import csg.gob.mx.convocatoria.entity.RolEntity;

@Repository
public interface RolRepository extends JpaRepository<RolEntity, Integer> {
}
