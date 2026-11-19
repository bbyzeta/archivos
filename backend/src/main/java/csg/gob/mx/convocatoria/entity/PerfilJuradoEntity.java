package csg.gob.mx.convocatoria.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "perfil_jurado")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PerfilJuradoEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Integer id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id", referencedColumnName = "id_usuario", nullable = false, unique = true)
    private UsuarioEntity usuario;

    @Column(name = "grado_academico", nullable = false, length = 20)
    private String gradoAcademico;

    @Column(name = "nombres", nullable = false, length = 100)
    private String nombres;

    @Column(name = "apellidos", nullable = false, length = 100)
    private String apellidos;

    @Column(name = "rfc", length = 13)
    private String rfc;

    @Column(name = "telefono", nullable = false, length = 10)
    private String telefono;

    @Column(name = "institucion_adscripcion", nullable = false, length = 200)
    private String institucionAdscripcion;

    @Column(name = "cargo_actual", nullable = false, length = 150)
    private String cargoActual;

    @Column(name = "area_conocimiento", nullable = false, length = 100)
    private String areaConocimiento;

    @Column(name = "orcid_url", length = 255)
    private String orcidUrl;

    @Column(name = "lineas_investigacion", nullable = false, columnDefinition = "TEXT")
    private String lineasInvestigacion;

    @Column(name = "acepto_conflicto_interes", nullable = false)
    private Boolean aceptoConflictoInteres = false;

    @Column(name = "acepto_confidencialidad", nullable = false)
    private Boolean aceptoConfidencialidad = false;

    @Column(name = "fecha_aceptacion_confidencialidad")
    private LocalDateTime fechaAceptacionConfidencialidad;
}