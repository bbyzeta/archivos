package csg.gob.mx.convocatoria.repository;


import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import csg.gob.mx.convocatoria.entity.ConvocatoriaGeneralEntity;

@Repository
public interface ConvocatoriaGeneralRepository extends JpaRepository<ConvocatoriaGeneralEntity, Long> {
	List<ConvocatoriaGeneralEntity> findByActivaTrue();
}
