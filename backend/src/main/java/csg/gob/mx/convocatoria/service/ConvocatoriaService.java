package csg.gob.mx.convocatoria.service;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Year;
import java.util.ArrayList;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import csg.gob.mx.convocatoria.dto.ConvocatoriaResponseDTO;
import csg.gob.mx.convocatoria.dto.GanadorDTO;
import csg.gob.mx.convocatoria.dto.RequisitoDTO;
import csg.gob.mx.convocatoria.entity.ConvocatoriaEntity;
import csg.gob.mx.convocatoria.entity.GanadorHistoricoEntity;
import csg.gob.mx.convocatoria.entity.RequisitoEntity;
import csg.gob.mx.convocatoria.repository.ConvocatoriaRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ConvocatoriaService {

    private final ConvocatoriaRepository convocatoriaRepository;
    private final FileStorageService fileStorageService;
    private final ObjectMapper objectMapper;

    @Transactional(readOnly = true)
    public Page<ConvocatoriaResponseDTO> listarConvocatorias(int pagina, int tamanio, String busqueda) {
        Pageable pageable = PageRequest.of(pagina, tamanio);
        String termino = (busqueda != null && !busqueda.trim().isEmpty()) ? busqueda.trim() : null;
        return convocatoriaRepository.buscarPaginado(termino, pageable).map(this::mapearAResponse);
    }

    @Transactional(readOnly = true)
    public ConvocatoriaResponseDTO obtenerPorId(Long id) {
        ConvocatoriaEntity entity = convocatoriaRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Convocatoria no encontrada con ID: " + id));
        return mapearAResponse(entity);
    }
    
    @Transactional(readOnly = true)
    public ConvocatoriaResponseDTO obtenerConvocatoriaActiva() {
        ConvocatoriaEntity entity = convocatoriaRepository.findByActivaTrue()
                .orElseThrow(() -> new EntityNotFoundException("No se encontró una convocatoria activa en este momento."));
        
        return mapearAResponse(entity);
    }

    @Transactional
    public ConvocatoriaEntity crearConvocatoria(
            String titulo,
            String subtitulo,
            String tituloBlog,
            String fechaAperturaStr,
            String fechaCierreStr,
            String contenidoBlog,
            Boolean activa,
            MultipartFile imagen,
            MultipartFile documentoBases,
            String requisitosJson,
            String ganadoresHistoricosJson,
            Integer idGrupoPremio
    ) throws IOException {

      /*if (Boolean.TRUE.equals(activa)) {
            convocatoriaRepository.desactivarTodasLasConvocatorias();
        }
      */
    	
        String nombreImagen = (imagen != null && !imagen.isEmpty()) ? fileStorageService.guardarArchivo(imagen, "img") : null;
        String nombreBases = (documentoBases != null && !documentoBases.isEmpty()) ? fileStorageService.guardarArchivo(documentoBases, "pdf") : null;
        int anioActual = LocalDate.now().getYear();

        ConvocatoriaEntity convocatoria = ConvocatoriaEntity.builder()
                .titulo(titulo)
                .subtitulo(subtitulo)
                .tituloBlog(tituloBlog)
                .fechaApertura(LocalDateTime.parse(fechaAperturaStr))
                .fechaCierre(LocalDateTime.parse(fechaCierreStr))
                .contenidoBlog(contenidoBlog)
                .rutaImagenHeader(nombreImagen)
                .rutaPdfBases(nombreBases)
                .periodo(anioActual)
                .activa(activa != null ? activa : false)
                .idConvocatoriaGeneral(idGrupoPremio)
                .build();

        poblarColeccionesHijas(convocatoria, requisitosJson, ganadoresHistoricosJson);

        return convocatoriaRepository.save(convocatoria);
    }

    @Transactional
    public ConvocatoriaEntity actualizarConvocatoria(
            Long id,
            String titulo,
            String subtitulo,
            String tituloBlog,
            String fechaAperturaStr,
            String fechaCierreStr,
            String contenidoBlog,
            Boolean activa,
            MultipartFile imagen,
            MultipartFile documentoBases,
            String requisitosJson,
            String ganadoresHistoricosJson,
            Integer idGrupoPremio
    ) throws IOException {

    	/*if (Boolean.TRUE.equals(activa)) {
    		convocatoriaRepository.desactivarTodasLasConvocatorias();
    	}*/

    	ConvocatoriaEntity convocatoria = convocatoriaRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Convocatoria no encontrada con ID: " + id));


        convocatoria.setTitulo(titulo);
        convocatoria.setSubtitulo(subtitulo);
        convocatoria.setTituloBlog(tituloBlog);
        convocatoria.setFechaApertura(LocalDateTime.parse(fechaAperturaStr));
        convocatoria.setFechaCierre(LocalDateTime.parse(fechaCierreStr));
        convocatoria.setContenidoBlog(contenidoBlog);
        convocatoria.setActiva(activa != null ? activa : false);
        convocatoria.setIdConvocatoriaGeneral(idGrupoPremio);

        if (imagen != null && !imagen.isEmpty()) {
            convocatoria.setRutaImagenHeader(fileStorageService.guardarArchivo(imagen, "img"));
        }

        if (documentoBases != null && !documentoBases.isEmpty()) {
            convocatoria.setRutaPdfBases(fileStorageService.guardarArchivo(documentoBases, "pdf"));
        }

        convocatoria.getRequisitos().clear();
        convocatoria.getGanadoresHistoricos().clear();

        poblarColeccionesHijas(convocatoria, requisitosJson, ganadoresHistoricosJson);

        return convocatoriaRepository.save(convocatoria);
    }

    @Transactional
    public void alternarEstadoActiva(Long id) {
        ConvocatoriaEntity entity = convocatoriaRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Convocatoria no encontrada con ID: " + id));

        boolean nuevoEstado = !Boolean.TRUE.equals(entity.getActiva());

        if (nuevoEstado) {
            convocatoriaRepository.desactivarTodasLasConvocatorias();
        }

        entity.setActiva(nuevoEstado);
        convocatoriaRepository.save(entity);
    }

    @Transactional
    public void eliminarConvocatoria(Long id) {
        if (!convocatoriaRepository.existsById(id)) {
            throw new EntityNotFoundException("Convocatoria no encontrada con ID: " + id);
        }
        convocatoriaRepository.deleteById(id);
    }

    private void poblarColeccionesHijas(ConvocatoriaEntity convocatoria, String requisitosJson, String ganadoresHistoricosJson) throws IOException {
        if (requisitosJson != null && !requisitosJson.isBlank()) {
            List<RequisitoDTO> reqDTOs = objectMapper.readValue(requisitosJson, new TypeReference<List<RequisitoDTO>>() {});
            for (RequisitoDTO dto : reqDTOs) {
                RequisitoEntity req = new RequisitoEntity();
                req.setNombre(dto.getNombre());
                req.setEsAnexo(dto.getEsAnexo() != null ? dto.getEsAnexo() : false);
                req.setPorcentajeMinimo(dto.getPorcentajeMinimo());
                req.setPorcentajeMaximo(dto.getPorcentajeMaximo());
                convocatoria.addRequisito(req);
            }
        }

        if (ganadoresHistoricosJson != null && !ganadoresHistoricosJson.isBlank()) {
            List<GanadorDTO> ganDTOs = objectMapper.readValue(ganadoresHistoricosJson, new TypeReference<List<GanadorDTO>>() {});
            for (GanadorDTO dto : ganDTOs) {
                GanadorHistoricoEntity gan = new GanadorHistoricoEntity();
                gan.setNombreGanador(dto.getNombreGanador());
                gan.setAnio(dto.getAnio());
                convocatoria.addGanador(gan);
            }
        }
    }

    public ConvocatoriaResponseDTO mapearAResponse(ConvocatoriaEntity entity) {
        return ConvocatoriaResponseDTO.builder()
                .id(entity.getId())
                .titulo(entity.getTitulo())
                .subtitulo(entity.getSubtitulo())
                .tituloBlog(entity.getTituloBlog())
                .fechaApertura(entity.getFechaApertura())
                .fechaCierre(entity.getFechaCierre())
                .contenidoBlog(entity.getContenidoBlog())
                .rutaImagenHeader(entity.getRutaImagenHeader())
                .rutaPdfBases(entity.getRutaPdfBases())
                .periodo(entity.getPeriodo())
                .activa(entity.getActiva())
                .idGrupoPremio(entity.getIdConvocatoriaGeneral())
                .requisitos(entity.getRequisitos() != null ? entity.getRequisitos().stream().map(r -> {
                    RequisitoDTO dto = new RequisitoDTO();
                    dto.setIdRequisito(r.getIdRequisito());
                    dto.setNombre(r.getNombre());
                    dto.setEsAnexo(r.getEsAnexo());
                    dto.setPorcentajeMinimo(r.getPorcentajeMinimo());
                    dto.setPorcentajeMaximo(r.getPorcentajeMaximo());
                    return dto;
                }).toList() : new ArrayList<>())
                .ganadoresHistoricos(entity.getGanadoresHistoricos() != null ? entity.getGanadoresHistoricos().stream().map(g -> {
                    GanadorDTO dto = new GanadorDTO();
                    dto.setNombreGanador(g.getNombreGanador());
                    dto.setAnio(g.getAnio());
                    return dto;
                }).toList() : new ArrayList<>())
                .build();
    }
    
    @Transactional(readOnly = true)
    public List<ConvocatoriaResponseDTO> obtenerPorIdCategoria(Integer idCategoria) {
    	int anioActual = Year.now().getValue();
        List<ConvocatoriaEntity> convocatorias = convocatoriaRepository.findByIdConvocatoriaGeneralAndActivaAndPeriodo(idCategoria,true, anioActual);           
        
        return convocatorias.stream()
                .map(this::mapearAResponseDos)
                .toList();
    }
    
    private ConvocatoriaResponseDTO mapearAResponseDos(ConvocatoriaEntity entity) {
        return ConvocatoriaResponseDTO.builder()
                .id(entity.getId())                
                .titulo(entity.getTitulo())
                .subtitulo(entity.getSubtitulo())
                .rutaImagenHeader(entity.getRutaImagenHeader())
                .activa(entity.getActiva())
                .build();
    }
}