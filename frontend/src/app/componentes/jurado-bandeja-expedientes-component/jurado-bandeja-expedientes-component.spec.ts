import { ComponentFixture, TestBed } from '@angular/core/testing';

import { JuradoBandejaExpedientesComponent } from './jurado-bandeja-expedientes-component';

describe('JuradoBandejaExpedientesComponent', () => {
  let component: JuradoBandejaExpedientesComponent;
  let fixture: ComponentFixture<JuradoBandejaExpedientesComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [JuradoBandejaExpedientesComponent],
    }).compileComponents();

    fixture = TestBed.createComponent(JuradoBandejaExpedientesComponent);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
