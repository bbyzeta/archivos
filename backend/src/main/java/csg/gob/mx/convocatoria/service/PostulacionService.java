package csg.gob.mx.convocatoria.service;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.Year;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import csg.gob.mx.convocatoria.dto.ArchivosPendientesDTO;
import csg.gob.mx.convocatoria.dto.NotificacionEmailDTO;
import csg.gob.mx.convocatoria.dto.PostulacionRegistroDTO;
import csg.gob.mx.convocatoria.dto.PostulacionResponseDTO;
import csg.gob.mx.convocatoria.entity.ParticipanteEntity;
import csg.gob.mx.convocatoria.entity.PostulacionAnexoEntity;
import csg.gob.mx.convocatoria.entity.PostulacionDocumentoEntity;
import csg.gob.mx.convocatoria.entity.PostulacionExpedienteEntity;
import csg.gob.mx.convocatoria.entity.RequisitoEntity;
import csg.gob.mx.convocatoria.enums.TipoNotificacionEnum;
import csg.gob.mx.convocatoria.repository.ParticipanteRepository;
import csg.gob.mx.convocatoria.repository.PostulacionAnexoRepository;
import csg.gob.mx.convocatoria.repository.PostulacionDocumentoRepository;
import csg.gob.mx.convocatoria.repository.PostulacionExpedienteRepository;
import jakarta.mail.MessagingException;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class PostulacionService {

    private final PostulacionExpedienteRepository postulacionRepository;
    private final PostulacionDocumentoRepository documentoRepository;
    private final FileStorageService fileStorageService;
    private final EntityManager entityManager;
    private final PostulacionAnexoRepository anexoRepository;
    private final EmailService emailService;
    private final ParticipanteRepository participanteRepository;
    
    @Value("${app.envia.correo}")
    private String destino;

    @Transactional
    public PostulacionResponseDTO obtenerOCrearBorrador(PostulacionRegistroDTO dto) {
        PostulacionExpedienteEntity entity = postulacionRepository
                .findByIdConvocatoriaAndIdUsuario(dto.getIdConvocatoria(), dto.getIdUsuario())
                .orElseGet(() -> {
                    String folioGenerado = "EXP-" + Year.now().getValue() + "-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
                    PostulacionExpedienteEntity nuevo = PostulacionExpedienteEntity.builder()
                            .idConvocatoria(dto.getIdConvocatoria())
                            .idUsuario(dto.getIdUsuario())
                            .folio(folioGenerado)
                            .tituloProyecto(dto.getTituloProyecto() != null ? dto.getTituloProyecto() : "Borrador de Postulación")
                            .resumenEjecutivo(dto.getResumenEjecutivo())
                            .fechaCreacion(LocalDateTime.now())
                            .estado("BORRADOR")
                            .build();
                    return postulacionRepository.save(nuevo);
                });

        return mapearADTO(entity);
    }

    @Transactional(readOnly = true)
    public PostulacionResponseDTO obtenerPorId(Long id) {
        PostulacionExpedienteEntity entity = postulacionRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Expediente no encontrado con ID: " + id));
        return mapearADTO(entity);
    }

    @Transactional
    public PostulacionResponseDTO guardarDocumentoRequisito(Long idPostulacion, Long idRequisito, MultipartFile archivo) throws IOException {
        PostulacionExpedienteEntity expediente = postulacionRepository.findById(idPostulacion)
                .orElseThrow(() -> new EntityNotFoundException("Expediente no encontrado con ID: " + idPostulacion));

        if ("ENVIADO".equalsIgnoreCase(expediente.getEstado()) && !"CON_OBSERVACIONES".equalsIgnoreCase(expediente.getEstado())) {
        }

        RequisitoEntity requisito = entityManager.find(RequisitoEntity.class, idRequisito);
        if (requisito == null) {
            throw new EntityNotFoundException("El requisito especificado no existe con ID: " + idRequisito);
        }

        String nombreGuardado = fileStorageService.guardarArchivo(archivo, obtenerExtension(archivo));

        PostulacionDocumentoEntity doc = documentoRepository
                .findByPostulacionIdAndRequisitoId(idPostulacion, idRequisito)
                .orElseGet(() -> {
                    PostulacionDocumentoEntity nuevoDoc = new PostulacionDocumentoEntity();
                    nuevoDoc.setPostulacion(expediente);
                    nuevoDoc.setRequisito(requisito);
                    return nuevoDoc;
                });

        doc.setNombreOriginal(archivo.getOriginalFilename());
        doc.setRutaArchivo(nombreGuardado);
        doc.setFechaCarga(LocalDateTime.now());
        
        if(Objects.isNull(doc.getCorregido())) {
        	doc.setCorregido(0);
        }else if(doc.getCorregido() == 0) {
        	doc.setCorregido(1);
       
        	var docReq = documentoRepository.findById(doc.getId()).get();
			enviaCorreo(expediente.getId(), docReq, null);
        }
        
        doc.setEstadoRevision("PENDIENTE");
        documentoRepository.save(doc);

        return mapearADTO(expediente);
    }
    
    private void enviaCorreo(Long idExpediente, PostulacionDocumentoEntity doc, PostulacionAnexoEntity anx) {
        
    	PostulacionExpedienteEntity exp = postulacionRepository.findById(idExpediente)
                .orElseThrow(() -> new EntityNotFoundException("No se encontró el expediente con ID: " + idExpediente));
    	
    	ParticipanteEntity part = exp.getIdUsuario() != null 
                 ? participanteRepository.findByUsuario_IdUsuario(exp.getIdUsuario()).orElse(null)
                 : null;
    	
        NotificacionEmailDTO contenido = new NotificacionEmailDTO();
        contenido.setAsunto("Corrección de Documento - Sistema Premios");
        
        contenido.setDestinatario(part.getUsuario().getUsername()+","+destino);
        contenido.setTipoNotificacion(TipoNotificacionEnum.DOCUMENTO_CORREGIDO);
        
        Map<String, Object> variables = new HashMap<>();
        variables.put("nombreCompleto", part.getNombres()+ " "+ part.getApellidos());
	    variables.put("nombreArchivoRequerido", Objects.nonNull(doc) ? doc.getRequisito().getNombre() : "Documento probatorio descrito en el CV");
	    variables.put("nombreArchivoSubido", Objects.nonNull(doc) ? doc.getNombreOriginal() : anx.getNombreOriginal());
	    variables.put("observacion", Objects.nonNull(doc) ? doc.getObservacionAuditor() : anx.getObservacionAuditor());
        contenido.setVariables(variables);
        
        try {
			emailService.enviarCorreoPlantilla(contenido);
		 } catch (MessagingException e) {		
			e.printStackTrace();
		 }
   }
    
    public String obtenerExtension(MultipartFile archivo) {
        if (archivo == null || archivo.getOriginalFilename() == null) {
            return "";
        }

        String nombreOriginal = archivo.getOriginalFilename();
        int ultimoPunto = nombreOriginal.lastIndexOf(".");

        if (ultimoPunto != -1 && ultimoPunto < nombreOriginal.length() - 1) {
            return nombreOriginal.substring(ultimoPunto + 1).toLowerCase();
        }

        return "";
    }

    @Transactional
    public PostulacionResponseDTO finalizarYEnviar(Long idPostulacion) {
        PostulacionExpedienteEntity expediente = postulacionRepository.findById(idPostulacion)
                .orElseThrow(() -> new EntityNotFoundException("Expediente no encontrado con ID: " + idPostulacion));

        expediente.setEstado("ENVIADO");
        expediente.setFechaEnvio(LocalDateTime.now());
        PostulacionResponseDTO resultado = mapearADTO(postulacionRepository.save(expediente));
        
        enviaCorreo(expediente);
        
        return resultado;
    }
    
    private void enviaCorreo(PostulacionExpedienteEntity expediente) {
    	 ParticipanteEntity participante = participanteRepository.findByUsuario_IdUsuario(expediente.getIdUsuario())
         	    .orElseThrow(() -> new EntityNotFoundException("Participante no encontrado"));
         
         NotificacionEmailDTO contenido = new NotificacionEmailDTO();
         contenido.setAsunto("Expediente - " + expediente.getFolio());
         contenido.setDestinatario(participante.getUsuario().getUsername()+","+destino);
         contenido.setTipoNotificacion(TipoNotificacionEnum.CONFIRMACION_POSTULACION);
         
         Map<String, Object> variables = new HashMap<>();
         variables.put("nombreUsuario", participante.getNombres()+" "+participante.getApellidos());
 	     variables.put("folio", expediente.getFolio());
         contenido.setVariables(variables);
         
         try {
 			emailService.enviarCorreoPlantilla(contenido);
 		 } catch (MessagingException e) {		
 			e.printStackTrace();
 		 }
    }

    private PostulacionResponseDTO mapearADTO(PostulacionExpedienteEntity e) {
        List<PostulacionResponseDTO.DocumentoAdjuntoDTO> docsDto = new ArrayList<>();

        if (e.getDocumentos() != null) {
            for (PostulacionDocumentoEntity d : e.getDocumentos()) {
                PostulacionResponseDTO.DocumentoAdjuntoDTO item = new PostulacionResponseDTO.DocumentoAdjuntoDTO();
                item.setId(d.getId());
                item.setIdRequisito(d.getRequisito() != null ? d.getRequisito().getIdRequisito() : null);
                item.setNombreRequisito(d.getRequisito() != null ? d.getRequisito().getNombre() : null);
                item.setEsAnexo(d.getRequisito() != null && Boolean.TRUE.equals(d.getRequisito().getEsAnexo()));
                item.setNombreOriginal(d.getNombreOriginal());
                item.setRutaArchivo(d.getRutaArchivo());
                item.setFechaCarga(d.getFechaCarga());
                item.setEstadoRevision(d.getEstadoRevision());
                item.setObservacionAuditor(d.getObservacionAuditor());
                docsDto.add(item);
            }
        }
        
        List<PostulacionResponseDTO.AnexoAdjuntoDTO> anexosDto = new ArrayList<>();
        if (e.getAnexos() != null) {
            for (PostulacionAnexoEntity a : e.getAnexos()) {
                anexosDto.add(PostulacionResponseDTO.AnexoAdjuntoDTO.builder()
                        .id(a.getId())
                        .idConvocatoria(a.getIdConvocatoria())
                        .nombreAnexo(a.getNombreAnexo())
                        .nombreOriginal(a.getNombreOriginal())
                        .rutaArchivo(a.getRutaArchivo())
                        .fechaCarga(a.getFechaCarga())
                        .estadoRevision(a.getEstadoRevision())
                        .observacionAuditor(a.getObservacionAuditor())
                        .build());
            }
        }

        return PostulacionResponseDTO.builder()
                .id(e.getId())
                .idConvocatoria(e.getIdConvocatoria())
                .idUsuario(e.getIdUsuario())
                .folio(e.getFolio())
                .estado(e.getEstado())
                .tituloProyecto(e.getTituloProyecto())
                .resumenEjecutivo(e.getResumenEjecutivo())
                .fechaCreacion(e.getFechaCreacion())
                .fechaEnvio(e.getFechaEnvio())
                .documentos(docsDto)
                .anexos(anexosDto)
                .build();
    }
    
    @Transactional
    public PostulacionResponseDTO guardarAnexoLibre(Long idPostulacion, String nombreAnexo, MultipartFile archivo, Long idAnexo) throws IOException {
        PostulacionExpedienteEntity expediente = postulacionRepository.findById(idPostulacion)
                .orElseThrow(() -> new EntityNotFoundException("Expediente no encontrado: " + idPostulacion));

        if (anexoRepository.countByPostulacionId(idPostulacion) >= 10) {
            throw new IllegalStateException("Se ha alcanzado el límite máximo de 10 anexos.");
        }

        String rutaGuardada = fileStorageService.guardarArchivo(archivo, "pdf");
        
        PostulacionAnexoEntity anexoAr = anexoRepository.findById(idAnexo).orElse(null);
                

        if(Objects.nonNull(anexoAr)) {        	
        	anexoAr.setNombreOriginal(archivo.getOriginalFilename());
        	anexoAr.setRutaArchivo(rutaGuardada);
        	anexoAr.setEstadoRevision("PENDIENTE");
        	anexoAr.setCorregido(1);
        	anexoRepository.save(anexoAr);
        	
        	var docAnx = anexoRepository.findById(anexoAr.getId()).get();
			enviaCorreo(expediente.getId(), null, docAnx);
        }else {
        	
        	PostulacionAnexoEntity anexo = PostulacionAnexoEntity.builder()
                    .postulacion(expediente)
                    .idConvocatoria(expediente.getIdConvocatoria())
                    .nombreAnexo(nombreAnexo != null && !nombreAnexo.isBlank() ? nombreAnexo : archivo.getOriginalFilename())
                    .nombreOriginal(archivo.getOriginalFilename())
                    .rutaArchivo(rutaGuardada)
                    .estadoRevision("PENDIENTE")
                    .fechaCarga(LocalDateTime.now())
                    .corregido(0)
                    .build();
        	anexoRepository.save(anexo);
        }
        

        entityManager.flush();
        entityManager.refresh(expediente);

        return mapearADTO(expediente);
    }

    @Transactional
    public PostulacionResponseDTO eliminarAnexoLibre(Long idPostulacion, Long idAnexo) {
        PostulacionAnexoEntity anexo = anexoRepository.findById(idAnexo)
                .orElseThrow(() -> new EntityNotFoundException("Anexo no encontrado: " + idAnexo));

        if (!anexo.getPostulacion().getId().equals(idPostulacion)) {
            throw new IllegalArgumentException("El anexo no corresponde a la postulación especificada.");
        }

        anexoRepository.delete(anexo);
        
        PostulacionExpedienteEntity expediente = postulacionRepository.findById(idPostulacion)
                .orElseThrow(() -> new EntityNotFoundException("Expediente no encontrado"));
        entityManager.flush();
        entityManager.refresh(expediente);

        return mapearADTO(expediente);
    }
    
    @Transactional(readOnly = true)
    public ArchivosPendientesDTO pendientes(Long idPostulacion) {
        if (!postulacionRepository.existsById(idPostulacion)) {
            throw new EntityNotFoundException("Expediente no encontrado: " + idPostulacion);
        }

        boolean tieneRevisiones = documentoRepository.tieneRevisionesRealizadas(idPostulacion);

        ArchivosPendientesDTO apd = new ArchivosPendientesDTO();
        apd.setTieneObservaciones(tieneRevisiones);
        
        return apd;
    }
}