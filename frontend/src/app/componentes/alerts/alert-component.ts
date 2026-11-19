import {Injectable } from '@angular/core';
import Swal, { SweetAlertIcon, SweetAlertResult } from 'sweetalert2';

@Injectable({
  providedIn: 'root'
})
export class AlertaService {

  mostrarAlerta(titulo:string, mensaje:string, icono: SweetAlertIcon = 'info', txtConfirm:string, txtColorConfirm:string): void {
    Swal.fire({
      title: titulo,
      text: mensaje,
      icon: icono,
      confirmButtonText: txtConfirm,
      confirmButtonColor: txtColorConfirm,
      customClass: {
          title: 'font-patria',
          htmlContainer: 'font-patria'
        }
    });
  }

  mostrarAlertaError(mensaje: string): void {
    Swal.fire({
      title: 'Error de validación',
      text: mensaje,
      icon: 'error',
      confirmButtonText: 'Entendido',
      confirmButtonColor: '#9b2247',
      customClass: {
          title: 'font-patria',
          htmlContainer: 'font-patria'
        }
    });
  }

  mostrarLoader(mensaje: string = 'Procesando información...'): void {
    Swal.fire({
      title: mensaje,
      allowOutsideClick: false,
      allowEscapeKey: false,
      customClass: {
          title: 'font-patria',
          htmlContainer: 'font-patria'
      },
      didOpen: () => {
        Swal.showLoading();
      }
    });
  }

  cerrarLoader(): void {
    Swal.close();
  }

  async confirmarAccion(
    titulo: string = '¿Estás seguro?',
    mensaje: string = 'Acción.',
    textoBotonConfirmar: string = 'Sí, continuar',
    textoBotonCancelar: string = 'Cancelar',
  ): Promise<boolean> {
    const respuesta: SweetAlertResult = await Swal.fire({
      title: titulo,
      text: mensaje,
      icon: 'question',
      showCancelButton: true,
      confirmButtonColor: '#1e5b4f', 
      cancelButtonColor: '#6c757d',  
      confirmButtonText: textoBotonConfirmar,
      cancelButtonText: textoBotonCancelar,
      reverseButtons: true,
      customClass: {
          title: 'font-patria',
          htmlContainer: 'font-patria'
        }
    });

    return respuesta.isConfirmed;
  }

  mostrarAlertaConTimer(
    titulo: string,
    mensaje: string,
    tiempoMs: number = 2000,
    icono: SweetAlertIcon = 'success'
  ): void {
    Swal.fire({
      title: titulo,
      text: mensaje,
      icon: icono,
      timer: tiempoMs,
      timerProgressBar: true, 
      showConfirmButton: false, 
      allowOutsideClick: false,
      customClass: {
          title: 'font-patria',
          htmlContainer: 'font-patria'
        }
    });
  }

  async mostrarPromptInput(
    titulo: string,
    mensaje: string,
    valorInicial: string = '',
    placeholder: string = 'Ingresa aquí la observación...'
  ): Promise<string | null> {
    const respuesta: SweetAlertResult = await Swal.fire({
      title: titulo,
      text: mensaje,
      input: 'textarea',
      inputValue: valorInicial,
      inputPlaceholder: placeholder,
      showCancelButton: true,
      confirmButtonText: 'Guardar',
      cancelButtonText: 'Cancelar',
      confirmButtonColor: '#1e5b4f',
      cancelButtonColor: '#6c757d',
      reverseButtons: true,
      customClass: {
        title: 'font-patria',
        htmlContainer: 'font-patria',
        input: 'font-patria input-foco-verde'
      },
      inputValidator: (value) => {
        if (!value || !value.trim()) {
          return 'Debes redactar una observación para el rechazo.';
        }
        return null;
      }
    });

    if (respuesta.isConfirmed && respuesta.value) {
      return respuesta.value.trim();
    }

    return null; 
  }
    
}