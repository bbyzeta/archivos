package csg.gob.mx.convocatoria.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "convocatoria_jurado")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ConvocatoriaJuradoEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "id_convocatoria", nullable = false)
    private Integer idConvocatoria;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_jurado", nullable = false)
    private PerfilJuradoEntity jurado;

    @Column(name = "estado_invitacion", length = 30)
    private String estadoInvitacion = "ASIGNADO";

    @Column(name = "fecha_asignacion", updatable = false)
    private LocalDateTime fechaAsignacion = LocalDateTime.now();

    public ConvocatoriaJuradoEntity(Integer idConvocatoria, PerfilJuradoEntity jurado) {
        this.idConvocatoria = idConvocatoria;
        this.jurado = jurado;
        this.estadoInvitacion = "ASIGNADO";
        this.fechaAsignacion = LocalDateTime.now();
    }
}