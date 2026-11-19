package csg.gob.mx.convocatoria.controller;


import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import csg.gob.mx.convocatoria.dto.PersonaDTO;
import csg.gob.mx.convocatoria.service.PersonasService;

@RestController
@RequestMapping("/api/personas")
public class PersonasController {

    private final PersonasService personasService;

    public PersonasController(PersonasService personasService) {
        this.personasService = personasService;
    }

    @GetMapping
    public ResponseEntity<List<PersonaDTO>> getAll() {
        List<PersonaDTO> personas = personasService.findAll();
        return ResponseEntity.ok(personas);
    }

    @GetMapping("/{id}")
    public ResponseEntity<PersonaDTO> getById(@PathVariable Integer id) {
        PersonaDTO persona = personasService.findById(id);
        return ResponseEntity.ok(persona);
    }

    @PostMapping
    public ResponseEntity<PersonaDTO> create(@RequestBody PersonaDTO personasDTO) {
        PersonaDTO creada = personasService.save(personasDTO);
        return new ResponseEntity<>(creada, HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    public ResponseEntity<PersonaDTO> update(@PathVariable Integer id, @RequestBody PersonaDTO personasDTO) {
        PersonaDTO actualizada = personasService.update(id, personasDTO);
        return ResponseEntity.ok(actualizada);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Integer id) {
        personasService.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}