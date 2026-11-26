package csg.gob.mx.convocatoria.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import csg.gob.mx.convocatoria.dto.ChangePasswordRequest;
import csg.gob.mx.convocatoria.dto.CreateUserRequest;
import csg.gob.mx.convocatoria.dto.ResetPasswordRequest;
import csg.gob.mx.convocatoria.dto.SetActiveRequest;
import csg.gob.mx.convocatoria.dto.UpdateUserRequest;
import csg.gob.mx.convocatoria.dto.UserResponse;
import csg.gob.mx.convocatoria.service.UserService;
import jakarta.validation.Valid;

/** Gestión de usuarios. Todo excepto /me/password exige rol ADMIN (ver SecurityConfig). */
@RestController
@RequestMapping("/api/admin/users")
public class AdminUserController {

    private final UserService userService;

    public AdminUserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping
    public List<UserResponse> list() {
        return userService.findAll();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public UserResponse create(@Valid @RequestBody CreateUserRequest request) {
        return userService.create(request);
    }

    @PatchMapping("/{id}")
    public UserResponse update(@PathVariable UUID id, @Valid @RequestBody UpdateUserRequest request,
            Authentication authentication) {
        return userService.update(id, request, authentication.getName());
    }

    @PatchMapping("/{id}/active")
    public UserResponse setActive(@PathVariable UUID id, @RequestBody SetActiveRequest request,
            Authentication authentication) {
        return userService.setActive(id, request.active(), authentication.getName());
    }

    @PutMapping("/{id}/password")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void resetPassword(@PathVariable UUID id, @Valid @RequestBody ResetPasswordRequest request) {
        userService.resetPassword(id, request.newPassword());
    }

    /** Cambio de la contraseña propia (cualquier usuario autenticado). */
    @PutMapping("/me/password")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void changeOwnPassword(@Valid @RequestBody ChangePasswordRequest request, Authentication authentication) {
        userService.changeOwnPassword(authentication.getName(), request);
    }
}
