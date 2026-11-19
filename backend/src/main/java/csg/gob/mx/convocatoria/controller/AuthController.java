package csg.gob.mx.convocatoria.controller;


import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import csg.gob.mx.convocatoria.dto.UsuarioDTO;
import csg.gob.mx.convocatoria.entity.RolEntity;
import csg.gob.mx.convocatoria.entity.UsuarioEntity;
import csg.gob.mx.convocatoria.repository.UsuarioRepository;
import csg.gob.mx.convocatoria.security.JwtProvider;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final JwtProvider jwtProvider;
    private final UsuarioRepository usuarioRepository;

    public AuthController(AuthenticationManager authenticationManager, JwtProvider jwtProvider, UsuarioRepository usuarioRepository) {
        this.authenticationManager = authenticationManager;
        this.jwtProvider = jwtProvider;
        this.usuarioRepository = usuarioRepository;
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody UsuarioDTO loginRequest) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(loginRequest.username(), loginRequest.password())
        );

        UserDetails userDetails = (UserDetails) authentication.getPrincipal();
        String jwt = jwtProvider.generateToken(userDetails);
        
        UsuarioEntity usuario = this.usuarioRepository.findByUsername(userDetails.getUsername()).get();
        String rolPrincipal = usuario.getRoles().stream()
                .map(RolEntity::getNombre)
                .findFirst()
                .orElse("ROLE_USER");

        return ResponseEntity.ok(Map.of("token", jwt, "userName", userDetails.getUsername(), "idUsuario", usuario.getIdUsuario(), "rol", rolPrincipal));
    }
}

