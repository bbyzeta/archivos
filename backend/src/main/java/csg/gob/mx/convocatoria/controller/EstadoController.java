package csg.gob.mx.convocatoria.controller;


import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import csg.gob.mx.convocatoria.entity.EstadoEntity;
import csg.gob.mx.convocatoria.service.EstadoService;

@RestController
@RequestMapping("/api/estados")
public class EstadoController {

    @Autowired
    private EstadoService estadoService;

    @GetMapping
    public ResponseEntity<List<EstadoEntity>> listarEstados() {
        List<EstadoEntity> estados = estadoService.obtenerTodosLosEstados();
        return ResponseEntity.ok(estados);
    }
}