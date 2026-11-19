package csg.gob.mx.convocatoria.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import csg.gob.mx.convocatoria.dto.AsignacionJuradosDTO;
import csg.gob.mx.convocatoria.dto.JuradoResponseDTO;
import csg.gob.mx.convocatoria.service.JuradoService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/admin/convocatorias/{idConvocatoria}/jurados")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminConvocatoriaJuradoController {

    private final JuradoService juradoService;

    @GetMapping("/ids")
    public ResponseEntity<List<Integer>> obtenerJuradosAsignados(@PathVariable Integer idConvocatoria) {
        return ResponseEntity.ok(juradoService.obtenerJuradosAsignadosAConvocatoria(idConvocatoria));
    }

    @PostMapping
    public ResponseEntity<Void> asignarJurados(
            @PathVariable Integer idConvocatoria,
            @Valid @RequestBody AsignacionJuradosDTO dto) {
        juradoService.asignarJuradosAConvocatoria(idConvocatoria, dto.getJuradoIds());
        return ResponseEntity.noContent().build();
    }
    
    @GetMapping("/detalle")
    public ResponseEntity<List<JuradoResponseDTO>> obtenerJuradosDetalle(@PathVariable Integer idConvocatoria) {
        return ResponseEntity.ok(juradoService.obtenerJuradosDetallePorConvocatoria(idConvocatoria));
    }
}