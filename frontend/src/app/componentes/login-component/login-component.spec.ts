import { ComponentFixture, TestBed } from '@angular/core/testing';
import { Router } from '@angular/router';
import { Subject, of, throwError } from 'rxjs';
import { vi } from 'vitest';
import { LoginResponse } from '../../models/login.model';
import { AuthService } from '../../services/auth.service';
import { AlertaService } from '../alerts/alert-component';
import { LoginComponent } from './login-component';

const SESSION: LoginResponse = { token: 't', userId: 'u1', username: 'ana', fullName: 'Ana', role: 'USER' };

describe('LoginComponent', () => {
  let fixture: ComponentFixture<LoginComponent>;
  let component: LoginComponent;
  let login: ReturnType<typeof vi.fn>;
  let alerts: { mostrarLoader: ReturnType<typeof vi.fn>; cerrarLoader: ReturnType<typeof vi.fn>; mostrarAlerta: ReturnType<typeof vi.fn> };
  let navigate: ReturnType<typeof vi.fn>;

  beforeEach(async () => {
    login = vi.fn();
    navigate = vi.fn().mockResolvedValue(true);
    alerts = { mostrarLoader: vi.fn(), cerrarLoader: vi.fn(), mostrarAlerta: vi.fn() };

    await TestBed.configureTestingModule({
      imports: [LoginComponent],
      providers: [
        { provide: AuthService, useValue: { login } },
        { provide: AlertaService, useValue: alerts },
        { provide: Router, useValue: { navigate } }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(LoginComponent);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  function fill(username = 'ana', password = 'Secreta123'): void {
    component.loginForm.setValue({ username, password });
  }

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('no envía el formulario inválido y marca los campos como tocados', () => {
    component.onSubmit();

    expect(login).not.toHaveBeenCalled();
    expect(component.submitting()).toBe(false);
    expect(component.esCampoInvalido('username')).toBe(true);
    expect(component.esCampoInvalido('password')).toBe(true);
  });

  it('con credenciales válidas inicia sesión y redirige a /home', () => {
    login.mockReturnValue(of(SESSION));
    fill();

    component.onSubmit();

    expect(login).toHaveBeenCalledWith({ username: 'ana', password: 'Secreta123' });
    expect(navigate).toHaveBeenCalledWith(['/home']);
    expect(alerts.cerrarLoader).toHaveBeenCalled();
    expect(component.submitting()).toBe(false);
  });

  it('mientras se envía bloquea el botón y evita un segundo envío', () => {
    const pending = new Subject<LoginResponse>();
    login.mockReturnValue(pending);
    fill();

    component.onSubmit();
    component.onSubmit(); // doble clic / doble Enter
    fixture.detectChanges();

    expect(component.submitting()).toBe(true);
    expect(login).toHaveBeenCalledTimes(1);
    const button = fixture.nativeElement.querySelector('button[type="submit"]') as HTMLButtonElement;
    expect(button.disabled).toBe(true);

    pending.next(SESSION);
    pending.complete();
    fixture.detectChanges();

    expect(component.submitting()).toBe(false);
    expect(button.disabled).toBe(false);
  });

  it('con un 401 muestra el error de credenciales, no redirige y permite reintentar', () => {
    login.mockReturnValue(throwError(() => ({ status: 401 })));
    fill('ana', 'incorrecta');

    component.onSubmit();

    expect(alerts.mostrarAlerta).toHaveBeenCalledWith(
      '¡Atención!', 'Usuario o contraseña incorrectos.', 'warning', 'Aceptar', '#13322b');
    expect(navigate).not.toHaveBeenCalled();
    expect(component.submitting()).toBe(false);
  });

  it('sin conexión con el servidor (status 0) lo indica con otro mensaje', () => {
    login.mockReturnValue(throwError(() => ({ status: 0 })));
    fill();

    component.onSubmit();

    expect(alerts.mostrarAlerta).toHaveBeenCalledWith(
      '¡Atención!', 'No se pudo conectar con el servidor.', 'warning', 'Aceptar', '#13322b');
    expect(component.submitting()).toBe(false);
  });

  it('el botón del ojo alterna entre ocultar y mostrar la contraseña', () => {
    const input = fixture.nativeElement.querySelector('#password') as HTMLInputElement;
    expect(component.showPassword()).toBe(false);
    expect(input.type).toBe('password');

    component.toggleShowPassword();
    fixture.detectChanges();
    expect(component.showPassword()).toBe(true);
    expect(input.type).toBe('text');

    component.toggleShowPassword();
    fixture.detectChanges();
    expect(input.type).toBe('password');
  });
});
