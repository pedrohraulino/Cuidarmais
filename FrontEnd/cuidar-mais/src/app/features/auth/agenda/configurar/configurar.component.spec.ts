import { ComponentFixture, TestBed } from '@angular/core/testing';

import { ConfigurarComponent } from './configurar.component';

describe('ConfigurarComponent', () => {
  let component: ConfigurarComponent;
  let fixture: ComponentFixture<ConfigurarComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      declarations: [ConfigurarComponent]
    })
    .compileComponents();
    
    fixture = TestBed.createComponent(ConfigurarComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
