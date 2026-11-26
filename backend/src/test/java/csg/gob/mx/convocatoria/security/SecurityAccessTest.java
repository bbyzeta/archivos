package csg.gob.mx.convocatoria.security;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.net.URI;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.server.ResponseStatusException;

import csg.gob.mx.convocatoria.dto.PublicFolderResponse;
import csg.gob.mx.convocatoria.dto.UserResponse;
import csg.gob.mx.convocatoria.entity.UserEntity;
import csg.gob.mx.convocatoria.repository.UserRepository;
import csg.gob.mx.convocatoria.service.FolderDirectoryService;
import csg.gob.mx.convocatoria.service.FolderFileService;
import csg.gob.mx.convocatoria.service.FolderFileService.PublicFile;
import csg.gob.mx.convocatoria.service.FolderService;
import csg.gob.mx.convocatoria.service.UserService;

/** Control de acceso: anónimo, USER y ADMIN contra la cadena de seguridad real con JWT real. */
@WebMvcTest
@Import({ SecurityConfig.class, JwtProvider.class, JwtAuthenticationFilter.class })
@TestPropertySource(properties = {
        "security.jwt.secret-key=dGVzdC1zZWNyZXQta2V5LXRlc3Qtc2VjcmV0LWtleS0xMjM0NTY3ODkw",
        "security.jwt.expiration=3600000",
        "app.cors.allowed-origins=http://localhost:4200"
})
class SecurityAccessTest {

    private static final String PASSWORD = "Secreta123";

    @Autowired
    MockMvc mvc;
    @Autowired
    JwtProvider jwtProvider;

    @MockitoBean
    CustomUserDetailsService userDetailsService;
    @MockitoBean
    UserRepository userRepository;
    @MockitoBean
    FolderService folderService;
    @MockitoBean
    FolderFileService folderFileService;
    @MockitoBean
    FolderDirectoryService folderDirectoryService;
    @MockitoBean
    UserService userService;

    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

    @BeforeEach
    void setUp() {
        when(userDetailsService.loadUserByUsername("admin")).thenReturn(account("admin", "ADMIN", true));
        when(userDetailsService.loadUserByUsername("ana")).thenReturn(account("ana", "USER", true));
        when(userDetailsService.loadUserByUsername("baja")).thenReturn(account("baja", "USER", false));
    }

    private UserDetails account(String username, String role, boolean enabled) {
        return User.withUsername(username).password(encoder.encode(PASSWORD)).authorities("ROLE_" + role)
                .disabled(!enabled).build();
    }

    private String bearer(String username) {
        return "Bearer " + jwtProvider.generateToken(userDetailsService.loadUserByUsername(username));
    }

    private static final String NEW_USER = """
            {"username":"nuevo","fullName":"Nuevo","password":"Secreta123","role":"USER"}""";
    private static final String CHANGE_PASSWORD = """
            {"currentPassword":"Secreta123","newPassword":"Otra-clave-456"}""";

    // ---------- anónimo ----------

    @Test
    void sinTokenLasRutasAdminDevuelven401() throws Exception {
        mvc.perform(get("/api/admin/folders")).andExpect(status().isUnauthorized());
        mvc.perform(post("/api/admin/folders").contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"x\"}"))
                .andExpect(status().isUnauthorized());
        mvc.perform(get("/api/admin/users")).andExpect(status().isUnauthorized());
        mvc.perform(put("/api/admin/users/me/password").contentType(MediaType.APPLICATION_JSON).content(CHANGE_PASSWORD))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void unTokenInvalidoOManipuladoDevuelve401() throws Exception {
        mvc.perform(get("/api/admin/folders").header("Authorization", "Bearer no-es-un-jwt"))
                .andExpect(status().isUnauthorized());

        String tampered = bearer("ana").replaceAll(".$", "x");
        mvc.perform(get("/api/admin/folders").header("Authorization", tampered)).andExpect(status().isUnauthorized());
    }

