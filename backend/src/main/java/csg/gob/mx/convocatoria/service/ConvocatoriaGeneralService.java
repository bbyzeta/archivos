package csg.gob.mx.convocatoria.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import csg.gob.mx.convocatoria.dto.ConvocatoriaGeneralResponseDTO;
import csg.gob.mx.convocatoria.entity.ConvocatoriaGeneralEntity;
import csg.gob.mx.convocatoria.repository.ConvocatoriaGeneralRepository;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ConvocatoriaGeneralService {

    private final ConvocatoriaGeneralRepository convocatoriaGeneralRepository;

    @Transactional(readOnly = true)
    public List<ConvocatoriaGeneralResponseDTO> listarConvocatoriasGenerales() {
        return convocatoriaGeneralRepository.findByActivaTrue().stream()
                											   .map(this::mapearAResponse)
                											   .toList();
    }

    public ConvocatoriaGeneralResponseDTO mapearAResponse(ConvocatoriaGeneralEntity entity) {
        return ConvocatoriaGeneralResponseDTO.builder()
                .id(entity.getId())
                .titulo(entity.getTitulo())
                .descripcion(entity.getDescripcion())
                .activa(entity.getActiva())               
                .build();
    }
}