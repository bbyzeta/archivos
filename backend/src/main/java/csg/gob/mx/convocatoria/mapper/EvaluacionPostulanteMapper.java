package csg.gob.mx.convocatoria.mapper;


import java.time.LocalDateTime;

import org.springframework.stereotype.Component;

import csg.gob.mx.convocatoria.dto.EvaluacionPostulanteDTO;
import csg.gob.mx.convocatoria.entity.EvaluacionPostulanteEntity;

@Component
public class EvaluacionPostulanteMapper {

    public EvaluacionPostulanteDTO toDTO(EvaluacionPostulanteEntity entity) {
        if (entity == null) return null;

        return EvaluacionPostulanteDTO.builder()
                .idEvaluacion(entity.getIdEvaluacion().intValue())
                .idExpediente(entity.getIdExpediente().intValue())
                .idCriterio(entity.getIdCriterio())
                .idUsuario(entity.getIdUsuario().intValue())
                .porcentajeAsignado(entity.getPorcentajeAsignado().intValue())
                .criterioEvaluado(entity.getCriterioEvaluado())
                .fechaEvaluacion(entity.getFechaEvaluacion())
                .build();
    }

    public EvaluacionPostulanteEntity toEntity(EvaluacionPostulanteDTO dto) {
        if (dto == null) return null;

        return EvaluacionPostulanteEntity.builder()                
                .idCriterio(dto.getIdCriterio())
                .idUsuario(Long.parseLong(dto.getIdUsuario().toString()))
                .porcentajeAsignado(dto.getPorcentajeAsignado())
                .fechaEvaluacion(LocalDateTime.now())
                .criterioEvaluado(1)
                .build();
    }

   /* public List<CriterioEvaluacionDTO> toDTOList(List<CriterioEvaluacionEntity> entities) {
        if (entities == null) return List.of();
        return entities.stream().map(this::toDTO).collect(Collectors.toList());
    }
    
    public CriterioEvaluacionDTO projectionToDTO(CriterioEvaluacionProjection projection) {
        if (projection == null) {
            return null;
        }

        return CriterioEvaluacionDTO.builder()
                .idCriterio(projection.getIdCriterio())
                .idConvocatoria(projection.getIdConvocatoria())
                .nombreCriterio(projection.getNombreCriterio())
                .factoresConsiderar(projection.getFactoresConsiderar())
                .porcentajeMaximo(projection.getPorcentajeMaximo())
                .periodo(projection.getPeriodo())
                .activa(projection.getActiva())
                .porcentajeAsignado(projection.getPorcentajeAsignado() != null ? projection.getPorcentajeAsignado() : null)
                .criterioEvaluado(projection.getCriterioEvaluado())
                .build();
    }

    public List<CriterioEvaluacionDTO> projectionListToDTOList(List<CriterioEvaluacionProjection> projections) {
        if (projections == null) {
            return List.of();
        }

        return projections.stream()
                .map(this::projectionToDTO)
                .collect(Collectors.toList());
    }*/
}