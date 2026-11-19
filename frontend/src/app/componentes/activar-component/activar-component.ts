import { Component, inject, signal, Signal } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { ActivarService } from '../../services/activar.service';

@Component({
  selector: 'app-activar-component',
  imports: [],
  templateUrl: './activar-component.html',
  styleUrl: './activar-component.scss',
})
export class ActivarComponent {
  private readonly route = inject(ActivatedRoute);
  private readonly activarService = inject(ActivarService);

  token : string | null = null;
  titulo = signal<string>("");
  seccion = 3;

  ngOnInit(){
    this.token = this.route.snapshot.queryParamMap.get('token');
    if (this.token) {
      this.activarCuenta(this.token);
    } else {
      console.error('No se encontró ningún token en la URL');
      this.titulo.set('TOKEN NO ENCONTRADO');
    }
  }

  activarCuenta(token:string){
    this.activarService.activarUsuario(token).subscribe({
      next: (data) =>{
        console.log(data);

        this.seccion = data;
        switch(data){
          case 0 :
            this.titulo.set("TOKEN NO REGISTRADO");
            break;
          case 1 :
            this.titulo.set("USUARIO ACTIVO");
            break;
          case 2 :
            this.titulo.set("USUARIO ACTIVADO");
            break;
          default:
            this.titulo.set("TOKEN NO ENCONTRADO");
        }
        console.log(this.titulo+"***");
      },
      error: (err) =>{
        console.log(err);
      }
      
    });
  }

}
