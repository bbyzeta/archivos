package csg.gob.mx.convocatoria.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "postulacion_expediente")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PostulacionExpedienteEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "id_convocatoria", nullable = false)
    private Long idConvocatoria;

    @Column(name = "id_usuario", nullable = false)
    private Long idUsuario;

    @Column(nullable = false, unique = true, length = 40)
    private String folio;

    @Column(nullable = false, length = 20)
    @Builder.Default
    private String estado = "BORRADOR";

    @Column(name = "titulo_proyecto", nullable = false, length = 300)
    private String tituloProyecto;

    @Column(name = "resumen_ejecutivo", columnDefinition = "TEXT")
    private String resumenEjecutivo;

    @Column(name = "fecha_creacion", nullable = false)
    private LocalDateTime fechaCreacion;

    @Column(name = "fecha_envio")
    private LocalDateTime fechaEnvio;

    @Column(nullable = false)
    @Builder.Default
    private Boolean activo = true;

    @OneToMany(mappedBy = "postulacion", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<PostulacionDocumentoEntity> documentos = new ArrayList<>();
    
    @OneToMany(mappedBy = "postulacion", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<PostulacionAnexoEntity> anexos = new ArrayList<>();

    public void addDocumento(PostulacionDocumentoEntity documento) {
        documentos.add(documento);
        documento.setPostulacion(this);
    }
}