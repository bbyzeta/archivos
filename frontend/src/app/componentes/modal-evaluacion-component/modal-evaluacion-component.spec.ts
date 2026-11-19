import { ComponentFixture, TestBed } from '@angular/core/testing';

import { ModalEvaluacionComponent } from './modal-evaluacion-component';

describe('ModalEvaluacionComponent', () => {
  let component: ModalEvaluacionComponent;
  let fixture: ComponentFixture<ModalEvaluacionComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [ModalEvaluacionComponent],
    }).compileComponents();

    fixture = TestBed.createComponent(ModalEvaluacionComponent);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
