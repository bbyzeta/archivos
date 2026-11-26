import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { of, throwError } from 'rxjs';
import { vi } from 'vitest';
import { User } from '../../models/user.model';
import { AuthService } from '../../services/auth.service';
import { UserService } from '../../services/user.service';
import { AlertaService } from '../alerts/alert-component';
import { UsersComponent } from './users-component';

function user(id: string, username: string, fullName: string, role: 'ADMIN' | 'USER', active: boolean, createdAt: string): User {
  return { id, username, fullName, role, active, createdAt };
}

const USERS: User[] = [
  user('1', 'admin', 'Administrador', 'ADMIN', true, '2026-01-01T10:00:00'),
  user('2', 'ana', 'Ana Pérez', 'USER', true, '2026-02-01T10:00:00'),
  user('3', 'luis', 'Luis Gómez', 'USER', false, '2026-03-01T10:00:00'),
  user('4', 'marta', 'Marta Núñez', 'ADMIN', false, '2026-04-01T10:00:00')
];

describe('UsersComponent', () => {
  let fixture: ComponentFixture<UsersComponent>;
  let component: UsersComponent;
  let users: Record<string, ReturnType<typeof vi.fn>>;
  let alerts: Record<string, ReturnType<typeof vi.fn>>;

  beforeEach(async () => {
    users = {
      list: vi.fn().mockReturnValue(of(USERS)),
      create: vi.fn(),
      update: vi.fn(),
      setActive: vi.fn(),
      resetPassword: vi.fn()
    };
    alerts = {
      mostrarAlertaError: vi.fn(),
      mostrarAlertaConTimer: vi.fn(),
      confirmarAccion: vi.fn().mockResolvedValue(true)
    };

    await TestBed.configureTestingModule({
      imports: [UsersComponent],
      providers: [
        provideRouter([]),
        { provide: UserService, useValue: users },
        { provide: AlertaService, useValue: alerts },
        { provide: AuthService, useValue: { currentUser: () => ({ userId: '1' }), isAdmin: () => true, logout: vi.fn() } }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(UsersComponent);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  const names = () => component.visibleUsers().map((u) => u.username);

  it('lista los usuarios con el resumen de activos', () => {
    expect(names()).toEqual(['admin', 'ana', 'luis', 'marta']);
    expect(component.summary()).toBe('4 usuarios · 2 activos');
  });

  it('busca por usuario o nombre sin distinguir mayúsculas ni acentos', () => {
    component.search.set('PEREZ');
    expect(names()).toEqual(['ana']);

    component.search.set('nunez');
    expect(names()).toEqual(['marta']);

    component.search.set('adm');
    expect(names()).toEqual(['admin']);
  });

  it('filtra por rol y por estado, y los combina', () => {
    component.roleFilter.set('ADMIN');
    expect(names()).toEqual(['admin', 'marta']);

    component.statusFilter.set('inactive');
    expect(names()).toEqual(['marta']);

    component.roleFilter.set('USER');
    component.statusFilter.set('active');
    expect(names()).toEqual(['ana']);
  });

  it('limpiar filtros restablece la lista completa', () => {
    component.search.set('ana');
    component.roleFilter.set('USER');
    expect(component.hasFilters()).toBe(true);

    component.clearFilters();

    expect(component.hasFilters()).toBe(false);
    expect(names()).toHaveLength(4);
  });

  it('el resumen indica cuántos se muestran cuando hay filtros', () => {
    component.statusFilter.set('active');

    expect(component.summary()).toBe('2 de 4 usuarios · 2 activos');
  });

  it('ordena por columna y alterna el sentido al repetir', () => {
    component.sortBy('fullName');
    expect(names()).toEqual(['admin', 'ana', 'luis', 'marta']);
    expect(component.ariaSort('fullName')).toBe('ascending');

    component.sortBy('fullName');
    expect(names()).toEqual(['marta', 'luis', 'ana', 'admin']);
    expect(component.ariaSort('fullName')).toBe('descending');

    component.sortBy('createdAt');
    component.sortBy('createdAt');
    expect(names()[0]).toBe('marta');
  });

  it('ordena por estado agrupando inactivos y activos', () => {
    component.sortBy('active');

    expect(component.visibleUsers().map((u) => u.active)).toEqual([false, false, true, true]);
  });

  it('edita el nombre y el rol de otro usuario', () => {
    const edited = { ...USERS[1], fullName: 'Ana María', role: 'ADMIN' as const };
    users['update'].mockReturnValue(of(edited));

    component.openEdit(USERS[1]);
    component.editForm.setValue({ fullName: '  Ana María ', role: 'ADMIN' });
    component.updateUser();

    expect(users['update']).toHaveBeenCalledWith('2', { fullName: 'Ana María', role: 'ADMIN' });
    expect(component.users().find((u) => u.id === '2')?.fullName).toBe('Ana María');
    expect(component.modal()).toBeNull();
    expect(component.saving()).toBe(false);
  });

  it('no envía la edición con el nombre vacío', () => {
    component.openEdit(USERS[1]);
    component.editForm.setValue({ fullName: '', role: 'USER' });

    component.updateUser();

    expect(users['update']).not.toHaveBeenCalled();
    expect(component.invalid('edit', 'fullName')).toBe(true);
  });

  it('muestra el motivo del backend cuando una regla de negocio rechaza la edición', () => {
    users['update'].mockReturnValue(throwError(() => ({
      status: 409, error: { message: 'Debe existir al menos un administrador activo.' }
    })));

    component.openEdit(USERS[3]);
    component.editForm.setValue({ fullName: 'Marta', role: 'USER' });
    component.updateUser();

    expect(alerts['mostrarAlertaError']).toHaveBeenCalledWith('Debe existir al menos un administrador activo.');
    expect(component.saving()).toBe(false);
    expect(component.modal()).toBe('edit');
  });

  it('identifica la propia cuenta para bloquear el cambio de rol y la desactivación', () => {
    expect(component.isSelf(USERS[0])).toBe(true);
    expect(component.isSelf(USERS[1])).toBe(false);
    expect(component.isSelf(null)).toBe(false);
  });

  it('desactivar pide confirmación y no actúa si se cancela', async () => {
    alerts['confirmarAccion'].mockResolvedValue(false);

    await component.toggleActive(USERS[1]);

    expect(users['setActive']).not.toHaveBeenCalled();
  });

  it('desactivar confirmado actualiza al usuario; activar no pide confirmación', async () => {
    users['setActive'].mockImplementation((id: string, active: boolean) =>
      of({ ...USERS.find((u) => u.id === id)!, active }));

    await component.toggleActive(USERS[1]);
    expect(component.users().find((u) => u.id === '2')?.active).toBe(false);

    alerts['confirmarAccion'].mockClear();
    await component.toggleActive(USERS[2]);
    expect(alerts['confirmarAccion']).not.toHaveBeenCalled();
    expect(component.users().find((u) => u.id === '3')?.active).toBe(true);
  });

  it('muestra el motivo cuando no se puede desactivar al último administrador', async () => {
    users['setActive'].mockReturnValue(throwError(() => ({
      status: 409, error: { message: 'Debe existir al menos un administrador activo.' }
    })));

    await component.toggleActive(USERS[0]);

    expect(alerts['mostrarAlertaError']).toHaveBeenCalledWith('Debe existir al menos un administrador activo.');
  });

  it('un error inesperado usa el mensaje genérico', async () => {
    users['setActive'].mockReturnValue(throwError(() => ({ status: 500, error: {} })));

    await component.toggleActive(USERS[1]);

    expect(alerts['mostrarAlertaError']).toHaveBeenCalledWith('No se pudo actualizar el usuario.');
  });

  it('restablecer la contraseña avisa de que se cierran las sesiones', () => {
    users['resetPassword'].mockReturnValue(of(undefined));

    component.openReset(USERS[1]);
    component.resetForm.setValue({ newPassword: 'Nueva-clave-1' });
    component.resetPassword();

    expect(users['resetPassword']).toHaveBeenCalledWith('2', 'Nueva-clave-1');
    expect(alerts['mostrarAlertaConTimer']).toHaveBeenCalledWith('Listo', expect.stringContaining('sesiones abiertas'));
  });
});
