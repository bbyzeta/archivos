package csg.gob.mx.convocatoria.controller;


import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import csg.gob.mx.convocatoria.service.RegistroService;

@RestController
@RequestMapping("/api/activar")
public class ActivarController {

    @Autowired
    private RegistroService registroService;

    @PostMapping
    public ResponseEntity<?> activar(@RequestParam String token) {
        try {
        	
            Integer resultado = registroService.activarParticipante(token);
            
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(resultado);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "El token ya se ha utilizado o no existe el usuario."));
        }
    }
}