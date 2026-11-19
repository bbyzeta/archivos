package csg.gob.mx.convocatoria.service;



import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import csg.gob.mx.convocatoria.dto.CambiarPasswordDTO;
import csg.gob.mx.convocatoria.dto.NotificacionEmailDTO;
import csg.gob.mx.convocatoria.entity.ParticipanteEntity;
import csg.gob.mx.convocatoria.entity.UsuarioEntity;
import csg.gob.mx.convocatoria.enums.TipoNotificacionEnum;
import csg.gob.mx.convocatoria.repository.ParticipanteRepository;
import csg.gob.mx.convocatoria.repository.UsuarioRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import jakarta.mail.MessagingException;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class RecuperarService {

   private final UsuarioRepository usuarioRepository;
   private final ParticipanteRepository participanteRepository;
   private final EmailService emailService;
   private final PasswordEncoder passwordEncoder;
   
   @Value("${app.url.recupera}")
   private String urlRecupera;
   
   @Transactional
   public void recuperarPassWord(String correo) {
       UsuarioEntity usuario = usuarioRepository.findByUsername(correo)
               .orElseThrow(() -> new RuntimeException("No se encontró el usuario con el correo: " + correo));

       String nuevoToken = UUID.randomUUID().toString();

       usuario.setToken(nuevoToken);
       usuario.setRecupera(1);

       usuarioRepository.save(usuario);

       
      enviarCorreoRecuperacion(usuario.getIdUsuario(), usuario.getUsername(), nuevoToken);
   }
   
   private void enviarCorreoRecuperacion(Long idUsuario, String correo, String token) {
	   
	   ParticipanteEntity part = idUsuario != null 
               ? participanteRepository.findByUsuario_IdUsuario(idUsuario).orElse(null)
               : null;
	   
	   NotificacionEmailDTO contenido = new NotificacionEmailDTO();
       contenido.setAsunto("Restablecimiento de Contraseña - Sistema Premios");
       
       contenido.setDestinatario(part.getUsuario().getUsername());
       contenido.setTipoNotificacion(TipoNotificacionEnum.RECUPERAR_CUENTA);
       
       Map<String, Object> variables = new HashMap<>();
       variables.put("nombreCompleto", part.getNombres()+ " "+ part.getApellidos()); 
       variables.put("username", correo);    
       variables.put("urlPortal", urlRecupera+token);	    
       variables.put("token", token);
       contenido.setVariables(variables);
       
       try {
			emailService.enviarCorreoPlantilla(contenido);
		 } catch (MessagingException e) {		
			e.printStackTrace();
		 }
   }
   
   @Transactional
   public void cambiarPasswordConToken(CambiarPasswordDTO dto) {
       UsuarioEntity usuario = usuarioRepository.findByToken(dto.getToken())
               .orElseThrow(() -> new RuntimeException("El token de recuperación es inválido o no existe"));

       if (usuario.getRecupera() == null || usuario.getRecupera() != 1) {
           throw new RuntimeException("El enlace de recuperación ya ha sido utilizado o ha expirado");
       }

       String passwordEncriptada = passwordEncoder.encode(dto.getNewPassword());
       usuario.setPassword(passwordEncriptada);

       usuario.setToken(null);
       usuario.setRecupera(0);

       usuarioRepository.save(usuario);
   }
    
}