package csg.gob.mx.convocatoria.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import csg.gob.mx.convocatoria.dto.ArgumentoPostulanteRequestDTO;
import csg.gob.mx.convocatoria.dto.ArgumentoPostulanteResponseDTO;
import csg.gob.mx.convocatoria.entity.ArgumentoPostulanteEntity;
import csg.gob.mx.convocatoria.mapper.ArgumentoPostulanteMapper;
import csg.gob.mx.convocatoria.repository.ArgumentoPostulanteRepository;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ArgumentoPostulanteService{

    private final ArgumentoPostulanteRepository repository;
    private final ArgumentoPostulanteMapper mapper;

    @Transactional
    public ArgumentoPostulanteResponseDTO guardar(ArgumentoPostulanteRequestDTO request) {
        ArgumentoPostulanteEntity entity = mapper.toEntity(request);
        return mapper.toDto(repository.save(entity));
    }

    @Transactional
    public List<ArgumentoPostulanteResponseDTO> guardarMasivo(List<ArgumentoPostulanteRequestDTO> requests) {
        List<ArgumentoPostulanteEntity> entities = requests.stream()
                .map(mapper::toEntity)
                .toList();
        return mapper.toDtoList(repository.saveAll(entities));
    }

    @Transactional(readOnly = true)
    public ArgumentoPostulanteResponseDTO obtenerPorId(Long id) {
    	ArgumentoPostulanteEntity entity = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Argumento no encontrado con ID: " + id));
        return mapper.toDto(entity);
    }

    @Transactional(readOnly = true)
    public List<ArgumentoPostulanteResponseDTO> listarTodos() {
        return mapper.toDtoList(repository.findAll());
    }

    @Transactional(readOnly = true)
    public List<ArgumentoPostulanteResponseDTO> obtenerPorPostulanteYJuez(Long idUsuario, Long idJuez) {
        return mapper.toDtoList(repository.findByIdUsuarioAndIdJuez(idUsuario, idJuez));
    }

    @Transactional
    public ArgumentoPostulanteResponseDTO actualizar(Long id, ArgumentoPostulanteRequestDTO request) {
    	ArgumentoPostulanteEntity existente = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Argumento no encontrado con ID: " + id));

        existente.setIdConvocatoria(request.getIdConvocatoria());
        existente.setIdCriterio(request.getIdCriterio());
        existente.setIdUsuario(request.getIdUsuario());
        existente.setIdJuez(request.getIdJuez());
        existente.setArgumento(request.getArgumento());

        return mapper.toDto(repository.save(existente));
    }

    @Transactional
    public void eliminar(Long id) {
        if (!repository.existsById(id)) {
            throw new RuntimeException("Argumento no encontrado con ID: " + id);
        }
        repository.deleteById(id);
    }
    
    @Transactional(readOnly = true)
    public ArgumentoPostulanteResponseDTO obtenerPorConvocatoriaPostulanteYJuez(
            Long idConvocatoria, Long idUsuario, Long idJuez) {
        
        ArgumentoPostulanteEntity entity = repository
                .findByIdConvocatoriaAndIdUsuarioAndIdJuez(idConvocatoria, idUsuario, idJuez)
                .orElseThrow(() -> new RuntimeException(String.format(
                        "No se encontró argumento para la convocatoria %d, criterio %d, postulante %d y juez %d",
                        idConvocatoria, idUsuario, idJuez)));

        return mapper.toDto(entity);
    }
    
    @Transactional(readOnly = true)
    public List<ArgumentoPostulanteResponseDTO> obtenerPorJuezYConvocatoria(Long idJuez, Long idConvocatoria) {
        List<ArgumentoPostulanteEntity> lista = repository.findByIdJuezAndIdConvocatoria(idJuez, idConvocatoria);
        return mapper.toDtoList(lista);
    }
    
    @Transactional
    public Boolean permiteProponer(ArgumentoPostulanteRequestDTO request) {
    	boolean yaExisteOLimite = repository.existeUsuarioOLimiteAlcanzado(
    		    request.getIdConvocatoria(), 
    		    request.getIdJuez(), 
    		    request.getIdUsuario()
    		) == 1;

        if (yaExisteOLimite) {
            return true;
        }

        return false;
    }
}