    @Test
    void unTokenExpiradoDevuelve401() throws Exception {
        JwtProvider expired = new JwtProvider("dGVzdC1zZWNyZXQta2V5LXRlc3Qtc2VjcmV0LWtleS0xMjM0NTY3ODkw", -1000);
        String token = expired.generateToken(userDetailsService.loadUserByUsername("admin"));

        mvc.perform(get("/api/admin/folders").header("Authorization", "Bearer " + token))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void unUsuarioDesactivadoPierdeElAccesoAunConTokenVigente() throws Exception {
        mvc.perform(get("/api/admin/folders").header("Authorization", bearer("baja")))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void lasRutasNoDeclaradasEstanDenegadas() throws Exception {
        mvc.perform(get("/api/otra-cosa")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/otra-cosa").header("Authorization", bearer("admin"))).andExpect(status().isForbidden());
        mvc.perform(get("/actuator/env").header("Authorization", bearer("admin"))).andExpect(status().isForbidden());
    }

    // ---------- USER ----------

    @Test
    void unUsuarioNormalPuedeUsarCarpetasYArchivos() throws Exception {
        when(folderService.findAll()).thenReturn(List.of());

        mvc.perform(get("/api/admin/folders").header("Authorization", bearer("ana"))).andExpect(status().isOk());
        mvc.perform(get("/api/admin/folders/" + UUID.randomUUID() + "/files").header("Authorization", bearer("ana")))
                .andExpect(status().isOk());
    }

    @Test
    void unUsuarioNormalNoPuedeGestionarUsuarios() throws Exception {
        String auth = bearer("ana");
        UUID id = UUID.randomUUID();

        mvc.perform(get("/api/admin/users").header("Authorization", auth)).andExpect(status().isForbidden());
        mvc.perform(post("/api/admin/users").header("Authorization", auth)
                .contentType(MediaType.APPLICATION_JSON).content(NEW_USER)).andExpect(status().isForbidden());
        mvc.perform(patch("/api/admin/users/" + id + "/active").header("Authorization", auth)
                .contentType(MediaType.APPLICATION_JSON).content("{\"active\":false}")).andExpect(status().isForbidden());
        mvc.perform(put("/api/admin/users/" + id + "/password").header("Authorization", auth)
                .contentType(MediaType.APPLICATION_JSON).content("{\"newPassword\":\"Otra-clave-456\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void cualquierUsuarioAutenticadoPuedeCambiarSuPropiaContraseña() throws Exception {
        doNothing().when(userService).changeOwnPassword(any(), any());

        mvc.perform(put("/api/admin/users/me/password").header("Authorization", bearer("ana"))
                .contentType(MediaType.APPLICATION_JSON).content(CHANGE_PASSWORD)).andExpect(status().isNoContent());
        verify(userService).changeOwnPassword(any(), any());
    }

    @Test
    void laContraseñaNuevaMuyCortaSeRechazaConValidacion() throws Exception {
        mvc.perform(put("/api/admin/users/me/password").header("Authorization", bearer("ana"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"currentPassword\":\"Secreta123\",\"newPassword\":\"corta\"}"))
                .andExpect(status().isBadRequest());
    }

    // ---------- ADMIN ----------

    @Test
    void unAdministradorPuedeGestionarUsuarios() throws Exception {
        UserResponse created = new UserResponse(UUID.randomUUID(), "nuevo", "Nuevo", "USER", true, LocalDateTime.now());
        when(userService.findAll()).thenReturn(List.of(created));
        when(userService.create(any())).thenReturn(created);

        String auth = bearer("admin");
        mvc.perform(get("/api/admin/users").header("Authorization", auth))
                .andExpect(status().isOk()).andExpect(jsonPath("$[0].username").value("nuevo"));
        mvc.perform(post("/api/admin/users").header("Authorization", auth)
                .contentType(MediaType.APPLICATION_JSON).content(NEW_USER)).andExpect(status().isCreated());
    }

    @Test
    void crearUsuarioValidaRolYContraseña() throws Exception {
        String auth = bearer("admin");

        mvc.perform(post("/api/admin/users").header("Authorization", auth).contentType(MediaType.APPLICATION_JSON)
                .content("{\"username\":\"nuevo\",\"fullName\":\"N\",\"password\":\"Secreta123\",\"role\":\"ROOT\"}"))
                .andExpect(status().isBadRequest());
        mvc.perform(post("/api/admin/users").header("Authorization", auth).contentType(MediaType.APPLICATION_JSON)
                .content("{\"username\":\"nuevo\",\"fullName\":\"N\",\"password\":\"corta\",\"role\":\"USER\"}"))
                .andExpect(status().isBadRequest());
        mvc.perform(post("/api/admin/users").header("Authorization", auth).contentType(MediaType.APPLICATION_JSON)
                .content("{\"username\":\"../x\",\"fullName\":\"N\",\"password\":\"Secreta123\",\"role\":\"USER\"}"))
                .andExpect(status().isBadRequest());
    }

    // ---------- público ----------

    @Test
    void laVistaPublicaNoRequiereAutenticacion() throws Exception {
        when(folderFileService.getPublicFolder("abc123"))
                .thenReturn(new PublicFolderResponse("Docs", "abc123", null, List.of(), List.of()));

        mvc.perform(get("/api/public/abc123")).andExpect(status().isOk()).andExpect(jsonPath("$.name").value("Docs"));
    }

    @Test
    void laDescargaPublicaDecodificaLaRutaYFuerzaDescargaDeTiposRiesgosos() throws Exception {
        when(folderFileService.getPublicFile("abc123", "Mis docs/informe.html"))
                .thenReturn(new PublicFile(new ByteArrayResource("<script>1</script>".getBytes()), "informe.html", "text/html"));

        mvc.perform(get(URI.create("/api/public/abc123/file/Mis%20docs/informe.html")))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith("text/html"))
                .andExpect(header().string("Content-Disposition", org.hamcrest.Matchers.startsWith("attachment")));
    }

    @Test
    void lasImagenesYPdfSeMuestranEnLineaYDownloadFuerzaDescarga() throws Exception {
        when(folderFileService.getPublicFile("abc123", "foto.png"))
                .thenReturn(new PublicFile(new ByteArrayResource(new byte[] { 1, 2, 3 }), "foto.png", "image/png"));

        mvc.perform(get("/api/public/abc123/file/foto.png"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition", org.hamcrest.Matchers.startsWith("inline")));
        mvc.perform(get("/api/public/abc123/file/foto.png?download=true"))
                .andExpect(header().string("Content-Disposition", org.hamcrest.Matchers.startsWith("attachment")));
    }

    // ---------- login ----------

    @Test
    void loginCorrectoDevuelveTokenYRol() throws Exception {
        UserEntity entity = new UserEntity();
        entity.setId(UUID.randomUUID());
        entity.setUsername("admin");
        entity.setFullName("Administrador");
        entity.setRole("ADMIN");
        when(userRepository.findByUsername("admin")).thenReturn(Optional.of(entity));

        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content("{\"username\":\"admin\",\"password\":\"" + PASSWORD + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(jsonPath("$.role").value("ADMIN"));
    }

    @Test
    void loginConCredencialesIncorrectasDevuelve401() throws Exception {
        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content("{\"username\":\"admin\",\"password\":\"incorrecta\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void loginDeUnUsuarioDesactivadoDevuelve401() throws Exception {
        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content("{\"username\":\"baja\",\"password\":\"" + PASSWORD + "\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void loginSinCamposDevuelve400() throws Exception {
        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void eliminarCarpetasRequiereAutenticacion() throws Exception {
        mvc.perform(delete("/api/admin/folders/" + UUID.randomUUID())).andExpect(status().isUnauthorized());
        mvc.perform(delete("/api/admin/folders/" + UUID.randomUUID()).header("Authorization", bearer("ana")))
                .andExpect(status().isNoContent());
    }

    // ---------- color ----------

    @Test
    void elColorSeValidaYCualquierUsuarioAutenticadoPuedeAsignarlo() throws Exception {
        UUID id = UUID.randomUUID();
        String auth = bearer("ana");

        mvc.perform(patch("/api/admin/folders/" + id + "/color").header("Authorization", auth)
                .contentType(MediaType.APPLICATION_JSON).content("{\"color\":\"#9b2247\"}")).andExpect(status().isOk());
        mvc.perform(patch("/api/admin/folders/" + id + "/color").header("Authorization", auth)
                .contentType(MediaType.APPLICATION_JSON).content("{\"color\":null}")).andExpect(status().isOk());
        for (String invalid : new String[] { "rojo", "#fff", "#ggggg0", "red;background:url(x)", "#9b2247ff" }) {
            mvc.perform(patch("/api/admin/folders/" + id + "/color").header("Authorization", auth)
                    .contentType(MediaType.APPLICATION_JSON).content("{\"color\":\"" + invalid + "\"}"))
                    .andExpect(status().isBadRequest());
        }
        mvc.perform(patch("/api/admin/folders/" + id + "/color").contentType(MediaType.APPLICATION_JSON)
                .content("{\"color\":\"#9b2247\"}")).andExpect(status().isUnauthorized());
    }

    @Test
    void elColorDeSubcarpetaExigeRutaYColorValido() throws Exception {
        UUID id = UUID.randomUUID();
        String auth = bearer("ana");

        mvc.perform(put("/api/admin/folders/" + id + "/directories/color").header("Authorization", auth)
                .contentType(MediaType.APPLICATION_JSON).content("{\"path\":\"Docs\",\"color\":\"#9b2247\"}"))
                .andExpect(status().isOk());
        mvc.perform(put("/api/admin/folders/" + id + "/directories/color").header("Authorization", auth)
                .contentType(MediaType.APPLICATION_JSON).content("{\"path\":\"\",\"color\":\"#9b2247\"}"))
                .andExpect(status().isBadRequest());
        mvc.perform(put("/api/admin/folders/" + id + "/directories/color").header("Authorization", auth)
                .contentType(MediaType.APPLICATION_JSON).content("{\"path\":\"Docs\",\"color\":\"azul\"}"))
                .andExpect(status().isBadRequest());
    }

    // ---------- edición de usuarios y cierre de sesiones ----------

    @Test
    void soloUnAdministradorPuedeEditarUsuarios() throws Exception {
        UUID id = UUID.randomUUID();
        String body = "{\"fullName\":\"Ana\",\"role\":\"USER\"}";
        when(userService.update(any(), any(), any()))
                .thenReturn(new UserResponse(id, "ana", "Ana", "USER", true, LocalDateTime.now()));

        mvc.perform(patch("/api/admin/users/" + id).header("Authorization", bearer("ana"))
                .contentType(MediaType.APPLICATION_JSON).content(body)).andExpect(status().isForbidden());
        mvc.perform(patch("/api/admin/users/" + id).contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isUnauthorized());
        mvc.perform(patch("/api/admin/users/" + id).header("Authorization", bearer("admin"))
                .contentType(MediaType.APPLICATION_JSON).content(body)).andExpect(status().isOk());
    }

    @Test
    void laEdicionValidaElRolYElNombre() throws Exception {
        String auth = bearer("admin");
        String url = "/api/admin/users/" + UUID.randomUUID();

        mvc.perform(patch(url).header("Authorization", auth).contentType(MediaType.APPLICATION_JSON)
                .content("{\"fullName\":\"Ana\",\"role\":\"ROOT\"}")).andExpect(status().isBadRequest());
        mvc.perform(patch(url).header("Authorization", auth).contentType(MediaType.APPLICATION_JSON)
                .content("{\"fullName\":\"  \",\"role\":\"USER\"}")).andExpect(status().isBadRequest());
    }

    @Test
    void unTokenEmitidoAntesDelCambioDeContraseñaDejaDeSerValido() throws Exception {
        String token = bearer("ana"); // emitido ahora
        // La contraseña "cambió" una hora después de emitirse el token
        when(userDetailsService.loadUserByUsername("ana")).thenReturn(
                new AppUserDetails("ana", encoder.encode(PASSWORD), true,
                        List.of(new SimpleGrantedAuthority("ROLE_USER")), Instant.now().plusSeconds(3600)));

        mvc.perform(get("/api/admin/folders").header("Authorization", token)).andExpect(status().isUnauthorized());
    }

    @Test
    void unTokenEmitidoDespuesDelCambioDeContraseñaSigueSiendoValido() throws Exception {
        when(userDetailsService.loadUserByUsername("ana")).thenReturn(
                new AppUserDetails("ana", encoder.encode(PASSWORD), true,
                        List.of(new SimpleGrantedAuthority("ROLE_USER")), Instant.now().minusSeconds(3600)));
        when(folderService.findAll()).thenReturn(List.of());

        mvc.perform(get("/api/admin/folders").header("Authorization", bearer("ana"))).andExpect(status().isOk());
    }

    @Test
    void unUsuarioSinCambiosDeContraseñaConservaSusSesiones() throws Exception {
        when(userDetailsService.loadUserByUsername("ana")).thenReturn(
                new AppUserDetails("ana", encoder.encode(PASSWORD), true,
                        List.of(new SimpleGrantedAuthority("ROLE_USER")), null));
        when(folderService.findAll()).thenReturn(List.of());

        mvc.perform(get("/api/admin/folders").header("Authorization", bearer("ana"))).andExpect(status().isOk());
    }

    @Test
    void losErroresDeNegocioDevuelvenElMotivoEnElCuerpo() throws Exception {
        doThrow(new ResponseStatusException(HttpStatus.BAD_REQUEST, "La contraseña actual es incorrecta."))
                .when(userService).changeOwnPassword(any(), any());
        when(userService.update(any(), any(), any()))
                .thenThrow(new ResponseStatusException(HttpStatus.CONFLICT, "Debe existir al menos un administrador activo."));

        mvc.perform(put("/api/admin/users/me/password").header("Authorization", bearer("ana"))
                .contentType(MediaType.APPLICATION_JSON).content(CHANGE_PASSWORD))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("La contraseña actual es incorrecta."));

        mvc.perform(patch("/api/admin/users/" + UUID.randomUUID()).header("Authorization", bearer("admin"))
                .contentType(MediaType.APPLICATION_JSON).content("{\"fullName\":\"Jefe\",\"role\":\"USER\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Debe existir al menos un administrador activo."));
    }
}
