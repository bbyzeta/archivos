package csg.gob.mx.convocatoria.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Entity
@Table(name = "datos_institucionales")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DatosInstitucionalesEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_institucional")
    private Long idInstitucional;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_participante", nullable = false)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private ParticipanteEntity participante;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "id_estado", nullable = false)
    private EstadoEntity estado;

    @Column(name = "institucion", nullable = false, length = 255)
    private String institucion;

    @Column(name = "cargo", nullable = false, length = 150)
    private String cargo;
}