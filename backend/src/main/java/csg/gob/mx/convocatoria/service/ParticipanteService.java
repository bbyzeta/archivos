package csg.gob.mx.convocatoria.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import csg.gob.mx.convocatoria.dto.DatosInstitucionalesDTO;
import csg.gob.mx.convocatoria.dto.ParticipanteDTO;
import csg.gob.mx.convocatoria.dto.ParticipanteRequestDTO;
import csg.gob.mx.convocatoria.entity.DatosInstitucionalesEntity;
import csg.gob.mx.convocatoria.entity.EstadoEntity;
import csg.gob.mx.convocatoria.entity.ParticipanteEntity;
import csg.gob.mx.convocatoria.entity.RolEntity;
import csg.gob.mx.convocatoria.entity.UsuarioEntity;
import csg.gob.mx.convocatoria.repository.EstadoRepository;
import csg.gob.mx.convocatoria.repository.ParticipanteRepository;
import csg.gob.mx.convocatoria.repository.RolRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ParticipanteService {

    private final ParticipanteRepository participanteRepository;
    private final EstadoRepository estadoRepository;
    private final RolRepository rolRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional(readOnly = true)
    public Page<ParticipanteDTO> listar(int page, int size, String termino) {
        PageRequest pageable = PageRequest.of(page, size);
        Page<ParticipanteEntity> resultado;

        if (termino != null && !termino.trim().isEmpty()) {
            resultado = participanteRepository.buscarConFiltro(termino.trim(), pageable);
        } else {
            resultado = participanteRepository.findAll(pageable);
        }

        return resultado.map(this::toDTO);
    }

    @Transactional
    public ParticipanteDTO crear(ParticipanteRequestDTO request) {
        EstadoEntity estado = estadoRepository.findById(request.getDatosInstitucionales().getIdEstado())
                .orElseThrow(() -> new EntityNotFoundException("Estado no encontrado"));

        RolEntity rol = rolRepository.findById(4)
                .orElseThrow(() -> new RuntimeException("El rol con ID 4 no existe"));
        
        UsuarioEntity usuario = new UsuarioEntity();
        usuario.setUsername(request.getUsername());
        usuario.setPassword(passwordEncoder.encode(request.getPassword())); 
        usuario.setActivo(request.getActivo() != null ? request.getActivo() : true);
        usuario.getRoles().add(rol);

        ParticipanteEntity participante = new ParticipanteEntity();
        participante.setUsuario(usuario);
        participante.setNombres(request.getNombres());
        participante.setApellidos(request.getApellidos());
        participante.setCurp(request.getCurp().toUpperCase());
        participante.setTelefono(request.getTelefono());
        participante.setAceptoTerminos(false);

        DatosInstitucionalesEntity inst = new DatosInstitucionalesEntity();
        inst.setParticipante(participante);
        inst.setEstado(estado);
        inst.setInstitucion(request.getDatosInstitucionales().getInstitucion());
        inst.setCargo(request.getDatosInstitucionales().getCargo());

        participante.setDatosInstitucionales(inst);

        ParticipanteEntity guardado = participanteRepository.save(participante);
        return toDTO(guardado);
    }

    @Transactional
    public ParticipanteDTO actualizar(Long id, ParticipanteRequestDTO request) {
        ParticipanteEntity participante = participanteRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Participante no encontrado con id: " + id));

        EstadoEntity estado = estadoRepository.findById(request.getDatosInstitucionales().getIdEstado())
                .orElseThrow(() -> new EntityNotFoundException("Estado no encontrado"));

        UsuarioEntity usuario = participante.getUsuario();
        if (usuario != null) {
            usuario.setUsername(request.getUsername());
            if (request.getActivo() != null) {
                usuario.setActivo(request.getActivo());
            }
            if (request.getPassword() != null && !request.getPassword().isBlank()) {
                usuario.setPassword(passwordEncoder.encode(request.getPassword()));
            }
        }

        participante.setNombres(request.getNombres());
        participante.setApellidos(request.getApellidos());
        participante.setCurp(request.getCurp().toUpperCase());
        participante.setTelefono(request.getTelefono());

        DatosInstitucionalesEntity inst = participante.getDatosInstitucionales();
        if (inst == null) {
            inst = new DatosInstitucionalesEntity();
            inst.setParticipante(participante);
        }
        inst.setEstado(estado);
        inst.setInstitucion(request.getDatosInstitucionales().getInstitucion());
        inst.setCargo(request.getDatosInstitucionales().getCargo());

        participante.setDatosInstitucionales(inst);

        ParticipanteEntity actualizado = participanteRepository.save(participante);
        return toDTO(actualizado);
    }

    @Transactional
    public void eliminar(Long id) {
        ParticipanteEntity participante = participanteRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Participante no encontrado con id: " + id));
        
        participanteRepository.delete(participante);
    }

    private ParticipanteDTO toDTO(ParticipanteEntity entity) {
        DatosInstitucionalesDTO instDTO = null;
        if (entity.getDatosInstitucionales() != null) {
            instDTO = DatosInstitucionalesDTO.builder()
                    .idInstitucional(entity.getDatosInstitucionales().getIdInstitucional())
                    .idEstado(entity.getDatosInstitucionales().getEstado() != null ? entity.getDatosInstitucionales().getEstado().getIdEstado() : null)
                    .nombreEstado(entity.getDatosInstitucionales().getEstado() != null ? entity.getDatosInstitucionales().getEstado().getNombre() : null)
                    .institucion(entity.getDatosInstitucionales().getInstitucion())
                    .cargo(entity.getDatosInstitucionales().getCargo())
                    .build();
        }

        String username = entity.getUsuario() != null ? entity.getUsuario().getUsername() : null;
        Boolean activo = entity.getUsuario() != null ? entity.getUsuario().isActivo() : false;
        Long idUsuario = entity.getUsuario() != null ? entity.getUsuario().getIdUsuario() : null;

        return ParticipanteDTO.builder()
                .idParticipante(entity.getIdParticipante())
                .idUsuario(idUsuario)
                .nombres(entity.getNombres())
                .apellidos(entity.getApellidos())
                .curp(entity.getCurp())
                .telefono(entity.getTelefono())
                .username(username)
                .activo(activo)
                .aceptoTerminos(entity.getAceptoTerminos())
                .fechaRegistro(entity.getFechaRegistro())
                .datosInstitucionales(instDTO)
                .build();
    }
    

    @Transactional
    public void toggleActivo(Long idParticipante) {
        ParticipanteEntity participante = participanteRepository.findById(idParticipante)
                .orElseThrow(() -> new EntityNotFoundException(
                        "No se encontró el participante con ID: " + idParticipante));

        UsuarioEntity usuario = participante.getUsuario();
        
        if (usuario == null) {
            throw new IllegalStateException(
                    "El participante con ID " + idParticipante + " no tiene un usuario asociado.");
        }

        boolean nuevoEstado = !Boolean.TRUE.equals(usuario.isActivo());
        usuario.setActivo(nuevoEstado);

        participanteRepository.save(participante);
    }
}