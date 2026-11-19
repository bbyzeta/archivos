import { ComponentFixture, TestBed } from '@angular/core/testing';

import { AuditorBandejaExpedientesComponent } from './auditor-bandeja-expedientes-component';

describe('AuditorBandejaExpedientesComponent', () => {
  let component: AuditorBandejaExpedientesComponent;
  let fixture: ComponentFixture<AuditorBandejaExpedientesComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [AuditorBandejaExpedientesComponent],
    }).compileComponents();

    fixture = TestBed.createComponent(AuditorBandejaExpedientesComponent);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
