package csg.gob.mx.convocatoria.repository;

import csg.gob.mx.convocatoria.entity.ConvocatoriaJuradoEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ConvocatoriaJuradoRepository extends JpaRepository<ConvocatoriaJuradoEntity, Integer> {

    @Query("SELECT cj.jurado.id FROM ConvocatoriaJuradoEntity cj WHERE cj.idConvocatoria = :idConvocatoria")
    List<Integer> findJuradoIdsByConvocatoriaId(@Param("idConvocatoria") Integer idConvocatoria);

    @Modifying
    @Query("DELETE FROM ConvocatoriaJuradoEntity cj WHERE cj.idConvocatoria = :idConvocatoria")
    void deleteByIdConvocatoria(@Param("idConvocatoria") Integer idConvocatoria);
    
    List<ConvocatoriaJuradoEntity> findByJurado_Id(Integer idJurado);
}