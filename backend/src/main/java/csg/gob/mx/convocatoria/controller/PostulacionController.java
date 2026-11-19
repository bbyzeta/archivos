package csg.gob.mx.convocatoria.controller;

import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import csg.gob.mx.convocatoria.dto.PostulacionRegistroDTO;
import csg.gob.mx.convocatoria.dto.PostulacionResponseDTO;
import csg.gob.mx.convocatoria.service.PostulacionService;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/postulaciones")
@RequiredArgsConstructor
public class PostulacionController {

    private final PostulacionService postulacionService;

    @PostMapping("/iniciar")
    public ResponseEntity<PostulacionResponseDTO> iniciarOObtenerBorrador(@RequestBody PostulacionRegistroDTO dto) {
        PostulacionResponseDTO response = postulacionService.obtenerOCrearBorrador(dto);
        return ResponseEntity.status(HttpStatus.OK).body(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<PostulacionResponseDTO> obtenerExpediente(@PathVariable Long id) {
        return ResponseEntity.ok(postulacionService.obtenerPorId(id));
    }

    @PostMapping(value = "/{idPostulacion}/documentos/{idRequisito}", consumes = {"multipart/form-data"})
    public ResponseEntity<?> subirDocumentoRequisito(
            @PathVariable Long idPostulacion,
            @PathVariable Long idRequisito,
            @RequestParam("archivo") MultipartFile archivo
    ) {
        try {
            if (archivo.isEmpty() || !archivo.getOriginalFilename().toLowerCase().endsWith(".pdf") && !archivo.getOriginalFilename().toLowerCase().endsWith(".xls") 
            		&& !archivo.getOriginalFilename().toLowerCase().endsWith(".xlsx")) {
                return ResponseEntity.badRequest().body(Map.of("error", "El archivo debe ser un documento PDF/XLSX válido."));
            }

            PostulacionResponseDTO response = postulacionService.guardarDocumentoRequisito(idPostulacion, idRequisito, archivo);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                    "error", "No se pudo adjuntar el documento",
                    "detalle", e.getMessage()
            ));
        }
    }

    @PatchMapping("/{idPostulacion}/enviar")
    public ResponseEntity<?> enviarExpediente(@PathVariable Long idPostulacion) {
        try {
            PostulacionResponseDTO response = postulacionService.finalizarYEnviar(idPostulacion);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                    "error", "No se pudo formalizar el envío del expediente",
                    "detalle", e.getMessage()
            ));
        }
    }
    
    @PostMapping(value = "/{idPostulacion}/anexos/{idAnexo}", consumes = {"multipart/form-data"})
    public ResponseEntity<?> subirAnexoOpcional(
            @PathVariable Long idPostulacion,
            @RequestParam("archivo") MultipartFile archivo,
            @PathVariable Long idAnexo,
            @RequestParam(value = "nombreAnexo", required = false) String nombreAnexo
    ) {
        try {
            if (archivo.isEmpty() || !archivo.getOriginalFilename().toLowerCase().endsWith(".pdf") ) {
                return ResponseEntity.badRequest().body(Map.of("error", "El archivo debe ser un PDF válido."));
            }
            PostulacionResponseDTO response = postulacionService.guardarAnexoLibre(idPostulacion, nombreAnexo, archivo, idAnexo);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                    "error", "No se pudo adjuntar el anexo",
                    "detalle", e.getMessage()
            ));
        }
    }

    @DeleteMapping("/{idPostulacion}/anexos/{idAnexo}")
    public ResponseEntity<?> eliminarAnexo(
            @PathVariable Long idPostulacion,
            @PathVariable Long idAnexo
    ) {
        try {
            return ResponseEntity.ok(postulacionService.eliminarAnexoLibre(idPostulacion, idAnexo));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("error", e.getMessage()));
        }
    }
    
    @GetMapping("/pendientes/{idPostulacion}")
    public ResponseEntity<?> pendientes(
            @PathVariable Long idPostulacion            
    ) {
        try {
            return ResponseEntity.ok(postulacionService.pendientes(idPostulacion));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("error", e.getMessage()));
        }
    }
}