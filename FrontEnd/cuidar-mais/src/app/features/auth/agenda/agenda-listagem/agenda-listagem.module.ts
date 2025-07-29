import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { AgendaListagemComponent } from './agenda-listagem.component';
import { CustomCalendarComponent } from './custom-calendar.component';
import { CabecalhoComponent } from '../../../../cabecalho/cabecalho/cabecalho.component';
import { CabecalhoModule } from '../../../../cabecalho/cabecalho.module';

@NgModule({
  declarations: [AgendaListagemComponent, CustomCalendarComponent],
  imports: [CommonModule, CabecalhoModule],
  exports: [AgendaListagemComponent],
})
export class AgendaListagemModule {}
