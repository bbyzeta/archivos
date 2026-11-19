package csg.gob.mx.convocatoria.service;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import csg.gob.mx.convocatoria.dto.JuradoRequestDTO;
import csg.gob.mx.convocatoria.dto.JuradoResponseDTO;
import csg.gob.mx.convocatoria.entity.ConvocatoriaJuradoEntity;
import csg.gob.mx.convocatoria.entity.PerfilJuradoEntity;
import csg.gob.mx.convocatoria.entity.RolEntity;
import csg.gob.mx.convocatoria.entity.UsuarioEntity;
import csg.gob.mx.convocatoria.repository.ConvocatoriaJuradoRepository;
import csg.gob.mx.convocatoria.repository.PerfilJuradoRepository;
import csg.gob.mx.convocatoria.repository.RolRepository;
import csg.gob.mx.convocatoria.repository.UsuarioRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class JuradoService {

	private final PerfilJuradoRepository perfilJuradoRepository;
	private final UsuarioRepository usuarioRepository;
	private final RolRepository rolRepository;
	private final PasswordEncoder passwordEncoder;
	private final ConvocatoriaJuradoRepository convocatoriaJuradoRepository;

	@Transactional(readOnly = true)
	public Page<JuradoResponseDTO> listarPaginado(int page, int size, String filtro) {
		Pageable pageable = PageRequest.of(page, size, Sort.by("usuario.fechaCreacion").descending());
		Page<PerfilJuradoEntity> resultado = perfilJuradoRepository.buscarConFiltroPaginado(filtro, pageable);
		return resultado.map(this::mapearAResponse);
	}

	@Transactional(readOnly = true)
	public JuradoResponseDTO obtenerPorId(Integer idJurado) {
		PerfilJuradoEntity entity = perfilJuradoRepository.findById(idJurado)
				.orElseThrow(() -> new EntityNotFoundException("Jurado no encontrado con ID: " + idJurado));
		return mapearAResponse(entity);
	}

	@Transactional
	public JuradoResponseDTO crearJurado(JuradoRequestDTO dto) {
		String usernameLimpio = dto.getUsername().trim().toLowerCase();

		if (usuarioRepository.existsByUsername(usernameLimpio)) {
			throw new IllegalArgumentException("Ya existe un usuario con el correo institucional proporcionado");
		}

		if (dto.getPassword() == null || dto.getPassword().isBlank()) {
			throw new IllegalArgumentException("La contraseña es obligatoria para el registro");
		}

		UsuarioEntity usuario = new UsuarioEntity();
		usuario.setUsername(usernameLimpio);
		usuario.setPassword(passwordEncoder.encode(dto.getPassword()));
		usuario.setActivo(dto.getActivo() != null ? dto.getActivo() : true);
		usuario.setFechaCreacion(LocalDateTime.now());

		RolEntity rolJurado = rolRepository.findAll().stream()
				.filter(r -> "ROLE_JURADO".equalsIgnoreCase(r.getNombre())).findFirst()
				.orElseGet(() -> rolRepository.save(new RolEntity("ROLE_JURADO")));

		usuario.setRoles(Collections.singleton(rolJurado));
		UsuarioEntity usuarioGuardado = usuarioRepository.save(usuario);

		PerfilJuradoEntity perfil = new PerfilJuradoEntity();
		perfil.setUsuario(usuarioGuardado);
		perfil.setGradoAcademico(dto.getGradoAcademico());
		perfil.setNombres(dto.getNombres().trim());
		perfil.setApellidos(dto.getApellidos().trim());
		perfil.setRfc(dto.getRfc() != null ? dto.getRfc().trim().toUpperCase() : null);
		perfil.setTelefono(dto.getTelefono().trim());
		perfil.setInstitucionAdscripcion(dto.getInstitucionAdscripcion().trim());
		perfil.setCargoActual(dto.getCargoActual().trim());
		perfil.setAreaConocimiento(dto.getAreaConocimiento());
		perfil.setOrcidUrl(dto.getOrcidUrl() != null ? dto.getOrcidUrl().trim() : null);
		perfil.setLineasInvestigacion(dto.getLineasInvestigacion().trim());
		perfil.setAceptoConflictoInteres(dto.getAceptoConflictoInteres());
		perfil.setAceptoConfidencialidad(dto.getAceptoConfidencialidad());
		perfil.setFechaAceptacionConfidencialidad(LocalDateTime.now());

		PerfilJuradoEntity perfilGuardado = perfilJuradoRepository.save(perfil);
		return mapearAResponse(perfilGuardado);
	}

	@Transactional
	public JuradoResponseDTO actualizarJurado(Integer idJurado, JuradoRequestDTO dto) {
		PerfilJuradoEntity perfil = perfilJuradoRepository.findById(idJurado)
				.orElseThrow(() -> new EntityNotFoundException("Jurado no encontrado con ID: " + idJurado));

		UsuarioEntity usuario = perfil.getUsuario();
		String usernameLimpio = dto.getUsername().trim().toLowerCase();

		if (!usuario.getUsername().equalsIgnoreCase(usernameLimpio)
				&& usuarioRepository.existsByUsername(usernameLimpio)) {
			throw new IllegalArgumentException("El correo ya se encuentra en uso por otro registro");
		}

		usuario.setUsername(usernameLimpio);

		if (dto.getPassword() != null && !dto.getPassword().isBlank()) {
			usuario.setPassword(passwordEncoder.encode(dto.getPassword()));
		}

		if (dto.getActivo() != null) {
			usuario.setActivo(dto.getActivo());
		}

		usuarioRepository.save(usuario);

		perfil.setGradoAcademico(dto.getGradoAcademico());
		perfil.setNombres(dto.getNombres().trim());
		perfil.setApellidos(dto.getApellidos().trim());
		perfil.setRfc(dto.getRfc() != null ? dto.getRfc().trim().toUpperCase() : null);
		perfil.setTelefono(dto.getTelefono().trim());
		perfil.setInstitucionAdscripcion(dto.getInstitucionAdscripcion().trim());
		perfil.setCargoActual(dto.getCargoActual().trim());
		perfil.setAreaConocimiento(dto.getAreaConocimiento());
		perfil.setOrcidUrl(dto.getOrcidUrl() != null ? dto.getOrcidUrl().trim() : null);
		perfil.setLineasInvestigacion(dto.getLineasInvestigacion().trim());

		return mapearAResponse(perfilJuradoRepository.save(perfil));
	}

	@Transactional
	public void cambiarEstadoActivo(Integer idJurado, boolean activo) {
		PerfilJuradoEntity perfil = perfilJuradoRepository.findById(idJurado)
				.orElseThrow(() -> new EntityNotFoundException("Jurado no encontrado con ID: " + idJurado));

		UsuarioEntity usuario = perfil.getUsuario();
		usuario.setActivo(activo);
		usuarioRepository.save(usuario);
	}

	@Transactional
	public void eliminarJurado(Integer idJurado) {
		PerfilJuradoEntity perfil = perfilJuradoRepository.findById(idJurado)
				.orElseThrow(() -> new EntityNotFoundException("Jurado no encontrado con ID: " + idJurado));

		Long idUsuario = perfil.getUsuario().getIdUsuario();
		perfilJuradoRepository.delete(perfil);
		usuarioRepository.deleteById(idUsuario);
	}

	private JuradoResponseDTO mapearAResponse(PerfilJuradoEntity entity) {
		UsuarioEntity user = entity.getUsuario();
		return JuradoResponseDTO.builder().idJurado(entity.getId())
				.idUsuario(user.getIdUsuario() != null ? user.getIdUsuario().intValue() : null)
				.gradoAcademico(entity.getGradoAcademico()).nombres(entity.getNombres())
				.apellidos(entity.getApellidos()).rfc(entity.getRfc()).telefono(entity.getTelefono())
				.username(user.getUsername()).institucionAdscripcion(entity.getInstitucionAdscripcion())
				.cargoActual(entity.getCargoActual()).areaConocimiento(entity.getAreaConocimiento())
				.orcidUrl(entity.getOrcidUrl()).lineasInvestigacion(entity.getLineasInvestigacion())
				.aceptoConflictoInteres(entity.getAceptoConflictoInteres())
				.aceptoConfidencialidad(entity.getAceptoConfidencialidad()).activo(user.isActivo())
				.fechaRegistro(user.getFechaCreacion()).build();
	}

	@Transactional(readOnly = true)
	public List<Integer> obtenerJuradosAsignadosAConvocatoria(Integer idConvocatoria) {
		return convocatoriaJuradoRepository.findJuradoIdsByConvocatoriaId(idConvocatoria);
	}

	@Transactional
	public void asignarJuradosAConvocatoria(Integer idConvocatoria, List<Integer> juradoIds) {
		convocatoriaJuradoRepository.deleteByIdConvocatoria(idConvocatoria);

		if (juradoIds == null || juradoIds.isEmpty()) {
			return;
		}

		List<PerfilJuradoEntity> jurados = perfilJuradoRepository.findAllById(juradoIds);
		List<ConvocatoriaJuradoEntity> asignaciones = jurados.stream()
				.map(j -> new ConvocatoriaJuradoEntity(idConvocatoria, j)).toList();

		convocatoriaJuradoRepository.saveAll(asignaciones);
	}
	
	@Transactional(readOnly = true)
	public List<JuradoResponseDTO> obtenerJuradosDetallePorConvocatoria(Integer idConvocatoria) {
	    List<ConvocatoriaJuradoEntity> asignaciones = convocatoriaJuradoRepository.findAll().stream()
	            .filter(cj -> cj.getIdConvocatoria().equals(idConvocatoria))
	            .toList();

	    return asignaciones.stream()
	            .map(cj -> mapearAResponse(cj.getJurado()))
	            .toList();
	}
}