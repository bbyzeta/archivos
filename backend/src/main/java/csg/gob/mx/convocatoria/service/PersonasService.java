package csg.gob.mx.convocatoria.service;

import java.util.List;

import csg.gob.mx.convocatoria.dto.PersonaDTO;



public interface PersonasService {
    List<PersonaDTO> findAll();
    PersonaDTO findById(Integer id);
    PersonaDTO save(PersonaDTO personasDTO);
    PersonaDTO update(Integer id, PersonaDTO personasDTO);
    void deleteById(Integer id);
}