import { ComponentFixture, TestBed } from '@angular/core/testing';

import { CadastrarPacienteComponent } from './cadastrar-paciente.component';

describe('CadastrarPacienteComponent', () => {
  let component: CadastrarPacienteComponent;
  let fixture: ComponentFixture<CadastrarPacienteComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      declarations: [CadastrarPacienteComponent]
    })
    .compileComponents();
    
    fixture = TestBed.createComponent(CadastrarPacienteComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
