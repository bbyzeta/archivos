import { Routes } from '@angular/router';
import { LoginComponent } from './componentes/login-component/login-component';
import { RegistroComponent } from './componentes/registro-component/registro-component';
import { AdminConvocatoriaComponent } from './componentes/admin-convocatoria-component/admin-convocatoria-component';
import { authGuard } from './guard/auth-guard';
import { PostulanteConvocatoriaComponent } from './componentes/postulante-convocatoria-component/postulante-convocatoria-component';
import { PostulacionExpedienteComponent } from './componentes/postulacion-expediente-component/postulacion-expediente-component';
import { AuditorRevisionExpedienteComponent } from './componentes/auditor-revision-expediente-component/auditor-revision-expediente-component';
import { AuditorBandejaExpedientesComponent } from './componentes/auditor-bandeja-expedientes-component/auditor-bandeja-expedientes-component';
import { JuradoBandejaExpedientesComponent } from './componentes/jurado-bandeja-expedientes-component/jurado-bandeja-expedientes-component';
import { PostulanteConvocatoriasComponent } from './componentes/postulante-convocatorias-component/postulante-convocatorias-component';
import { ActivarComponent } from './componentes/activar-component/activar-component';
import { RecuperarPasswordComponent } from './componentes/recuperar-pass-word-component/recuperar-pass-word-component';
import { JuradoExpedienteComponent } from './componentes/jurado-expediente-component/jurado-expediente-component';

export const routes: Routes = [

  {
    path: 'login',
    component: LoginComponent,
    title: 'Premios CSG'
  },
  {
    path: 'registro',
    component: RegistroComponent,
    title: 'Registro'
  },
  {
    path: 'activar',
    component: ActivarComponent,
    title: 'Activar'
  },
  {
    path: 'recuperar',
    component: RecuperarPasswordComponent,
    title: 'Recuperar'
  },
  {
    path: 'convocatoria-admin',
    component: AdminConvocatoriaComponent,
    canActivate: [authGuard],
    title: 'Administrador'
  },
  {
    path: 'postulante/convocatoria-vigente/:id',
    component: PostulanteConvocatoriaComponent,
    canActivate: [authGuard],
    title: 'Postulante'
  },
  {
    path: 'postulante/convocatorias',
    component: PostulanteConvocatoriasComponent,
    canActivate: [authGuard],
    title: 'Convocatorias'
  },
  {
    path: 'postulante/expediente/:id',
    component: PostulacionExpedienteComponent,
    canActivate: [authGuard],
    title: 'Convocatoria'
  },
  {
    path: 'auditor/postulantes',
    component: AuditorBandejaExpedientesComponent,
    canActivate: [authGuard],
    title: 'Auditor'
  },
  {
    path: 'jurado/postulantes',
    component: JuradoBandejaExpedientesComponent,
    canActivate: [authGuard],
    title: 'Jurado'
  },
  {
    path: 'auditor/expediente/:idExpediente',
    component: AuditorRevisionExpedienteComponent,
    canActivate: [authGuard],
    title: 'Auditor'
  },
  {
    path: 'jurado/expediente/:idExpediente',
    component: JuradoExpedienteComponent,
    canActivate: [authGuard],
    title: 'Jurado'
  },
  {
    path: '', 
    redirectTo: 'login', 
    pathMatch: 'full' 
  },
  {
    path: '**', 
    redirectTo: 'login' 
  },
];
