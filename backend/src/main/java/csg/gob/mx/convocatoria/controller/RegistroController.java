package csg.gob.mx.convocatoria.controller;


import csg.gob.mx.convocatoria.dto.RegistroDTO;
import csg.gob.mx.convocatoria.service.RegistroService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/registro")
public class RegistroController {

    @Autowired
    private RegistroService registroService;

    @PostMapping
    public ResponseEntity<?> registrar(@Valid @RequestBody RegistroDTO registroDTO) {
        try {
            registroService.registrarParticipante(registroDTO);
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(Map.of("mensaje", "Registro completado con éxito."));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Ocurrió un error al procesar el registro."));
        }
    }
}