package csg.gob.mx.convocatoria.repository;

import csg.gob.mx.convocatoria.entity.PostulacionExpedienteEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PostulacionExpedienteRepository extends JpaRepository<PostulacionExpedienteEntity, Long> {
    Optional<PostulacionExpedienteEntity> findByIdConvocatoriaAndIdUsuario(Long idConvocatoria, Long idUsuario);
    Optional<PostulacionExpedienteEntity> findByFolio(String folio);
    List<PostulacionExpedienteEntity> findByIdConvocatoria(Long idConvocatoria);
    boolean existsByIdConvocatoriaAndIdUsuario(Long idConvocatoria, Long idUsuario);
}