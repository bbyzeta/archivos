package csg.gob.mx.convocatoria.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import csg.gob.mx.convocatoria.dto.DictamenReporteDTO;
import csg.gob.mx.convocatoria.service.DictamenService;

@RestController
@RequestMapping("/api/dictamenes")
public class DictamenController {

    @Autowired
    private DictamenService dictamenService;

    @GetMapping
    public ResponseEntity<List<DictamenReporteDTO>> listarDictamenes(
            @RequestParam(required = false) Long idConvocatoria,
            @RequestParam(required = false) Long idJuez,
            @RequestParam(required = false) String termino) {
        List<DictamenReporteDTO> lista = dictamenService.obtenerDictamenesConFiltros(idConvocatoria, idJuez, termino);
        return ResponseEntity.ok(lista);
    }
}