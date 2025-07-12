import { Component, NgModule } from '@angular/core';
import { CabecalhoModule } from '../../../../cabecalho/cabecalho.module';
import { CommonModule } from '@angular/common';

@Component({
  selector: 'app-configurar',
  templateUrl: './configurar.component.html',
  styleUrls: ['./configurar.component.scss'],
  standalone: true,
  imports: [CabecalhoModule, CommonModule]
})

export class ConfigurarComponent {

}
