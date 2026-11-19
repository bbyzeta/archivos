package csg.gob.mx.convocatoria.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "postulacion_anexo")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PostulacionAnexoEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_postulacion", nullable = false)
    @JsonIgnore
    private PostulacionExpedienteEntity postulacion;

    @Column(name = "id_convocatoria", nullable = false)
    private Long idConvocatoria;

    @Column(name = "nombre_anexo", nullable = false)
    private String nombreAnexo;

    @Column(name = "nombre_original", nullable = false)
    private String nombreOriginal;

    @Column(name = "ruta_archivo", nullable = false)
    private String rutaArchivo;

    @Column(name = "fecha_carga", nullable = false)
    private LocalDateTime fechaCarga;
    
    @Column(name = "estado_revision", length = 20)
    @Builder.Default
    private String estadoRevision = "PENDIENTE";

    @Column(name = "observacion_auditor", columnDefinition = "TEXT")
    private String observacionAuditor;
    
    private Integer corregido;
}