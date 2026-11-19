package csg.gob.mx.convocatoria.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import csg.gob.mx.convocatoria.dto.CriterioEvaluacionDTO;
import csg.gob.mx.convocatoria.dto.CriterioEvaluacionRequestDTO;
import csg.gob.mx.convocatoria.service.CriterioEvaluacionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/criterios")
@RequiredArgsConstructor
public class CriterioEvaluacionController {

    private final CriterioEvaluacionService service;

    @GetMapping
    public ResponseEntity<List<CriterioEvaluacionDTO>> obtenerTodos() {
        return ResponseEntity.ok(service.obtenerTodos());
    }

    @GetMapping("/{id}")
    public ResponseEntity<CriterioEvaluacionDTO> obtenerPorId(@PathVariable("id") Integer id) {
        return ResponseEntity.ok(service.obtenerPorId(id));
    }

    @GetMapping("/convocatoria/{idConvocatoria}/{idUsuario}")
    public ResponseEntity<List<CriterioEvaluacionDTO>> obtenerPorIdConvocatoria(@PathVariable("idConvocatoria") Long idConvocatoria, @PathVariable("idUsuario") Long idUsuario) {
        return ResponseEntity.ok(service.obtenerPorIdConvocatoria(idConvocatoria, idUsuario));
    }

    @PostMapping
    public ResponseEntity<CriterioEvaluacionDTO> crear(@Valid @RequestBody CriterioEvaluacionRequestDTO dto) {
        return new ResponseEntity<>(service.crear(dto), HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    public ResponseEntity<CriterioEvaluacionDTO> actualizar(
            @PathVariable("id") Integer id,
            @Valid @RequestBody CriterioEvaluacionRequestDTO dto) {
        return ResponseEntity.ok(service.actualizar(id, dto));
    }

    @PatchMapping("/{id}/desactivar")
    public ResponseEntity<Void> eliminarLogico(@PathVariable("id") Integer id) {
        service.eliminarLogico(id);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminarFisico(@PathVariable("id") Integer id) {
        service.eliminarFisico(id);
        return ResponseEntity.noContent().build();
    }
}