import { ComponentFixture, TestBed } from '@angular/core/testing';

import { PostulanteConvocatoriasComponent } from './postulante-convocatorias-component';

describe('PostulanteConvocatoriasComponent', () => {
  let component: PostulanteConvocatoriasComponent;
  let fixture: ComponentFixture<PostulanteConvocatoriasComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [PostulanteConvocatoriasComponent],
    }).compileComponents();

    fixture = TestBed.createComponent(PostulanteConvocatoriasComponent);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
