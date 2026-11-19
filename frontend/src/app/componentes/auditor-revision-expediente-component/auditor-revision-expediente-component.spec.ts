import { ComponentFixture, TestBed } from '@angular/core/testing';

import { AuditorRevisionExpedienteComponent } from './auditor-revision-expediente-component';

describe('AuditorRevisionExpedienteComponent', () => {
  let component: AuditorRevisionExpedienteComponent;
  let fixture: ComponentFixture<AuditorRevisionExpedienteComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [AuditorRevisionExpedienteComponent],
    }).compileComponents();

    fixture = TestBed.createComponent(AuditorRevisionExpedienteComponent);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
