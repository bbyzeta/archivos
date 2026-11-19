package csg.gob.mx.convocatoria.entity;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "convocatorias")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class ConvocatoriaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_convocatoria")
    private Long id;

    @Column(name="id_convocatoria_general")
    private Integer idConvocatoriaGeneral;
    
    @Column(nullable = false)
    private String titulo;

    private String subtitulo;

    @Column(name = "titulo_blog", nullable = false)
    private String tituloBlog;

    @Column(name = "fecha_apertura", nullable = false)
    private LocalDateTime fechaApertura;

    @Column(name = "fecha_cierre", nullable = false)
    private LocalDateTime fechaCierre;

    @Column(name = "contenido_blog", columnDefinition = "TEXT")
    private String contenidoBlog;

    @Column(name = "ruta_imagen_header")
    private String rutaImagenHeader;

    @Column(name = "ruta_pdf_bases", nullable = false)
    private String rutaPdfBases;

    @Column(name = "periodo")
    private Integer periodo;

    @Column(nullable = false)
    private Boolean activa;

    @OneToMany(mappedBy = "convocatoria", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<RequisitoEntity> requisitos = new ArrayList<>();

    @OneToMany(mappedBy = "convocatoria", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<GanadorHistoricoEntity> ganadoresHistoricos = new ArrayList<>();

    public void addRequisito(RequisitoEntity req) {
        requisitos.add(req);
        req.setConvocatoria(this);
    }

    public void addGanador(GanadorHistoricoEntity ganador) {
        ganadoresHistoricos.add(ganador);
        ganador.setConvocatoria(this);
    }
}