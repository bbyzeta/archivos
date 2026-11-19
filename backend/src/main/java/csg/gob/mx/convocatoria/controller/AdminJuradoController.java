package csg.gob.mx.convocatoria.controller;

import org.springframework.data.domain.Page;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import csg.gob.mx.convocatoria.dto.EstadoJuradoDTO;
import csg.gob.mx.convocatoria.dto.JuradoRequestDTO;
import csg.gob.mx.convocatoria.dto.JuradoResponseDTO;
import csg.gob.mx.convocatoria.service.JuradoService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/admin/jurados")
@RequiredArgsConstructor
public class AdminJuradoController {

    private final JuradoService juradoService;

    @GetMapping
    public ResponseEntity<Page<JuradoResponseDTO>> listarJurados(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false, defaultValue = "") String filtro) {
        return ResponseEntity.ok(juradoService.listarPaginado(page, size, filtro));
    }

    @GetMapping("/{id}")
    public ResponseEntity<JuradoResponseDTO> obtenerDetalle(@PathVariable Integer id) {
        return ResponseEntity.ok(juradoService.obtenerPorId(id));
    }

    @PostMapping
    public ResponseEntity<JuradoResponseDTO> registrarJurado(@Valid @RequestBody JuradoRequestDTO dto) {
        JuradoResponseDTO creado = juradoService.crearJurado(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(creado);
    }

    @PutMapping("/{id}")
    public ResponseEntity<JuradoResponseDTO> actualizarJurado(
            @PathVariable Integer id,
            @Valid @RequestBody JuradoRequestDTO dto) {
        return ResponseEntity.ok(juradoService.actualizarJurado(id, dto));
    }

    @PatchMapping("/{id}/estado")
    public ResponseEntity<Void> alternarEstado(
            @PathVariable Integer id,
            @Valid @RequestBody EstadoJuradoDTO estadoDTO) {
        juradoService.cambiarEstadoActivo(id, estadoDTO.getActivo());
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminarJurado(@PathVariable Integer id) {
        juradoService.eliminarJurado(id);
        return ResponseEntity.noContent().build();
    }
}