import { ComponentFixture, TestBed } from '@angular/core/testing';

import { ConsultaConvocatoriasComponent } from './consulta-convocatorias-component';

describe('ConsultaConvocatoriasComponent', () => {
  let component: ConsultaConvocatoriasComponent;
  let fixture: ComponentFixture<ConsultaConvocatoriasComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [ConsultaConvocatoriasComponent],
    }).compileComponents();

    fixture = TestBed.createComponent(ConsultaConvocatoriasComponent);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
