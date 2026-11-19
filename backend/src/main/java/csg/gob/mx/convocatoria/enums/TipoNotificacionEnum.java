package csg.gob.mx.convocatoria.enums;


public enum TipoNotificacionEnum {
    CONFIRMACION_POSTULACION("mail/confirmacion-postulacion"),
    CON_OBSERVACIONES("mail/con-observaciones"),
    OBSERVACIONES_CORREGIDAS("mail/observaciones-corregidas"),
    SOLICITUD_APROBADA("mail/solicitud-aprobada"),
	SOLICITUD_RECHAZADA("mail/solicitud-rechazada"),
	REGISTRO_POSTULANTE("mail/registro-postulante"),
	DOCUMENTO_CORREGIDO("mail/documento-corregido"),
	ESTADO_EXPEDIENTE("mail/estado-expediente"),
	RECUPERAR_CUENTA("mail/recuperar-correo");

    private final String plantillaPath;

    TipoNotificacionEnum(String plantillaPath) {
        this.plantillaPath = plantillaPath;
    }

    public String getPlantillaPath() {
        return plantillaPath;
    }
}