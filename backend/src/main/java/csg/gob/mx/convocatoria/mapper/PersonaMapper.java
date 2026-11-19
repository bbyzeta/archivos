package csg.gob.mx.convocatoria.mapper;


import org.springframework.stereotype.Component;

import csg.gob.mx.convocatoria.dto.PersonaDTO;
import csg.gob.mx.convocatoria.entity.PersonaEntity;

@Component
public class PersonaMapper {


    public PersonaDTO toDTO(PersonaEntity entity) {
        if (entity == null) {
            return null;
        }
        
        return new PersonaDTO(
            entity.getIdPersona(),
            entity.getNombre(),
            entity.getCargo(),
            entity.getInstitucionDependencia()
        );
    }


    public PersonaEntity toEntity(PersonaDTO dto) {
        if (dto == null) {
            return null;
        }

        PersonaEntity entity = new PersonaEntity();
        entity.setIdPersona(dto.idPersona());
        entity.setNombre(dto.nombre());
        entity.setCargo(dto.cargo());
        entity.setInstitucionDependencia(dto.institucionDependencia());
        return entity;
    }
}
