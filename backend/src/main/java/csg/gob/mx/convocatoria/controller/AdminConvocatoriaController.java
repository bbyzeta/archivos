package csg.gob.mx.convocatoria.controller;

import java.util.List;
import java.util.Map;

import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import csg.gob.mx.convocatoria.dto.ConvocatoriaResponseDTO;
import csg.gob.mx.convocatoria.entity.ConvocatoriaEntity;
import csg.gob.mx.convocatoria.service.ConvocatoriaService;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/admin/convocatorias")
@RequiredArgsConstructor
public class AdminConvocatoriaController {

	private final ConvocatoriaService convocatoriaService;

	@GetMapping
	public ResponseEntity<Page<ConvocatoriaResponseDTO>> listarConvocatorias(
			@RequestParam(name = "pagina", defaultValue = "0") int pagina,
			@RequestParam(name = "tamanio", defaultValue = "5") int tamanio,
			@RequestParam(name = "busqueda", required = false) String busqueda) {
		Page<ConvocatoriaResponseDTO> respuesta = convocatoriaService.listarConvocatorias(pagina, tamanio, busqueda);
		return ResponseEntity.ok(respuesta);
	}

	@GetMapping("/activa")
	public ResponseEntity<?> obtenerConvocatoriaActiva() {
		try {
			ConvocatoriaResponseDTO dto = convocatoriaService.obtenerConvocatoriaActiva();
			return ResponseEntity.ok(dto);
		} catch (Exception e) {
			return ResponseEntity.status(HttpStatus.NOT_FOUND)
					.body(Map.of("error", "No se encontró ninguna convocatoria activa", "detalle", e.getMessage()));
		}
	}

	@GetMapping("/{id:[0-9]+}")
	public ResponseEntity<?> obtenerConvocatoriaPorId(@PathVariable Long id) {
		try {
			ConvocatoriaResponseDTO dto = convocatoriaService.obtenerPorId(id);
			return ResponseEntity.ok(dto);
		} catch (Exception e) {
			return ResponseEntity.status(HttpStatus.NOT_FOUND)
					.body(Map.of("error", "No se encontró la convocatoria", "detalle", e.getMessage()));
		}
	}

	@PostMapping(consumes = { "multipart/form-data" })
	public ResponseEntity<?> registrarConvocatoria(@RequestParam("titulo") String titulo,
			@RequestParam(value = "subtitulo", required = false) String subtitulo,
			@RequestParam("tituloBlog") String tituloBlog, @RequestParam("fechaApertura") String fechaApertura,
			@RequestParam("fechaCierre") String fechaCierre, @RequestParam("contenidoBlog") String contenidoBlog,
			@RequestParam("activa") Boolean activa,
			@RequestParam(value = "imagen", required = false) MultipartFile imagen,
			@RequestParam("documentoBases") MultipartFile documentoBases,
			@RequestParam("requisitos") String requisitosJson,
			@RequestParam(value = "ganadoresHistoricos", required = false) String ganadoresHistoricosJson,
			@RequestParam("idGrupoPremio") Integer idGrupoPremio){
		try {
			ConvocatoriaEntity nuevaConvocatoria = convocatoriaService.crearConvocatoria(titulo, subtitulo, tituloBlog,
					fechaApertura, fechaCierre, contenidoBlog, activa, imagen, documentoBases, requisitosJson,
					ganadoresHistoricosJson, idGrupoPremio);

			return ResponseEntity.status(HttpStatus.CREATED)
					.body(Map.of("mensaje", "Convocatoria creada exitosamente", "id", nuevaConvocatoria.getId()));

		} catch (Exception e) {
			e.printStackTrace();
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
					.body(Map.of("error", "Ocurrió un error al guardar la convocatoria", "detalle", e.getMessage()));
		}
	}

	@PutMapping(value = "/{id}", consumes = { "multipart/form-data" })
	public ResponseEntity<?> actualizarConvocatoria(@PathVariable Long id, @RequestParam("titulo") String titulo,
			@RequestParam(value = "subtitulo", required = false) String subtitulo,
			@RequestParam("tituloBlog") String tituloBlog, @RequestParam("fechaApertura") String fechaApertura,
			@RequestParam("fechaCierre") String fechaCierre, @RequestParam("contenidoBlog") String contenidoBlog,
			@RequestParam("activa") Boolean activa,
			@RequestParam(value = "imagen", required = false) MultipartFile imagen,
			@RequestParam(value = "documentoBases", required = false) MultipartFile documentoBases,
			@RequestParam("requisitos") String requisitosJson,
			@RequestParam(value = "ganadoresHistoricos", required = false) String ganadoresHistoricosJson,
			@RequestParam("idGrupoPremio") Integer idGrupoPremio) {
		try {
			ConvocatoriaEntity actualizada = convocatoriaService.actualizarConvocatoria(id, titulo, subtitulo,
					tituloBlog, fechaApertura, fechaCierre, contenidoBlog, activa, imagen, documentoBases,
					requisitosJson, ganadoresHistoricosJson, idGrupoPremio);

			return ResponseEntity
					.ok(Map.of("mensaje", "Convocatoria actualizada exitosamente", "id", actualizada.getId()));

		} catch (Exception e) {
			e.printStackTrace();
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
					.body(Map.of("error", "Ocurrió un error al actualizar la convocatoria", "detalle", e.getMessage()));
		}
	}

	@PatchMapping("/{id}/toggle-activa")
	public ResponseEntity<?> alternarEstadoActiva(@PathVariable Long id) {
		try {
			convocatoriaService.alternarEstadoActiva(id);
			return ResponseEntity.noContent().build();
		} catch (Exception e) {
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
					Map.of("error", "No se pudo cambiar el estado de la convocatoria", "detalle", e.getMessage()));
		}
	}

	@DeleteMapping("/{id}")
	public ResponseEntity<?> eliminarConvocatoria(@PathVariable Long id) {
		try {
			convocatoriaService.eliminarConvocatoria(id);
			return ResponseEntity.noContent().build();
		} catch (Exception e) {
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
					.body(Map.of("error", "No se pudo eliminar la convocatoria", "detalle", e.getMessage()));
		}
	}
	
	@GetMapping("/categoria/{id:[0-9]+}")
	public ResponseEntity<?> obtenerConvocatoriaPorIdCategoria(@PathVariable Integer id) {
		try {
			List<ConvocatoriaResponseDTO> dto = convocatoriaService.obtenerPorIdCategoria(id);
			return ResponseEntity.ok(dto);
		} catch (Exception e) {
			return ResponseEntity.status(HttpStatus.NOT_FOUND)
					.body(Map.of("error", "No se encontró la convocatoria", "detalle", e.getMessage()));
		}
	}
}