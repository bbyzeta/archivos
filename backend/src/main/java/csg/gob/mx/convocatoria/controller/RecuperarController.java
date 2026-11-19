package csg.gob.mx.convocatoria.controller;


import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import csg.gob.mx.convocatoria.dto.CambiarPasswordDTO;
import csg.gob.mx.convocatoria.service.RecuperarService;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/cuenta")
public class RecuperarController {

    @Autowired
    private RecuperarService recuperarService;

    @PostMapping("/recuperar")
    public ResponseEntity<?> recuperar(@RequestParam String correo) {
        try {
        	
             recuperarService.recuperarPassWord(correo);
            
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(null);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "No existe el correo electrónico ingresado."));
        }
    }
    
    @PostMapping("/restablecer")
    public ResponseEntity<Map<String, String>> restablecerPassword(@Valid @RequestBody CambiarPasswordDTO dto) {
    	try {
	    	recuperarService.cambiarPasswordConToken(dto);
	        Map<String, String> response = new HashMap<>();
	        response.put("mensaje", "Su contraseña ha sido actualizada exitosamente.");
	        return ResponseEntity.ok(response);
    	 } catch (IllegalArgumentException e) {
             return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
         } catch (Exception e) {
             return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                     .body(Map.of("error", "El token ya no es valido"));
         }
    }
}