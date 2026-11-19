package csg.gob.mx.convocatoria.mapper;


import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import csg.gob.mx.convocatoria.dto.ArgumentoPostulanteRequestDTO;
import csg.gob.mx.convocatoria.dto.ArgumentoPostulanteResponseDTO;
import csg.gob.mx.convocatoria.entity.ArgumentoPostulanteEntity;

@Component
public class ArgumentoPostulanteMapper {

    public ArgumentoPostulanteEntity toEntity(ArgumentoPostulanteRequestDTO dto) {
        if (dto == null) return null;

        ArgumentoPostulanteEntity entity = new ArgumentoPostulanteEntity();
        entity.setIdConvocatoria(dto.getIdConvocatoria());
        entity.setIdCriterio(dto.getIdCriterio());
        entity.setIdUsuario(dto.getIdUsuario());
        entity.setIdJuez(dto.getIdJuez());
        entity.setArgumento(dto.getArgumento());
        return entity;
    }

    public ArgumentoPostulanteResponseDTO toDto(ArgumentoPostulanteEntity entity) {
        if (entity == null) return null;

        ArgumentoPostulanteResponseDTO dto = new ArgumentoPostulanteResponseDTO();
        dto.setIdArgumento(entity.getIdArgumento());
        dto.setIdConvocatoria(entity.getIdConvocatoria());
        dto.setIdCriterio(entity.getIdCriterio());
        dto.setIdUsuario(entity.getIdUsuario());
        dto.setIdJuez(entity.getIdJuez());
        dto.setArgumento(entity.getArgumento());
        dto.setFechaRegistro(entity.getFechaRegistro());
        return dto;
    }

    public List<ArgumentoPostulanteResponseDTO> toDtoList(List<ArgumentoPostulanteEntity> entities) {
        return entities.stream().map(this::toDto).collect(Collectors.toList());
    }
}
