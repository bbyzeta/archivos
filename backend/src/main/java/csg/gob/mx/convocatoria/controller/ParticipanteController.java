package csg.gob.mx.convocatoria.controller;

import csg.gob.mx.convocatoria.dto.ParticipanteDTO;
import csg.gob.mx.convocatoria.dto.ParticipanteRequestDTO;
import csg.gob.mx.convocatoria.service.ParticipanteService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/participantes")
@RequiredArgsConstructor
public class ParticipanteController {

    private final ParticipanteService participanteService;

    @GetMapping
    public ResponseEntity<Page<ParticipanteDTO>> listar(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "5") int size,
            @RequestParam(required = false) String busqueda) {
        return ResponseEntity.ok(participanteService.listar(page, size, busqueda));
    }

    @PostMapping
    public ResponseEntity<ParticipanteDTO> crear(@Valid @RequestBody ParticipanteRequestDTO request) {
        return new ResponseEntity<>(participanteService.crear(request), HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    public ResponseEntity<ParticipanteDTO> actualizar(
            @PathVariable Long id,
            @Valid @RequestBody ParticipanteRequestDTO request) {
        return ResponseEntity.ok(participanteService.actualizar(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        participanteService.eliminar(id);
        return ResponseEntity.noContent().build();
    }
    
    @PatchMapping("/{id}/toggle-activo")
    public ResponseEntity<Void> toggleActivo(@PathVariable Long id) {
        participanteService.toggleActivo(id);
        return ResponseEntity.noContent().build();
    }
}