import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { CabecalhoModule } from '../../../cabecalho/cabecalho.module';
import { RouterModule } from '@angular/router';

@NgModule({
  declarations: [],
  imports: [
    CommonModule,
    CabecalhoModule,
    RouterModule
  ],
  exports: []
})
export class AgendaModule { }
