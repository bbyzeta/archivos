package csg.gob.mx.convocatoria.controller;

import java.util.Map;

import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import csg.gob.mx.convocatoria.dto.DictamenDocumentoRequestDTO;
import csg.gob.mx.convocatoria.dto.DictamenExpedienteRequestDTO;
import csg.gob.mx.convocatoria.dto.ExpedienteAuditoriaDTO;
import csg.gob.mx.convocatoria.dto.ExpedienteBandejaItemDTO;
import csg.gob.mx.convocatoria.service.AuditorService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/auditor/expedientes")
@RequiredArgsConstructor
public class AuditorExpedienteController {

    private final AuditorService auditorService;

    @GetMapping
    public ResponseEntity<Page<ExpedienteBandejaItemDTO>> listarExpedientesParaAuditoria(
            @RequestParam(name = "pagina", defaultValue = "0") int pagina,
            @RequestParam(name = "tamanio", defaultValue = "10") int tamanio,
            @RequestParam(name = "busqueda", required = false) String busqueda,
            @RequestParam(name = "idConvocatoria", required = false) Long idConvocatoria,
            @RequestParam(name = "estado", required = false) String estado
    ) {
        Page<ExpedienteBandejaItemDTO> resultado = auditorService.listarExpedientesParaAuditoria(
                pagina,
                tamanio,
                busqueda,
                idConvocatoria,
                estado
        );
        return ResponseEntity.ok(resultado);
    }

    @GetMapping("/{idExpediente:[0-9]+}")
    public ResponseEntity<?> obtenerExpedienteParaAuditoria(@PathVariable Long idExpediente) {
        try {
            ExpedienteAuditoriaDTO dto = auditorService.obtenerExpedienteParaAuditoria(idExpediente);
            return ResponseEntity.ok(dto);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of(
                    "error", "No se encontró el expediente solicitado",
                    "detalle", e.getMessage()
            ));
        }
    }

    @PatchMapping("/{idExpediente:[0-9]+}/documentos/{idDocumento:[0-9]+}")
    public ResponseEntity<?> actualizarDictamenDocumento(
            @PathVariable Long idExpediente,
            @PathVariable Long idDocumento,
            @Valid @RequestBody DictamenDocumentoRequestDTO body
    ) {
        try {
            auditorService.actualizarDictamenDocumento(idExpediente, idDocumento, body);
            return ResponseEntity.noContent().build();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                    "error", "No se pudo actualizar el estado del documento",
                    "detalle", e.getMessage()
            ));
        }
    }

    @PostMapping("/{idExpediente:[0-9]+}/dictamen")
    public ResponseEntity<?> dictaminarExpedienteAuditor(
            @PathVariable Long idExpediente,
            @Valid @RequestBody DictamenExpedienteRequestDTO body
    ) {
        try {
            auditorService.dictaminarExpedienteAuditor(idExpediente, body);
            return ResponseEntity.ok(Map.of(
                    "mensaje", "Dictamen del expediente registrado con éxito",
                    "nuevoEstado", body.getResultado()
            ));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                    "error", "No se pudo registrar el dictamen del expediente",
                    "detalle", e.getMessage()
            ));
        }
    }
}