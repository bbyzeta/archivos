import { ComponentFixture, TestBed } from '@angular/core/testing';

import { PostulacionExpedienteComponent } from './postulacion-expediente-component';

describe('PostulacionExpedienteComponent', () => {
  let component: PostulacionExpedienteComponent;
  let fixture: ComponentFixture<PostulacionExpedienteComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [PostulacionExpedienteComponent],
    }).compileComponents();

    fixture = TestBed.createComponent(PostulacionExpedienteComponent);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
