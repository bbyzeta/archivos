package csg.gob.mx.convocatoria.service;



import java.util.List;

import org.springframework.stereotype.Service;

import csg.gob.mx.convocatoria.dto.EvaluacionPostulanteDTO;
import csg.gob.mx.convocatoria.entity.EvaluacionPostulanteEntity;
import csg.gob.mx.convocatoria.mapper.EvaluacionPostulanteMapper;
import csg.gob.mx.convocatoria.repository.EvaluacionPostulanteRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class EvaluacionPostulanteService {
	private final EvaluacionPostulanteRepository evaluacionRepository;
    private final EvaluacionPostulanteMapper mapperEvaluacion;

   

    @Transactional
    public List<EvaluacionPostulanteDTO> crear(List<EvaluacionPostulanteDTO> dto) {
    	
    	dto.forEach(eva ->{
    		EvaluacionPostulanteEntity entity = mapperEvaluacion.toEntity(eva);
    		evaluacionRepository.save(entity);    		
    	});
    	
        return dto;
    }

 
}