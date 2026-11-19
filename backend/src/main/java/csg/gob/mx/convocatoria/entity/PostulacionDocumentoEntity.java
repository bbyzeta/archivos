package csg.gob.mx.convocatoria.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "postulacion_documento")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PostulacionDocumentoEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_postulacion", nullable = false)
    @JsonIgnore
    private PostulacionExpedienteEntity postulacion;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "id_requisito", nullable = false)
    private RequisitoEntity requisito;

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