package csg.gob.mx.convocatoria.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import csg.gob.mx.convocatoria.dto.ArgumentoPostulanteRequestDTO;
import csg.gob.mx.convocatoria.dto.ArgumentoPostulanteResponseDTO;
import csg.gob.mx.convocatoria.dto.EvaluacionPostulanteDTO;
import csg.gob.mx.convocatoria.service.ArgumentoPostulanteService;
import csg.gob.mx.convocatoria.service.EvaluacionPostulanteService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/evaluacion/postulante")
@RequiredArgsConstructor
public class EvaluacionPostulanteController {

    private final EvaluacionPostulanteService service;
    private final ArgumentoPostulanteService serviceArg;

   
    @PostMapping
    public ResponseEntity<List<EvaluacionPostulanteDTO>> crear(@RequestBody List<EvaluacionPostulanteDTO> dto) {
        return new ResponseEntity<>(service.crear(dto), HttpStatus.CREATED);
    }   
    
    @PostMapping("/argumentos")
    public ResponseEntity<ArgumentoPostulanteResponseDTO> crear(@RequestBody ArgumentoPostulanteRequestDTO request) {
        return new ResponseEntity<>(serviceArg.guardar(request), HttpStatus.CREATED);
    }

    @PostMapping("/masivo")
    public ResponseEntity<List<ArgumentoPostulanteResponseDTO>> crearMasivo(@RequestBody List<ArgumentoPostulanteRequestDTO> requests) {
        return new ResponseEntity<>(serviceArg.guardarMasivo(requests), HttpStatus.CREATED);
    }

    @GetMapping("/{id}")
    public ResponseEntity<ArgumentoPostulanteResponseDTO> obtenerPorId(@PathVariable Long id) {
        return ResponseEntity.ok(serviceArg.obtenerPorId(id));
    }

    @GetMapping
    public ResponseEntity<List<ArgumentoPostulanteResponseDTO>> listarTodos() {
        return ResponseEntity.ok(serviceArg.listarTodos());
    }

    @GetMapping("/postulante/{idUsuario}/juez/{idJuez}")
    public ResponseEntity<List<ArgumentoPostulanteResponseDTO>> obtenerPorPostulanteYJuez(
            @PathVariable Long idUsuario,
            @PathVariable Long idJuez) {
        return ResponseEntity.ok(serviceArg.obtenerPorPostulanteYJuez(idUsuario, idJuez));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ArgumentoPostulanteResponseDTO> actualizar(
            @PathVariable Long id,
            @Valid @RequestBody ArgumentoPostulanteRequestDTO request) {
        return ResponseEntity.ok(serviceArg.actualizar(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
    	serviceArg.eliminar(id);
        return ResponseEntity.noContent().build();
    }
    
    @GetMapping("/propuesto")
    public ResponseEntity<ArgumentoPostulanteResponseDTO> obtenerPorEspecifico(
            @RequestParam Long idConvocatoria,            
            @RequestParam Long idPostulante,
            @RequestParam Long idJuez) {

        ArgumentoPostulanteResponseDTO dto = serviceArg
                .obtenerPorConvocatoriaPostulanteYJuez(idConvocatoria, idPostulante, idJuez);
                
        return ResponseEntity.ok(dto);
    }
    
    @GetMapping("/juez/{idJuez}/convocatoria/{idConvocatoria}")
    public ResponseEntity<List<ArgumentoPostulanteResponseDTO>> obtenerPorJuezYConvocatoria(
            @PathVariable Long idJuez,
            @PathVariable Long idConvocatoria) {
        
        List<ArgumentoPostulanteResponseDTO> lista = serviceArg.obtenerPorJuezYConvocatoria(idJuez, idConvocatoria);
        return ResponseEntity.ok(lista);
    }
    
    @PostMapping("/valida/propuesta")
    public ResponseEntity<Boolean> validaPropuesta(
          @RequestBody ArgumentoPostulanteRequestDTO dto) {

        Boolean res = serviceArg
                .permiteProponer(dto);
                
        return ResponseEntity.ok(res);
    }
}