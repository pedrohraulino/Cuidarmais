import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { CadastrarPacienteComponent } from './cadastrar-paciente/cadastrar-paciente.component';
import { CabecalhoModule } from '../../../cabecalho/cabecalho.module';



@NgModule({
  declarations: [
  ],
  imports: [
    CommonModule, CabecalhoModule
  ]
})
export class PacienteModule { }
