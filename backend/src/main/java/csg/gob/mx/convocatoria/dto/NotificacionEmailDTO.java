package csg.gob.mx.convocatoria.dto;


import java.util.Map;

import csg.gob.mx.convocatoria.enums.TipoNotificacionEnum;
import lombok.Data;

@Data
public class NotificacionEmailDTO {
    private String destinatario;
    private String asunto;
    private TipoNotificacionEnum tipoNotificacion;
    private Map<String, Object> variables;
}