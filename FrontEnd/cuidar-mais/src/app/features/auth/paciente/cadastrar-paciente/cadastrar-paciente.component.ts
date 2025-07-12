import { Component } from '@angular/core';
import { CabecalhoModule } from '../../../../cabecalho/cabecalho.module';
import { CommonModule } from '@angular/common';

@Component({
  selector: 'app-cadastrar-paciente',
  templateUrl: './cadastrar-paciente.component.html',
  styleUrl: './cadastrar-paciente.component.scss',
  standalone: true,
  imports: [CabecalhoModule, CommonModule]

})
export class CadastrarPacienteComponent {

}
