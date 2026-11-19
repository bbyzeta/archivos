package csg.gob.mx.convocatoria.service;



import java.util.List;

import org.springframework.stereotype.Service;

import csg.gob.mx.convocatoria.dto.CriterioEvaluacionDTO;
import csg.gob.mx.convocatoria.dto.CriterioEvaluacionProjection;
import csg.gob.mx.convocatoria.dto.CriterioEvaluacionRequestDTO;
import csg.gob.mx.convocatoria.entity.CriterioEvaluacionEntity;
import csg.gob.mx.convocatoria.mapper.CriterioEvaluacionMapper;
import csg.gob.mx.convocatoria.repository.CriterioEvaluacionRepository;
import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class CriterioEvaluacionService {
	private final CriterioEvaluacionRepository criterioRepository;
    private final CriterioEvaluacionMapper mapperCriterio;

    @Transactional
    public List<CriterioEvaluacionDTO> obtenerTodos() {
        return mapperCriterio.toDTOList(criterioRepository.findAll());
    }

    @Transactional
    public CriterioEvaluacionDTO obtenerPorId(Integer idCriterio) {
        CriterioEvaluacionEntity entity = criterioRepository.findById(idCriterio)
                .orElseThrow(() -> new EntityNotFoundException("Criterio no encontrado con ID: " + idCriterio));
        return mapperCriterio.toDTO(entity);
    }

    @Transactional
    public List<CriterioEvaluacionDTO> obtenerPorIdConvocatoria(Long idConvocatoria, Long idUsuario) {
        List<CriterioEvaluacionProjection> lista = criterioRepository.findCriteriosConEvaluacion(idConvocatoria, idUsuario);
        return mapperCriterio.projectionListToDTOList(lista);
    }

    @Transactional
    public CriterioEvaluacionDTO crear(CriterioEvaluacionRequestDTO dto) {
        CriterioEvaluacionEntity entity = mapperCriterio.toEntity(dto);
        CriterioEvaluacionEntity guardado = criterioRepository.save(entity);
        return mapperCriterio.toDTO(guardado);
    }

    @Transactional
    public CriterioEvaluacionDTO actualizar(Integer idCriterio, CriterioEvaluacionRequestDTO dto) {
        CriterioEvaluacionEntity entity = criterioRepository.findById(idCriterio)
                .orElseThrow(() -> new EntityNotFoundException("Criterio no encontrado con ID: " + idCriterio));

        entity.setIdConvocatoria(dto.getIdConvocatoria());
        entity.setNombreCriterio(dto.getNombreCriterio());
        entity.setFactoresConsiderar(dto.getFactoresConsiderar());
        entity.setPorcentajeMaximo(dto.getPorcentajeMaximo());
        entity.setPeriodo(dto.getPeriodo());
        if (dto.getActiva() != null) {
            entity.setActiva(dto.getActiva());
        }

        CriterioEvaluacionEntity actualizado = criterioRepository.save(entity);
        return mapperCriterio.toDTO(actualizado);
    }

    @Transactional
    public void eliminarLogico(Integer idCriterio) {
        CriterioEvaluacionEntity entity = criterioRepository.findById(idCriterio)
                .orElseThrow(() -> new EntityNotFoundException("Criterio no encontrado con ID: " + idCriterio));
        entity.setActiva(false);
        criterioRepository.save(entity);
    }

    @Transactional
    public void eliminarFisico(Integer idCriterio) {
        if (!criterioRepository.existsById(idCriterio)) {
            throw new EntityNotFoundException("Criterio no encontrado con ID: " + idCriterio);
        }
        criterioRepository.deleteById(idCriterio);
    }
}