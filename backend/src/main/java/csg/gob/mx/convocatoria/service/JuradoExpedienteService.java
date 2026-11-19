package csg.gob.mx.convocatoria.service;

import java.time.LocalDate;
import java.time.Year;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import csg.gob.mx.convocatoria.dto.ConvocatoriaJuradoDTO;
import csg.gob.mx.convocatoria.dto.DictamenDocumentoRequestDTO;
import csg.gob.mx.convocatoria.dto.DictamenExpedienteRequestDTO;
import csg.gob.mx.convocatoria.dto.ExpedienteAuditoriaDTO;
import csg.gob.mx.convocatoria.dto.ExpedienteBandejaItemDTO;
import csg.gob.mx.convocatoria.dto.ItemRevisionDocumentoDTO;
import csg.gob.mx.convocatoria.dto.NotificacionEmailDTO;
import csg.gob.mx.convocatoria.entity.ConvocatoriaEntity;
import csg.gob.mx.convocatoria.entity.ConvocatoriaJuradoEntity;
import csg.gob.mx.convocatoria.entity.ParticipanteEntity;
import csg.gob.mx.convocatoria.entity.PerfilJuradoEntity;
import csg.gob.mx.convocatoria.entity.PostulacionAnexoEntity;
import csg.gob.mx.convocatoria.entity.PostulacionDocumentoEntity;
import csg.gob.mx.convocatoria.entity.PostulacionExpedienteEntity;
import csg.gob.mx.convocatoria.entity.RequisitoEntity;
import csg.gob.mx.convocatoria.enums.TipoNotificacionEnum;
import csg.gob.mx.convocatoria.repository.ConvocatoriaJuradoRepository;
import csg.gob.mx.convocatoria.repository.ConvocatoriaRepository;
import csg.gob.mx.convocatoria.repository.ParticipanteRepository;
import csg.gob.mx.convocatoria.repository.PerfilJuradoRepository;
import csg.gob.mx.convocatoria.repository.PostulacionAnexoRepository;
import csg.gob.mx.convocatoria.repository.PostulacionDocumentoRepository;
import csg.gob.mx.convocatoria.repository.PostulacionRepository;
import csg.gob.mx.convocatoria.repository.RequisitoRepository;
import jakarta.mail.MessagingException;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class JuradoExpedienteService {

    private final PostulacionRepository postulacionRepository;
    private final ConvocatoriaRepository convocatoriaRepository;
    private final PostulacionDocumentoRepository documentoRepository;
    private final PostulacionAnexoRepository anexoRepository;
    private final ParticipanteRepository participanteRepository;
    private final RequisitoRepository requisitoRepository;
    private final EmailService emailService;
    private final PerfilJuradoRepository perfilJuradoRepository;
    private final ConvocatoriaJuradoRepository convocatoriaJuradoRepository;

    @Value("${app.url.login}")
    private String urlLogin;

    @Transactional(readOnly = true)
    public Page<ExpedienteBandejaItemDTO> listarExpedientesParaAuditoria(
            int pagina,
            int tamanio,
            String busqueda,
            Long idConvocatoria,
            String estado,
            List<Integer> idsCategorias
    ) {
        Pageable pageable = PageRequest.of(pagina, tamanio, Sort.by(Sort.Direction.DESC, "fechaCreacion"));
        int anioActual = Year.now().getValue();

        Page<PostulacionExpedienteEntity> expedientes = postulacionRepository.buscarExpedientesJuradoAuditor(
                (busqueda != null && !busqueda.isBlank()) ? busqueda.trim().toLowerCase() : null,
                idConvocatoria,
                idsCategorias,
                "Aceptado",
                anioActual,
                pageable
        );

        return expedientes.map(exp -> {
            ConvocatoriaEntity conv = convocatoriaRepository.findById(exp.getIdConvocatoria())
                    .orElse(null);

            Long idUsuarioPostulante = exp.getIdUsuario();
            ParticipanteEntity part = idUsuarioPostulante != null 
                    ? participanteRepository.findByUsuario_IdUsuario(idUsuarioPostulante).orElse(null)
                    : null;

            String nombreCompleto = (part != null)
                    ? (part.getNombres() + " " + part.getApellidos()).trim()
                    : exp.getTituloProyecto();

            String correo = (part != null && part.getUsuario() != null)
                    ? part.getUsuario().getUsername()
                    : "Sin correo";

            String curp = (part != null) ? part.getCurp() : "SIN-CURP";

            String institucion = (part != null && part.getDatosInstitucionales() != null)
                    ? part.getDatosInstitucionales().getInstitucion()
                    : "No registrada";

            int totalDocs = (exp.getDocumentos() != null ? exp.getDocumentos().size() : 0)
                          + (exp.getAnexos() != null ? exp.getAnexos().size() : 0);

            long aprobadosDocs = (exp.getDocumentos() != null)
                    ? exp.getDocumentos().stream().filter(d -> "APROBADO".equalsIgnoreCase(d.getEstadoRevision())).count()
                    : 0;

            long aprobadosAnx = (exp.getAnexos() != null)
                    ? exp.getAnexos().stream().filter(a -> "APROBADO".equalsIgnoreCase(a.getEstadoRevision())).count()
                    : 0;

            return ExpedienteBandejaItemDTO.builder()
                    .idExpediente(exp.getId())
                    .folio(exp.getFolio())
                    .idConvocatoria(exp.getIdConvocatoria())
                    .tituloConvocatoria(conv != null ? conv.getTitulo() : "Sin Convocatoria")
                    .periodo(conv != null ? conv.getPeriodo() : null)
                    .idPostulante(idUsuarioPostulante)
                    .nombrePostulante(nombreCompleto)
                    .correoPostulante(correo)
                    .curpPostulante(curp)
                    .institucionPostulante(institucion)
                    .estadoExpediente(exp.getEstado())
                    .totalDocumentos(totalDocs)
                    .documentosAprobados((int) (aprobadosDocs + aprobadosAnx))
                    .fechaEnvio(exp.getFechaEnvio() != null ? exp.getFechaEnvio() : exp.getFechaCreacion())
                    .build();
        });
    }

    @Transactional(readOnly = true)
    public ExpedienteAuditoriaDTO obtenerExpedienteParaAuditoria(Long idExpediente) {
        PostulacionExpedienteEntity exp = postulacionRepository.findById(idExpediente)
                .orElseThrow(() -> new EntityNotFoundException("No se encontró el expediente con ID: " + idExpediente));

        ConvocatoriaEntity conv = convocatoriaRepository.findById(exp.getIdConvocatoria())
                .orElseThrow(() -> new EntityNotFoundException("No se encontró la convocatoria del expediente: " + exp.getIdConvocatoria()));

        Long idUsuarioPostulante = exp.getIdUsuario();
        ParticipanteEntity part = idUsuarioPostulante != null 
                ? participanteRepository.findByUsuario_IdUsuario(idUsuarioPostulante).orElse(null)
                : null;

        String nombreCompleto = (part != null)
                ? (part.getNombres() + " " + part.getApellidos()).trim()
                : exp.getTituloProyecto();

        String correo = (part != null && part.getUsuario() != null)
                ? part.getUsuario().getUsername()
                : "Sin correo";

        String curp = (part != null) ? part.getCurp() : "SIN-CURP";
        String telefono = (part != null) ? part.getTelefono() : "S/N";
        String institucion = (part != null && part.getDatosInstitucionales() != null)
                ? part.getDatosInstitucionales().getInstitucion()
                : "No registrada";

        List<ItemRevisionDocumentoDTO> listaDocs = new ArrayList<>();

        if (exp.getDocumentos() != null) {
            exp.getDocumentos().forEach(d -> {
                String nombreRequisito = "Requisito Oficial";
                if (d.getRequisito() != null) {
                    nombreRequisito = requisitoRepository.findById(d.getRequisito().getIdRequisito())
                            .map(RequisitoEntity::getNombre)
                            .orElse("Requisito #" + d.getRequisito());
                }

                listaDocs.add(ItemRevisionDocumentoDTO.builder()
                        .idDocumento(d.getId())
                        .nombre(nombreRequisito)
                        .esAnexo(false)
                        .nombreOriginal(d.getNombreOriginal())
                        .rutaArchivo(d.getRutaArchivo())
                        .estadoRevision(d.getEstadoRevision() != null ? d.getEstadoRevision() : "PENDIENTE")
                        .observacionAuditor(d.getObservacionAuditor())
                        .corregido(d.getCorregido())
                        .build());
            });
        }

        if (exp.getAnexos() != null) {
            exp.getAnexos().forEach(a -> {
                listaDocs.add(ItemRevisionDocumentoDTO.builder()
                        .idDocumento(a.getId())
                        .nombre(a.getNombreAnexo() != null ? a.getNombreAnexo() : "Anexo Adicional")
                        .esAnexo(true)
                        .nombreOriginal(a.getNombreOriginal())
                        .rutaArchivo(a.getRutaArchivo())
                        .estadoRevision(a.getEstadoRevision() != null ? a.getEstadoRevision() : "PENDIENTE")
                        .observacionAuditor(a.getObservacionAuditor())
                        .corregido(a.getCorregido())
                        .build());
            });
        }

        return ExpedienteAuditoriaDTO.builder()
                .idExpediente(exp.getId())
                .folio(exp.getFolio())
                .estadoExpediente(exp.getEstado())
                .fechaEnvio(exp.getFechaEnvio() != null ? exp.getFechaEnvio() : exp.getFechaCreacion())
                .idPostulante(idUsuarioPostulante)
                .nombrePostulante(nombreCompleto)
                .correoPostulante(correo)
                .curpPostulante(curp)
                .telefonoPostulante(telefono)
                .institucionPostulante(institucion)
                .idConvocatoria(conv.getId())
                .tituloConvocatoria(conv.getTitulo())
                .periodoConvocatoria(conv.getPeriodo())
                .fechaApertura(conv.getFechaApertura())
                .fechaCierre(conv.getFechaCierre())
                .rutaImagenPremio(conv.getRutaImagenHeader())
                .documentos(listaDocs)
                .build();
    }

    @Transactional
    public void actualizarDictamenDocumento(Long idExpediente, Long idDocumento, DictamenDocumentoRequestDTO dto) {
        var docOpt = documentoRepository.findById(idDocumento);
        if (docOpt.isPresent()) {
            var doc = docOpt.get();
            doc.setEstadoRevision(dto.getEstado().toUpperCase());
            if(dto.getEstado().equals("APROBADO")) {
            	doc.setObservacionAuditor("");
            }else if(!dto.getObservacion().isEmpty()) {
            	doc.setObservacionAuditor(dto.getObservacion());
            	
            	
            }
            
            documentoRepository.save(doc);
            enviaCorreo(idExpediente, doc, null);
            return;
        }

        var anxOpt = anexoRepository.findById(idDocumento);
        if (anxOpt.isPresent()) {
            var anx = anxOpt.get();
            anx.setEstadoRevision(dto.getEstado().toUpperCase());
            if(anx.getEstadoRevision().equals("APROBADO")) {
            	anx.setObservacionAuditor("");
            }else if(!dto.getObservacion().isEmpty()) {
            	anx.setObservacionAuditor(dto.getObservacion());
            }
            anexoRepository.save(anx);
            enviaCorreo(idExpediente, null, anx);
            
            return;
        }

        throw new EntityNotFoundException("No se encontró el documento o anexo con ID: " + idDocumento);
    }
    
    private void enviaCorreo(Long idExpediente, PostulacionDocumentoEntity doc, PostulacionAnexoEntity anx) {
	        
    	PostulacionExpedienteEntity exp = postulacionRepository.findById(idExpediente)
                .orElseThrow(() -> new EntityNotFoundException("No se encontró el expediente con ID: " + idExpediente));
    	
    	ParticipanteEntity part = exp.getIdUsuario() != null 
                 ? participanteRepository.findByUsuario_IdUsuario(exp.getIdUsuario()).orElse(null)
                 : null;
    	
        NotificacionEmailDTO contenido = new NotificacionEmailDTO();
        contenido.setAsunto("Observación de Documento - Sistema Premios");
        
        contenido.setDestinatario(part.getUsuario().getUsername());
        contenido.setTipoNotificacion(TipoNotificacionEnum.CON_OBSERVACIONES);
        
        Map<String, Object> variables = new HashMap<>();
        variables.put("nombreCompleto", part.getNombres()+ " "+ part.getApellidos());
	    variables.put("nombreArchivoRequerido", Objects.nonNull(doc) ? doc.getRequisito().getNombre() : "Documento probatorio descrito en el CV");
	    variables.put("nombreArchivoSubido", Objects.nonNull(doc) ? doc.getNombreOriginal() : anx.getNombreOriginal());
	    variables.put("observacion", Objects.nonNull(doc) ? doc.getObservacionAuditor() : anx.getObservacionAuditor());
	    variables.put("urlPortal", urlLogin);
        contenido.setVariables(variables);
        
        try {
			emailService.enviarCorreoPlantilla(contenido);
		 } catch (MessagingException e) {		
			e.printStackTrace();
		 }
   }

    @Transactional
    public void dictaminarExpedienteAuditor(Long idExpediente, DictamenExpedienteRequestDTO dto) {
        PostulacionExpedienteEntity exp = postulacionRepository.findById(idExpediente)
                .orElseThrow(() -> new EntityNotFoundException("No se encontró el expediente con ID: " + idExpediente));

        exp.setEstado(dto.getResultado().toUpperCase());
        postulacionRepository.save(exp);
        enviarCorreo(exp.getId(), dto.getResultado());
    }
    
    private void enviarCorreo(Long idExpediente, String estado) {
    	PostulacionExpedienteEntity exp = postulacionRepository.findById(idExpediente)
                .orElseThrow(() -> new EntityNotFoundException("No se encontró el expediente con ID: " + idExpediente));
    	
    	ParticipanteEntity part = exp.getIdUsuario() != null 
                 ? participanteRepository.findByUsuario_IdUsuario(exp.getIdUsuario()).orElse(null)
                 : null;
    	
        NotificacionEmailDTO contenido = new NotificacionEmailDTO();
        contenido.setAsunto("Notificación Resultado Postulación - Sistema Premios");
        
        contenido.setDestinatario(part.getUsuario().getUsername());
        contenido.setTipoNotificacion(TipoNotificacionEnum.ESTADO_EXPEDIENTE);
        
        Map<String, Object> variables = new HashMap<>();
        variables.put("nombreCompleto", part.getNombres()+ " "+ part.getApellidos());
        variables.put("estado", estado);
	    
        contenido.setVariables(variables);
        
        try {
			emailService.enviarCorreoPlantilla(contenido);
		 } catch (MessagingException e) {		
			e.printStackTrace();
		 }
    }
    
    public List<ConvocatoriaJuradoDTO> getConvocatoria(Long idUsuario)throws Exception {
    	List<ConvocatoriaJuradoDTO> resultado = new ArrayList<>();

    	PerfilJuradoEntity jurado =  perfilJuradoRepository.findByUsuario_IdUsuario(idUsuario)
    			.orElseThrow(() -> new Exception("El usuario no existe."));
    	
    	if(Objects.nonNull(jurado)) {
    		List<ConvocatoriaJuradoEntity> convocatorias =  convocatoriaJuradoRepository.findByJurado_Id(jurado.getId());    		
    		resultado = convocatorias.stream().map(con -> new ConvocatoriaJuradoDTO(
    				con.getId(),
    				con.getIdConvocatoria(),
    				con.getJurado().getId().intValue(),
    				con.getEstadoInvitacion(),
    				con.getFechaAsignacion()
    				
    				
    		)).toList();
    		
    		resultado = obtenerConvocatoriasAnioActual(resultado, jurado.getId());
    	}
    	
    	return resultado;
    }
    
    public List<ConvocatoriaJuradoDTO> obtenerConvocatoriasAnioActual(List<ConvocatoriaJuradoDTO> listaObjetos, Integer idJurado) {
        
        List<Integer> idsCategorias = listaObjetos.stream()
                .map(ConvocatoriaJuradoDTO::getIdConvocatoria) 
                .filter(Objects::nonNull)
                .distinct() 
                .toList();

        if (idsCategorias.isEmpty()) {
            return List.of();
        }

        Integer periodoActual = LocalDate.now().getYear();

        List<ConvocatoriaEntity> convocatorias = convocatoriaRepository
                .findByIdInAndPeriodo(idsCategorias, periodoActual);

        return convocatorias.stream()
                .map(con -> this.toDto(con, idJurado))
                .toList();
    }

    private ConvocatoriaJuradoDTO toDto(ConvocatoriaEntity entity, Integer idJurado) {
    	ConvocatoriaJuradoDTO dto = new ConvocatoriaJuradoDTO();
        dto.setId(entity.getId().intValue());
        dto.setIdConvocatoria(entity.getId().intValue());
        dto.setIdJurado(idJurado);
       
        return dto;
    }
    
    @Transactional(readOnly = true)
    public Integer obtenerTotalEvaluacion(Integer idUsuario, Integer idConvocatoria) {
        return postulacionRepository.obtenerTotalPorcentajeAsignado(idUsuario, idConvocatoria);
    }

    @Transactional(readOnly = true)
    public Integer esPostulantePropuesto(Integer idConvocatoria, Integer idUsuario, Integer idJuez) {
    	return postulacionRepository.esPostulantePropuesto(idConvocatoria, idUsuario, idJuez );
    }
}