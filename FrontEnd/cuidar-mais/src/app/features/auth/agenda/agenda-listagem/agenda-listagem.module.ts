import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { AgendaListagemComponent } from './agenda-listagem.component';
import { CustomCalendarComponent } from './calendario/custom-calendar.component';

@NgModule({
  declarations: [AgendaListagemComponent, CustomCalendarComponent],
  imports: [CommonModule],
  exports: [AgendaListagemComponent],
})
export class AgendaListagemModule {}
