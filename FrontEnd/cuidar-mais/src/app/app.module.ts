import { TokenInterceptor } from './features/auth/auth.interceptor';
import { NgModule } from '@angular/core';
import {
  BrowserModule,
  provideClientHydration,
} from '@angular/platform-browser';

import { AppRoutingModule } from './app-routing.module';
import { AppComponent } from './app.component';
import { AuthModule } from './features/auth/auth.module';
import { HTTP_INTERCEPTORS, HttpClientModule } from '@angular/common/http';
import { MenuLateralComponent } from './menu-lateral/menu-lateral.component';
import { CabecalhoModule } from './cabecalho/cabecalho.module';
import { AgendaListagemModule } from './features/auth/agenda/agenda-listagem/agenda-listagem.module';

@NgModule({
  declarations: [AppComponent, MenuLateralComponent],
  imports: [BrowserModule, AppRoutingModule, AuthModule, HttpClientModule, CabecalhoModule, CabecalhoModule, AgendaListagemModule],
  providers: [
    { provide: HTTP_INTERCEPTORS, useClass: TokenInterceptor, multi: true }
  ],
  bootstrap: [AppComponent],
})
export class AppModule {}
