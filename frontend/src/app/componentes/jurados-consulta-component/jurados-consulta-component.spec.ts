import { ComponentFixture, TestBed } from '@angular/core/testing';

import { JuradosConsultaComponent } from './jurados-consulta-component';

describe('JuradosConsultaComponent', () => {
  let component: JuradosConsultaComponent;
  let fixture: ComponentFixture<JuradosConsultaComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [JuradosConsultaComponent],
    }).compileComponents();

    fixture = TestBed.createComponent(JuradosConsultaComponent);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
