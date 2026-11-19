import { ComponentFixture, TestBed } from '@angular/core/testing';

import { RecuperarPassWordComponent } from './recuperar-pass-word-component';

describe('RecuperarPassWordComponent', () => {
  let component: RecuperarPassWordComponent;
  let fixture: ComponentFixture<RecuperarPassWordComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [RecuperarPassWordComponent],
    }).compileComponents();

    fixture = TestBed.createComponent(RecuperarPassWordComponent);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
