package csg.gob.mx.convocatoria.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.server.ResponseStatusException;

import csg.gob.mx.convocatoria.dto.ChangePasswordRequest;
import csg.gob.mx.convocatoria.dto.CreateUserRequest;
import csg.gob.mx.convocatoria.dto.UpdateUserRequest;
import csg.gob.mx.convocatoria.dto.UserResponse;
import csg.gob.mx.convocatoria.entity.UserEntity;
import csg.gob.mx.convocatoria.repository.UserRepository;

class UserServiceTest {

    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
    private UserRepository userRepository;
    private UserService service;

    @BeforeEach
    void setUp() {
        userRepository = mock(UserRepository.class);
        service = new UserService(userRepository, encoder);
        when(userRepository.save(any(UserEntity.class))).thenAnswer(inv -> {
            UserEntity u = inv.getArgument(0);
            if (u.getId() == null) u.setId(UUID.randomUUID());
            if (u.getCreatedAt() == null) u.setCreatedAt(LocalDateTime.now());
            return u;
        });
    }

    private UserEntity user(String username, String password) {
        UserEntity u = new UserEntity();
        u.setId(UUID.randomUUID());
        u.setUsername(username);
        u.setFullName(username);
        u.setRole("USER");
        u.setActive(true);
        u.setPasswordHash(encoder.encode(password));
        when(userRepository.findById(u.getId())).thenReturn(Optional.of(u));
        when(userRepository.findByUsername(username)).thenReturn(Optional.of(u));
        return u;
    }

    private void assertStatus(Throwable t, HttpStatus expected) {
        assertThat(t).isInstanceOf(ResponseStatusException.class);
        assertThat(((ResponseStatusException) t).getStatusCode()).isEqualTo(expected);
    }

    @Test
    void creaElUsuarioActivoConLaContraseñaCifrada() {
        UserResponse response = service.create(new CreateUserRequest(" ana ", " Ana Pérez ", "Secreta123", "USER"));

        assertThat(response.username()).isEqualTo("ana");
        assertThat(response.fullName()).isEqualTo("Ana Pérez");
        assertThat(response.role()).isEqualTo("USER");
        assertThat(response.active()).isTrue();

        ArgumentCaptor<UserEntity> saved = ArgumentCaptor.forClass(UserEntity.class);
        verify(userRepository).save(saved.capture());
        assertThat(saved.getValue().getPasswordHash()).isNotEqualTo("Secreta123");
        assertThat(encoder.matches("Secreta123", saved.getValue().getPasswordHash())).isTrue();
    }

    @Test
    void noPermiteUsuariosDuplicados() {
        when(userRepository.existsByUsername("ana")).thenReturn(true);

        assertThatThrownBy(() -> service.create(new CreateUserRequest("ana", "Ana", "Secreta123", "USER")))
                .satisfies(t -> assertStatus(t, HttpStatus.CONFLICT));
        verify(userRepository, never()).save(any());
    }

    @Test
    void desactivaYReactivaAOtroUsuario() {
        UserEntity ana = user("ana", "x");

        assertThat(service.setActive(ana.getId(), false, "admin").active()).isFalse();
        assertThat(service.setActive(ana.getId(), true, "admin").active()).isTrue();
    }

    @Test
    void nadiePuedeDesactivarSuPropiaCuenta() {
        UserEntity admin = user("admin", "x");

        assertThatThrownBy(() -> service.setActive(admin.getId(), false, "admin"))
                .satisfies(t -> assertStatus(t, HttpStatus.BAD_REQUEST));
        assertThat(admin.isActive()).isTrue();
    }

    @Test
    void activarseASiMismoEsInocuo() {
        UserEntity admin = user("admin", "x");

        assertThat(service.setActive(admin.getId(), true, "admin").active()).isTrue();
    }

    @Test
    void desactivarUnUsuarioInexistenteDevuelve404() {
        UUID unknown = UUID.randomUUID();
        when(userRepository.findById(unknown)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.setActive(unknown, false, "admin"))
                .satisfies(t -> assertStatus(t, HttpStatus.NOT_FOUND));
    }

    @Test
    void restablecerLaContraseñaLaCifraDeNuevo() {
        UserEntity ana = user("ana", "vieja-clave-1");

        service.resetPassword(ana.getId(), "nueva-clave-2");

        assertThat(encoder.matches("nueva-clave-2", ana.getPasswordHash())).isTrue();
        assertThat(encoder.matches("vieja-clave-1", ana.getPasswordHash())).isFalse();
    }

    @Test
    void cambiarLaPropiaContraseñaExigeLaActual() {
        UserEntity ana = user("ana", "vieja-clave-1");

        assertThatThrownBy(() -> service.changeOwnPassword("ana", new ChangePasswordRequest("equivocada", "nueva-clave-2")))
                .satisfies(t -> assertStatus(t, HttpStatus.BAD_REQUEST)); // 400, no 401: no debe cerrar la sesión
        assertThat(encoder.matches("vieja-clave-1", ana.getPasswordHash())).isTrue();
    }

    @Test
    void laNuevaContraseñaDebeSerDistintaDeLaActual() {
        user("ana", "vieja-clave-1");

        assertThatThrownBy(() -> service.changeOwnPassword("ana", new ChangePasswordRequest("vieja-clave-1", "vieja-clave-1")))
                .satisfies(t -> assertStatus(t, HttpStatus.BAD_REQUEST));
    }

