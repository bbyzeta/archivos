package csg.gob.mx.convocatoria.service.impl;




import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import csg.gob.mx.convocatoria.dto.PersonaDTO;
import csg.gob.mx.convocatoria.entity.PersonaEntity;
import csg.gob.mx.convocatoria.mapper.PersonaMapper;
import csg.gob.mx.convocatoria.repository.PersonaRepository;
import csg.gob.mx.convocatoria.service.PersonasService;

@Service
public class PersonasServiceImpl implements PersonasService {

    private final PersonaRepository personaRepository;
    private final PersonaMapper PersonaMapper;

    public PersonasServiceImpl(PersonaRepository personaRepository, PersonaMapper PersonaMapper) {
        this.personaRepository = personaRepository;
        this.PersonaMapper = PersonaMapper;
    }

    @Override
    @Transactional(readOnly = true)
    public List<PersonaDTO> findAll() {
        return personaRepository.findAll()
                .stream()
                .map(PersonaMapper::toDTO)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public PersonaDTO findById(Integer id) {
        PersonaEntity entity = personaRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Persona no encontrada con el ID: " + id));
        return PersonaMapper.toDTO(entity);
    }

    @Override
    @Transactional
    public PersonaDTO save(PersonaDTO PersonaDTO) {
        PersonaEntity entity = PersonaMapper.toEntity(PersonaDTO);
        entity.setIdPersona(null); 
        PersonaEntity guardada = personaRepository.save(entity);
        return PersonaMapper.toDTO(guardada);
    }

    @Override
    @Transactional
    public PersonaDTO update(Integer id, PersonaDTO PersonaDTO) {
        if (!personaRepository.existsById(id)) {
            throw new RuntimeException("No se puede actualizar. Persona no encontrada con el ID: " + id);
        }

        PersonaEntity entity = PersonaMapper.toEntity(PersonaDTO);
        entity.setIdPersona(id); 
        
        PersonaEntity actualizada = personaRepository.save(entity);
        return PersonaMapper.toDTO(actualizada);
    }

    @Override
    @Transactional
    public void deleteById(Integer id) {
        if (!personaRepository.existsById(id)) {
            throw new RuntimeException("No se puede eliminar. Persona no encontrada con el ID: " + id);
        }
        personaRepository.deleteById(id);
    }
}