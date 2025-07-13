import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';
import { LoginComponent } from './features/auth/login/login.component';
import { CadastrarPacienteComponent } from './features/auth/paciente/cadastrar-paciente/cadastrar-paciente.component';
import { authGuard } from '../auth.guard';
import { ConfigurarComponent } from './features/auth/agenda/configurar/configurar.component';
import { ListaPacientesComponent } from './features/auth/paciente/lista-pacientes/lista-pacientes.component';

const routes: Routes = [
  { path: 'login', component: LoginComponent },
  {
    path: 'pacientes/cadastrar',
    component: CadastrarPacienteComponent,
    canActivate: [authGuard],
    data: { title: 'Cadastrar Paciente' },
  },
  {
    path: 'pacientes/editar/:id',
    component: CadastrarPacienteComponent,
    canActivate: [authGuard],
    data: { title: 'Editar Paciente' },
  },
  {
    path: 'pacientes',
    component: ListaPacientesComponent,
    canActivate: [authGuard],
    data: { title: 'Pacientes' },
  },
  {
    path: 'configuracoes',
    component: ConfigurarComponent,
    canActivate: [authGuard],
    data: { title: 'Configurações' },
  },

  { path: '', redirectTo: 'login', pathMatch: 'full' },
];

@NgModule({
  imports: [RouterModule.forRoot(routes)],
  exports: [RouterModule],
})
export class AppRoutingModule {}
