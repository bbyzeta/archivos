import { Routes } from '@angular/router';
import { LoginComponent } from './componentes/login-component/login-component';
import { HomeComponent } from './componentes/home-component/home-component';
import { FolderComponent } from './componentes/folder-component/folder-component';
import { authGuard } from './guard/auth-guard';

export const routes: Routes = [
  { path: 'login', component: LoginComponent, title: 'Iniciar sesión' },
  { path: 'home', component: HomeComponent, canActivate: [authGuard], title: 'Inicio' },
  { path: 'folders/:id', component: FolderComponent, canActivate: [authGuard], title: 'Carpeta' },
  { path: '', redirectTo: 'home', pathMatch: 'full' },
  { path: '**', redirectTo: 'home' }
];
