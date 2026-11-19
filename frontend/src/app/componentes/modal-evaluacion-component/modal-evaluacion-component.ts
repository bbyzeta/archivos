import { Component, Input, Output, EventEmitter, signal, computed, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { CriterioEvaluacion } from '../../models/jurado.model';

@Component({
  selector: 'app-modal-evaluacion',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './modal-evaluacion-component.html',
  styleUrls: ['./modal-evaluacion-component.scss']
})
export class ModalEvaluacionComponent  {
  @Input({ required: true }) nombrePostulante: string = '';
  @Input({ required: true }) premio: string = '';
  @Input() mostrarModal: boolean = false;

  @Output() alGuardar = new EventEmitter<CriterioEvaluacion[]>();
  @Output() alCancelar = new EventEmitter<void>();

  criterios = signal<CriterioEvaluacion[]>([]);

  @Input({ required: true }) set criteriosIniciales(val: CriterioEvaluacion[] | null) {
    if (val && val.length > 0) {
      this.criterios.set(JSON.parse(JSON.stringify(val)));
    }
  }


  totalPorcentajeAsignado = computed(() => {
    return this.criterios().reduce((acc, item) => {
      return acc + (item.porcentajeAsignado ?? 0);
    }, 0);
  });

  formularioValido = computed(() => {
    const lista = this.criterios();
    if (lista.length === 0) return false;
    
    return lista.every(c => 
      c.argumentos !== undefined && 
      c.argumentos.trim() !== ''
    );
  });

  onArgumentoChange(): void {
    this.criterios.set([...this.criterios()]);
  }
  guardar() {
    if (this.formularioValido()) {
      this.alGuardar.emit(this.criterios());
    }
  }

  cancelar() {
    this.alCancelar.emit();
  }
}