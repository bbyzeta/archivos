package csg.gob.mx.convocatoria.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import csg.gob.mx.convocatoria.dto.ConvocatoriaGeneralResponseDTO;
import csg.gob.mx.convocatoria.service.ConvocatoriaGeneralService;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/admin/convocatorias/general")
@RequiredArgsConstructor
public class ConvocatoriaGeneralController {

	private final ConvocatoriaGeneralService convocatoriaGeneralService;

	@GetMapping
	public ResponseEntity<List<ConvocatoriaGeneralResponseDTO>> listarConvocatoriasGenerales() {
		List<ConvocatoriaGeneralResponseDTO> respuesta = convocatoriaGeneralService.listarConvocatoriasGenerales();
		return ResponseEntity.ok(respuesta);
	}
}