import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { LoginComponent } from './login/login.component';
import { FormsComponent } from './login/forms/forms.component';
import { FormsModule } from '@angular/forms';
import { AgendaModule } from './agenda/agenda.module';
import { CabecalhoModule } from '../../cabecalho/cabecalho.module';



@NgModule({
  declarations: [
    LoginComponent,
    FormsComponent
  ],
  imports: [
    CommonModule,
    FormsModule, CabecalhoModule, AgendaModule
  ]
})
export class AuthModule { }
