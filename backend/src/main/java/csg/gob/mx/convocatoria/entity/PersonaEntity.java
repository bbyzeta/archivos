package csg.gob.mx.convocatoria.entity;


import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "personas")
@Getter
@Setter
public class PersonaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_persona")
    private Integer idPersona;

    @Column(name = "nombre", nullable = false, length = 150)
    private String nombre;

    @Column(name = "cargo", length = 255)
    private String cargo;

    @Column(name = "institucion_dependencia", length = 150)
    private String institucionDependencia;

    public PersonaEntity() {
    }

    public PersonaEntity(Integer idPersona, String nombre, String cargo, String institucionDependencia) {
        this.idPersona = idPersona;
        this.nombre = nombre;
        this.cargo = cargo;
        this.institucionDependencia = institucionDependencia;
    }
}