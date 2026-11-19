package csg.gob.mx.convocatoria.service;


import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import csg.gob.mx.convocatoria.dto.CriterioEvaluadoDTO;
import csg.gob.mx.convocatoria.dto.DictamenReporteDTO;
import csg.gob.mx.convocatoria.repository.DictamenRepository;

@Service
public class DictamenService {

    @Autowired
    private DictamenRepository dictamenRepository;

    public List<DictamenReporteDTO> obtenerDictamenesConFiltros(Long idConvocatoria, Long idJuez, String termino) {
        List<Object[]> rows = dictamenRepository.obtenerCabeceraDictamenesRaw(idConvocatoria, idJuez, termino);
        List<DictamenReporteDTO> resultado = new ArrayList<>();

        for (Object[] row : rows) {
            Long idConv = ((Number) row[0]).longValue();
            String tituloConv = (String) row[1];
            Integer periodo = row[2] != null ? ((Number) row[2]).intValue() : null;
            Long idJ = row[3] != null ? ((Number) row[3]).longValue() : 0L;
            String nombreJuez = row[4] != null ? (String) row[4] : "Sin Juez Asignado";
            Long idPost = ((Number) row[5]).longValue();
            String nombrePost = (String) row[6];
            String curp = (String) row[7];
            Integer pctTotal = row[8] != null ? ((Number) row[8]).intValue() : 0;
            Integer estado = row[9] != null ? ((Number) row[9]).intValue() : 0;
            String argumento = row[10] != null ? (String) row[10] : "";

            DictamenReporteDTO dto = new DictamenReporteDTO(
                idConv, tituloConv, periodo, idJ, nombreJuez, idPost, nombrePost, curp, pctTotal, estado, argumento
            );

            List<CriterioEvaluadoDTO> criterios = dictamenRepository.obtenerCriteriosPorPostulante(idPost, idConv);
            dto.setCriterios(criterios);

            resultado.add(dto);
        }

        return resultado;
    }
}