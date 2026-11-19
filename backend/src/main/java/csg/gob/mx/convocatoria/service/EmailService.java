package csg.gob.mx.convocatoria.service;

import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import csg.gob.mx.convocatoria.dto.NotificacionEmailDTO;
import csg.gob.mx.convocatoria.enums.TipoNotificacionEnum;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;

@Service
public class EmailService {

    private final JavaMailSender mailSender;
    private final TemplateEngine templateEngine;

    @Value("${spring.mail.username}")
    private String remitente;

    public EmailService(JavaMailSender mailSender, TemplateEngine templateEngine) {
        this.mailSender = mailSender;
        this.templateEngine = templateEngine;
    }

    @Async
    public void enviarCorreoPlantilla(NotificacionEmailDTO dto) throws MessagingException {
        Context context = new Context();
        if (dto.getVariables() != null) {
            context.setVariables(dto.getVariables());
        }

        String templatePath = obtenerRutaPlantilla(dto.getTipoNotificacion(), dto.getVariables());

        String htmlProcesado = templateEngine.process(templatePath, context);

        MimeMessage mimeMessage = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");

        helper.setFrom(remitente);
        helper.setTo(dto.getDestinatario().split(","));
        helper.setSubject(dto.getAsunto());
        helper.setText(htmlProcesado, true);
        
        helper.addInline("iconoCheck", new ClassPathResource("static/img/correos/circle-check-fill.png"));
        helper.addInline("iconoNotify", new ClassPathResource("static/img/correos/notify.png"));
        helper.addInline("iconoPersonFile", new ClassPathResource("static/img/correos/person-file.png"));
        helper.addInline("iconoClose", new ClassPathResource("static/img/correos/close-circle.png"));

        mailSender.send(mimeMessage);
    }

    private String obtenerRutaPlantilla(TipoNotificacionEnum tipo, Map<String, Object> variables) {
        switch (tipo) {
            case CONFIRMACION_POSTULACION:
                return TipoNotificacionEnum.CONFIRMACION_POSTULACION.getPlantillaPath();

            case CON_OBSERVACIONES:
                return TipoNotificacionEnum.CON_OBSERVACIONES.getPlantillaPath();

            case SOLICITUD_APROBADA:
                return TipoNotificacionEnum.SOLICITUD_APROBADA.getPlantillaPath();

            case REGISTRO_POSTULANTE:
            	return TipoNotificacionEnum.REGISTRO_POSTULANTE.getPlantillaPath();

            case DOCUMENTO_CORREGIDO:
            	return TipoNotificacionEnum.DOCUMENTO_CORREGIDO.getPlantillaPath();

            case ESTADO_EXPEDIENTE:
            	return TipoNotificacionEnum.ESTADO_EXPEDIENTE.getPlantillaPath();

            case RECUPERAR_CUENTA:
            	return TipoNotificacionEnum.RECUPERAR_CUENTA.getPlantillaPath();

            default:
                throw new IllegalArgumentException("Tipo de notificación no soportado: " + tipo);
        }
    }
}