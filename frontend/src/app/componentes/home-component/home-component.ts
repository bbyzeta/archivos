import { Component, HostListener, computed, inject, signal } from '@angular/core';
import { DatePipe } from '@angular/common';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { Folder } from '../../models/folder.model';
import { ExplorerPreferencesService } from '../../services/explorer-preferences.service';
import { FolderService } from '../../services/folder.service';
import { sortByKey } from '../../utils/file-explorer';
import { DEFAULT_FOLDER_COLOR } from '../../utils/folder-colors';
import { AlertaService } from '../alerts/alert-component';
import { ColorPickerComponent } from '../color-picker-component/color-picker-component';
import { ExplorerToolbarComponent } from '../explorer-toolbar-component/explorer-toolbar-component';
import { NavbarComponent } from '../navbar-component/navbar-component';

@Component({
  selector: 'app-home',
  standalone: true,
  imports: [DatePipe, ReactiveFormsModule, RouterLink, NavbarComponent, ColorPickerComponent, ExplorerToolbarComponent],
  templateUrl: './home-component.html',
  styleUrl: './home-component.scss'
})
export class HomeComponent {
  private readonly fb = inject(FormBuilder);
  private readonly folderService = inject(FolderService);
  private readonly alertService = inject(AlertaService);
  protected readonly prefs = inject(ExplorerPreferencesService);

  readonly folders = signal<Folder[]>([]);
  readonly loading = signal(true);
  readonly search = signal('');
  readonly defaultColor = DEFAULT_FOLDER_COLOR;

  /** Carpetas que coinciden con el buscador (sin distinguir mayúsculas ni acentos), ya ordenadas. */
  readonly filteredFolders = computed(() => {
    const term = this.normalize(this.search());
    const matches = term ? this.folders().filter((f) => this.normalize(f.name).includes(term)) : this.folders();
    return sortByKey(matches, this.prefs.homeSort(), (f) => f.name, (f) => f.createdAt);
  });

  readonly summary = computed(() => {
    const total = this.folders().length;
    const shown = this.filteredFolders().length;
    const noun = total === 1 ? 'carpeta' : 'carpetas';
    return this.search().trim() ? `${shown} de ${total} ${noun}` : `${total} ${noun}`;
  });

  // Modal compartido: crear (renaming = null) o renombrar (renaming = carpeta)
  readonly modalOpen = signal(false);
  readonly saving = signal(false);
  readonly renaming = signal<Folder | null>(null);

  readonly folderForm = this.fb.nonNullable.group({
    name: ['', [Validators.required, Validators.maxLength(150)]]
  });

  constructor() {
    this.loadFolders();
  }

  onSearch(event: Event): void {
    this.search.set((event.target as HTMLInputElement).value);
  }

  openModal(): void {
    this.renaming.set(null);
    this.folderForm.reset();
    this.modalOpen.set(true);
  }

  openRename(folder: Folder): void {
    this.renaming.set(folder);
    this.folderForm.reset({ name: folder.name });
    this.modalOpen.set(true);
  }

  closeModal(): void {
    if (!this.saving()) this.modalOpen.set(false);
  }

  @HostListener('document:keydown.escape')
  onEscape(): void {
    if (this.modalOpen()) this.closeModal();
  }

  submitFolder(): void {
    const name = this.folderForm.controls.name.value.trim();
    if (!name) {
      this.folderForm.controls.name.setErrors({ required: true });
      this.folderForm.markAllAsTouched();
      return;
    }

    const target = this.renaming();
    this.saving.set(true);

    if (target) {
      this.folderService.rename(target.id, name).subscribe({
        next: (updated) => {
          this.folders.update((list) => list.map((f) => (f.id === updated.id ? updated : f)));
          this.saving.set(false);
          this.modalOpen.set(false);
        },
        error: () => {
          this.saving.set(false);
          this.alertService.mostrarAlertaError('No se pudo renombrar la carpeta. Intenta de nuevo.');
        }
      });
      return;
    }

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

  setColor(folder: Folder, color: string | null): void {
    this.folderService.setColor(folder.id, color).subscribe({
      next: (updated) => this.folders.update((list) => list.map((f) => (f.id === updated.id ? updated : f))),
      error: () => this.alertService.mostrarAlertaError('No se pudo cambiar el color de la carpeta.')
    });
  }

  async deleteFolder(folder: Folder): Promise<void> {
    const confirmed = await this.alertService.confirmarAccion(
      '¿Eliminar carpeta?',
      `Se eliminará "${folder.name}" con todos sus archivos y subcarpetas. Los links públicos dejarán de funcionar. Esta acción no se puede deshacer.`,
      'Sí, eliminar');
    if (!confirmed) return;

    this.folderService.delete(folder.id).subscribe({
      next: () => this.folders.update((list) => list.filter((f) => f.id !== folder.id)),
      error: () => this.alertService.mostrarAlertaError('No se pudo eliminar la carpeta.')
    });
  }

  esNombreInvalido(): boolean {
    const control = this.folderForm.controls.name;
    return control.invalid && (control.dirty || control.touched);
  }

  private normalize(text: string): string {
    return text.trim().toLowerCase().normalize('NFD').replace(/[̀-ͯ]/g, '');
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
