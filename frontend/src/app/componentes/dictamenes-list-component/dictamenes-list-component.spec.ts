import { ComponentFixture, TestBed } from '@angular/core/testing';

import { DictamenesListComponent } from './dictamenes-list-component';

describe('DictamenesListComponent', () => {
  let component: DictamenesListComponent;
  let fixture: ComponentFixture<DictamenesListComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [DictamenesListComponent],
    }).compileComponents();

    fixture = TestBed.createComponent(DictamenesListComponent);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
