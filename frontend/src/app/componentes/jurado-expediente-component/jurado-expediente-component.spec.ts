import { ComponentFixture, TestBed } from '@angular/core/testing';

import { JuradoExpedienteComponent } from './jurado-expediente-component';

describe('JuradoExpedienteComponent', () => {
  let component: JuradoExpedienteComponent;
  let fixture: ComponentFixture<JuradoExpedienteComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [JuradoExpedienteComponent],
    }).compileComponents();

    fixture = TestBed.createComponent(JuradoExpedienteComponent);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
