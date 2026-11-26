package csg.gob.mx.convocatoria.service;

import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import csg.gob.mx.convocatoria.dto.ChangePasswordRequest;
import csg.gob.mx.convocatoria.dto.CreateUserRequest;
import csg.gob.mx.convocatoria.dto.UserResponse;
import csg.gob.mx.convocatoria.entity.UserEntity;
import csg.gob.mx.convocatoria.repository.UserRepository;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional(readOnly = true)
    public List<UserResponse> findAll() {
        return userRepository.findAll().stream()
                .sorted((a, b) -> a.getCreatedAt().compareTo(b.getCreatedAt()))
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public UserResponse create(CreateUserRequest request) {
        String username = request.username().trim();
        if (userRepository.existsByUsername(username)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Ya existe un usuario con ese nombre.");
        }

        UserEntity user = new UserEntity();
        user.setUsername(username);
        user.setFullName(request.fullName().trim());
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setRole(request.role());
        user.setActive(true);
        return toResponse(userRepository.save(user));
    }

    /** Activa o desactiva un usuario. Nadie puede desactivar su propia cuenta (evita quedarse sin acceso). */
    @Transactional
    public UserResponse setActive(UUID id, boolean active, String currentUsername) {
        UserEntity user = getUser(id);
        if (!active && user.getUsername().equals(currentUsername)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "No puedes desactivar tu propia cuenta.");
        }
        user.setActive(active);
        return toResponse(userRepository.save(user));
    }

    /** Restablece la contraseña de otro usuario (acción de administrador). */
    @Transactional
    public void resetPassword(UUID id, String newPassword) {
        UserEntity user = getUser(id);
        user.setPasswordHash(passwordEncoder.encode(newPassword));
        userRepository.save(user);
    }

    /** Cambia la contraseña propia verificando la actual. Un error responde 400 (no 401) para no cerrar la sesión. */
    @Transactional
    public void changeOwnPassword(String username, ChangePasswordRequest request) {
        UserEntity user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));

        if (!passwordEncoder.matches(request.currentPassword(), user.getPasswordHash())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La contraseña actual es incorrecta.");
        }
        if (request.currentPassword().equals(request.newPassword())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La nueva contraseña debe ser distinta de la actual.");
        }
        user.setPasswordHash(passwordEncoder.encode(request.newPassword()));
        userRepository.save(user);
    }

    private UserEntity getUser(UUID id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuario no encontrado."));
    }

    private UserResponse toResponse(UserEntity u) {
        return new UserResponse(u.getId(), u.getUsername(), u.getFullName(), u.getRole(), u.isActive(), u.getCreatedAt());
    }
}
