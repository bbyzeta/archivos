package csg.gob.mx.convocatoria.controller;


import java.util.HashMap;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import csg.gob.mx.convocatoria.dto.NotificacionEmailDTO;
import csg.gob.mx.convocatoria.service.EmailService;
import jakarta.mail.MessagingException;

@RestController
@RequestMapping("/api/correos")
public class EmailController {

    private final EmailService emailService;

    public EmailController(EmailService emailService) {
        this.emailService = emailService;
    }

    @PostMapping("/enviar-plantilla")
    public ResponseEntity<Map<String, Object>> enviarCorreoConPlantilla(@RequestBody NotificacionEmailDTO dto) {
        Map<String, Object> respuesta = new HashMap<>();

        try {
            emailService.enviarCorreoPlantilla(dto);
            respuesta.put("exito", true);
            respuesta.put("mensaje", "Notificación enviada correctamente a " + dto.getDestinatario());
            return ResponseEntity.ok(respuesta);

        } catch (MessagingException e) {
            respuesta.put("exito", false);
            respuesta.put("mensaje", "Error procesando el correo: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(respuesta);
        }
    }
}