    @Test
    void cambiaLaPropiaContraseñaCuandoLaActualEsCorrecta() {
        UserEntity ana = user("ana", "vieja-clave-1");

        service.changeOwnPassword("ana", new ChangePasswordRequest("vieja-clave-1", "nueva-clave-2"));

        assertThat(encoder.matches("nueva-clave-2", ana.getPasswordHash())).isTrue();
    }

    // ---------- edición, último administrador y sesiones ----------

    private UserEntity admin(String username) {
        UserEntity u = user(username, "x");
        u.setRole("ADMIN");
        return u;
    }

    private UpdateUserRequest edit(String fullName, String role) {
        return new UpdateUserRequest(fullName, role);
    }

    @Test
    void editaElNombreYElRolDeOtroUsuario() {
        UserEntity ana = user("ana", "x");

        UserResponse response = service.update(ana.getId(), edit("  Ana María  ", "ADMIN"), "admin");

        assertThat(response.fullName()).isEqualTo("Ana María");
        assertThat(response.role()).isEqualTo("ADMIN");
        assertThat(ana.getUsername()).isEqualTo("ana"); // el nombre de usuario no cambia
    }

    @Test
    void nadiePuedeCambiarSuPropioRolPeroSiSuNombre() {
        UserEntity yo = admin("admin");

        assertThatThrownBy(() -> service.update(yo.getId(), edit("Admin", "USER"), "admin"))
                .satisfies(t -> assertStatus(t, HttpStatus.BAD_REQUEST));
        assertThat(yo.getRole()).isEqualTo("ADMIN");

        assertThat(service.update(yo.getId(), edit("Nuevo nombre", "ADMIN"), "admin").fullName()).isEqualTo("Nuevo nombre");
    }

    @Test
    void noSeLeQuitaElRolDeAdministradorAlUltimoAdministradorActivo() {
        UserEntity ultimo = admin("jefe");
        when(userRepository.countByRoleAndActive("ADMIN", true)).thenReturn(1L);

        assertThatThrownBy(() -> service.update(ultimo.getId(), edit("Jefe", "USER"), "otro"))
                .satisfies(t -> assertStatus(t, HttpStatus.CONFLICT));
        assertThat(ultimo.getRole()).isEqualTo("ADMIN");
    }

    @Test
    void sePuedeQuitarElRolDeAdministradorSiHayOtroAdministradorActivo() {
        UserEntity jefe = admin("jefe");
        when(userRepository.countByRoleAndActive("ADMIN", true)).thenReturn(2L);

        assertThat(service.update(jefe.getId(), edit("Jefe", "USER"), "otro").role()).isEqualTo("USER");
    }

    @Test
    void noSeDesactivaAlUltimoAdministradorActivo() {
        UserEntity ultimo = admin("jefe");
        when(userRepository.countByRoleAndActive("ADMIN", true)).thenReturn(1L);

        assertThatThrownBy(() -> service.setActive(ultimo.getId(), false, "otro"))
                .satisfies(t -> assertStatus(t, HttpStatus.CONFLICT));
        assertThat(ultimo.isActive()).isTrue();
    }

    @Test
    void sePuedeDesactivarUnAdministradorSiQuedaOtroActivo() {
        UserEntity jefe = admin("jefe");
        when(userRepository.countByRoleAndActive("ADMIN", true)).thenReturn(2L);

        assertThat(service.setActive(jefe.getId(), false, "otro").active()).isFalse();
    }

    @Test
    void laReglaDelUltimoAdministradorNoAfectaAUsuariosNormalesNiAAdministradoresInactivos() {
        UserEntity normal = user("ana", "x");
        UserEntity inactivo = admin("baja");
        inactivo.setActive(false);
        when(userRepository.countByRoleAndActive("ADMIN", true)).thenReturn(1L);

        assertThat(service.setActive(normal.getId(), false, "admin").active()).isFalse();
        assertThat(service.update(inactivo.getId(), edit("Baja", "USER"), "admin").role()).isEqualTo("USER");
    }

    @Test
    void editarUnUsuarioInexistenteDevuelve404() {
        UUID unknown = UUID.randomUUID();
        when(userRepository.findById(unknown)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.update(unknown, edit("X", "USER"), "admin"))
                .satisfies(t -> assertStatus(t, HttpStatus.NOT_FOUND));
    }

    @Test
    void restablecerOCambiarLaContraseñaRegistraElInstanteParaCerrarSesiones() {
        UserEntity ana = user("ana", "vieja-clave-1");
        assertThat(ana.getPasswordChangedAt()).isNull();

        LocalDateTime before = LocalDateTime.now().minusSeconds(1);
        service.resetPassword(ana.getId(), "nueva-clave-2");
        assertThat(ana.getPasswordChangedAt()).isAfter(before);

        ana.setPasswordChangedAt(null);
        service.changeOwnPassword("ana", new ChangePasswordRequest("nueva-clave-2", "otra-clave-3"));
        assertThat(ana.getPasswordChangedAt()).isAfter(before);
    }

    @Test
    void unCambioDeContraseñaFallidoNoRegistraElInstante() {
        UserEntity ana = user("ana", "vieja-clave-1");

        assertThatThrownBy(() -> service.changeOwnPassword("ana", new ChangePasswordRequest("equivocada", "nueva-clave-2")))
                .isInstanceOf(ResponseStatusException.class);

        assertThat(ana.getPasswordChangedAt()).isNull();
    }
}
