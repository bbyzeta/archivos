package csg.gob.mx.convocatoria.service;



import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import csg.gob.mx.convocatoria.dto.NotificacionEmailDTO;
import csg.gob.mx.convocatoria.dto.RegistroDTO;
import csg.gob.mx.convocatoria.entity.DatosInstitucionalesEntity;
import csg.gob.mx.convocatoria.entity.ParticipanteEntity;
import csg.gob.mx.convocatoria.entity.RolEntity;
import csg.gob.mx.convocatoria.entity.UsuarioEntity;
import csg.gob.mx.convocatoria.enums.TipoNotificacionEnum;
import csg.gob.mx.convocatoria.repository.DatosInstitucionalesRepository;
import csg.gob.mx.convocatoria.repository.ParticipanteRepository;
import csg.gob.mx.convocatoria.repository.RolRepository;
import csg.gob.mx.convocatoria.repository.UsuarioRepository;
import jakarta.mail.MessagingException;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class RegistroService {

    private final UsuarioRepository usuarioRepository;
    private final ParticipanteRepository participanteRepository;
    private final DatosInstitucionalesRepository institucionalesRepository;
    private final RolRepository rolRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;
    
    @Value("${app.url.activacion}")
    private String urlActivacionBase;

    @Transactional
    public void registrarParticipante(RegistroDTO dto) {
        if (usuarioRepository.existsByUsername(dto.getEmail())) {
            throw new IllegalArgumentException("El correo electrónico ya se encuentra registrado.");
        }
        if (participanteRepository.existsByCurp(dto.getCurp())) {
            throw new IllegalArgumentException("La CURP ingresada ya se encuentra registrada.");
        }

        RolEntity rol = rolRepository.findById(4)
                .orElseThrow(() -> new RuntimeException("El rol con ID 4 no existe"));
        String token = UUID.randomUUID().toString();
        
        UsuarioEntity usuario = new UsuarioEntity();
        usuario.setUsername(dto.getEmail());
        usuario.setPassword(passwordEncoder.encode(dto.getPassword()));
        usuario.setActivo(false);
        usuario.getRoles().add(rol);
        usuario.setToken(token);
        UsuarioEntity usuarioGuardado = usuarioRepository.save(usuario);

        ParticipanteEntity participante = new ParticipanteEntity();
        participante.setUsuario(usuarioGuardado);
        participante.setNombres(dto.getNombres());
        participante.setApellidos(dto.getApellidos());
        participante.setCurp(dto.getCurp().toUpperCase());
        participante.setTelefono(dto.getTelefono());
        participante.setAceptoTerminos(dto.isAceptoTerminos());
        ParticipanteEntity participanteGuardado = participanteRepository.save(participante);

        //EstadoEntity estado = estadoRepository.findById(dto.getIdEstado())
        //        .orElseThrow(() -> new IllegalArgumentException("El estado seleccionado no existe."));

        DatosInstitucionalesEntity institucionales = new DatosInstitucionalesEntity();
        institucionales.setParticipante(participanteGuardado);
        //institucionales.setEstado(estado);
        //institucionales.setInstitucion(dto.getInstitucion());
        institucionales.setCargo(dto.getTitulo());
        institucionalesRepository.save(institucionales);
        
        enviaCorreo(usuarioGuardado, participanteGuardado, institucionales, token);
    }
    
    private void enviaCorreo(UsuarioEntity usuario, ParticipanteEntity participante, DatosInstitucionalesEntity datos, String token) {
   	        
        NotificacionEmailDTO contenido = new NotificacionEmailDTO();
        contenido.setAsunto("Registro de Usuario - Sistema Premios");
        contenido.setDestinatario(usuario.getUsername());
        contenido.setTipoNotificacion(TipoNotificacionEnum.REGISTRO_POSTULANTE);
        
        Map<String, Object> variables = new HashMap<>();
        variables.put("nombreCompleto", participante.getNombres()+ " "+ participante.getApellidos());
	    variables.put("nombre", participante.getNombres());
	    variables.put("apellidos", participante.getApellidos());
	    variables.put("curp", participante.getCurp());
	    variables.put("telefono", participante.getTelefono());
	    variables.put("email", usuario.getUsername());
	    variables.put("titulo", datos.getCargo());	    
	    variables.put("urlActivacion", urlActivacionBase+token);
        contenido.setVariables(variables);
        
        try {
			emailService.enviarCorreoPlantilla(contenido);
		 } catch (MessagingException e) {		
			e.printStackTrace();
		 }
   }
    
    public Integer activarParticipante(String token)throws Exception {
    	UsuarioEntity usuario = usuarioRepository.findByToken(token).orElse(null);
    	Integer resultado = 0;
    	
    	if(!Objects.isNull(usuario) ) {
    		if(usuario.isActivo()) {
    			resultado = 1;    		    			
    		}else {
    			usuario.setActivo(true);
        		usuarioRepository.save(usuario);
        		resultado = 2;    		
    		}
    	}
    	
    	return resultado;
    }
    
}