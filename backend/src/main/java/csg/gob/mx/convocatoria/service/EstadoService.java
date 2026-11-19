package csg.gob.mx.convocatoria.service;


import csg.gob.mx.convocatoria.entity.EstadoEntity;
import csg.gob.mx.convocatoria.repository.EstadoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class EstadoService {

    @Autowired
    private EstadoRepository estadoRepository;

    @Transactional(readOnly = true)
    public List<EstadoEntity> obtenerTodosLosEstados() {
        return estadoRepository.findAllByOrderByNombreAsc();
    }
}