package csg.gob.mx.convocatoria.mapper;


import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import csg.gob.mx.convocatoria.dto.CriterioEvaluacionDTO;
import csg.gob.mx.convocatoria.dto.CriterioEvaluacionProjection;
import csg.gob.mx.convocatoria.dto.CriterioEvaluacionRequestDTO;
import csg.gob.mx.convocatoria.entity.CriterioEvaluacionEntity;

@Component
public class CriterioEvaluacionMapper {

    public CriterioEvaluacionDTO toDTO(CriterioEvaluacionEntity entity) {
        if (entity == null) return null;

        return CriterioEvaluacionDTO.builder()
                .idCriterio(entity.getIdCriterio())
                .idConvocatoria(entity.getIdConvocatoria())
                .nombreCriterio(entity.getNombreCriterio())
                .factoresConsiderar(entity.getFactoresConsiderar())
                .porcentajeMaximo(entity.getPorcentajeMaximo())
                .periodo(entity.getPeriodo())
                .activa(entity.getActiva())
                .build();
    }

    public CriterioEvaluacionEntity toEntity(CriterioEvaluacionRequestDTO dto) {
        if (dto == null) return null;

        return CriterioEvaluacionEntity.builder()
                .idConvocatoria(dto.getIdConvocatoria())
                .nombreCriterio(dto.getNombreCriterio())
                .factoresConsiderar(dto.getFactoresConsiderar())
                .porcentajeMaximo(dto.getPorcentajeMaximo())
                .periodo(dto.getPeriodo())
                .activa(dto.getActiva() != null ? dto.getActiva() : true)
                .build();
    }

    public List<CriterioEvaluacionDTO> toDTOList(List<CriterioEvaluacionEntity> entities) {
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
    }
}