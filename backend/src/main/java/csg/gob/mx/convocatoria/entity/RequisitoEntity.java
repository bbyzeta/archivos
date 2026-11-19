package csg.gob.mx.convocatoria.entity;

import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "requisitos_convocatoria")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
public class RequisitoEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name="id_requisito")
    private Long idRequisito;

    @Column(nullable = false)
    private String nombre;

    @Column(name = "es_anexo", nullable = false)
    private Boolean esAnexo;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_convocatoria")
    private ConvocatoriaEntity convocatoria;
    
    @OneToMany(mappedBy = "requisito", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<PostulacionDocumentoEntity> documentos = new ArrayList<>();
    
    @Column(name="porcentaje_minimo")
    private Integer porcentajeMinimo;
    
    @Column(name="porcentaje_maximo")
    private Integer porcentajeMaximo;
}