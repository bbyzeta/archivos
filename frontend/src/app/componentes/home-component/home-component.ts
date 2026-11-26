import { Component, inject, signal } from '@angular/core';
import { DatePipe } from '@angular/common';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { Folder } from '../../models/folder.model';
import { FolderService } from '../../services/folder.service';
import { AlertaService } from '../alerts/alert-component';
import { NavbarComponent } from '../navbar-component/navbar-component';

@Component({
  selector: 'app-home',
  standalone: true,
  imports: [DatePipe, ReactiveFormsModule, RouterLink, NavbarComponent],
  templateUrl: './home-component.html',
  styleUrl: './home-component.scss'
})
export class HomeComponent {
  private readonly fb = inject(FormBuilder);
  private readonly folderService = inject(FolderService);
  private readonly alertService = inject(AlertaService);

  readonly folders = signal<Folder[]>([]);
  readonly loading = signal(true);
  readonly modalOpen = signal(false);
  readonly saving = signal(false);

  readonly folderForm = this.fb.nonNullable.group({
    name: ['', [Validators.required, Validators.maxLength(150)]]
  });

  constructor() {
    this.loadFolders();
  }

  openModal(): void {
    this.folderForm.reset();
    this.modalOpen.set(true);
  }

  closeModal(): void {
    if (!this.saving()) this.modalOpen.set(false);
  }

  createFolder(): void {
    const name = this.folderForm.controls.name.value.trim();
    if (!name) {
      this.folderForm.controls.name.setErrors({ required: true });
      this.folderForm.markAllAsTouched();
      return;
    }

    this.saving.set(true);
    this.folderService.create({ name }).subscribe({
      next: (folder) => {
        this.folders.update((list) => [folder, ...list]);
        this.saving.set(false);
        this.modalOpen.set(false);
      },
      error: () => {
        this.saving.set(false);
        this.alertService.mostrarAlertaError('No se pudo crear la carpeta. Intenta de nuevo.');
      }
    });
  }

  esNombreInvalido(): boolean {
    const control = this.folderForm.controls.name;
    return control.invalid && (control.dirty || control.touched);
  }

  private loadFolders(): void {
    this.folderService.list().subscribe({
      next: (folders) => {
        this.folders.set(folders);
        this.loading.set(false);
      },
      error: () => {
        this.loading.set(false);
        this.alertService.mostrarAlertaError('No se pudieron cargar las carpetas.');
      }
    });
  }
}
