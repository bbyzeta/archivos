import { ComponentFixture, TestBed } from '@angular/core/testing';

import { PostulanteConvocatoriaComponent } from './postulante-convocatoria-component';

describe('PostulanteConvocatoriaComponent', () => {
  let component: PostulanteConvocatoriaComponent;
  let fixture: ComponentFixture<PostulanteConvocatoriaComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [PostulanteConvocatoriaComponent],
    }).compileComponents();

    fixture = TestBed.createComponent(PostulanteConvocatoriaComponent);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
