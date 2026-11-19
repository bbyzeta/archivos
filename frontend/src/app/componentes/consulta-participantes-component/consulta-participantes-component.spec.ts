import { ComponentFixture, TestBed } from '@angular/core/testing';

import { ConsultaParticipantesComponent } from './consulta-participantes-component';

describe('ConsultaParticipantesComponent', () => {
  let component: ConsultaParticipantesComponent;
  let fixture: ComponentFixture<ConsultaParticipantesComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [ConsultaParticipantesComponent],
    }).compileComponents();

    fixture = TestBed.createComponent(ConsultaParticipantesComponent);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
