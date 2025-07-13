import { Component, OnInit } from '@angular/core';
import { CabecalhoModule } from '../../../../cabecalho/cabecalho.module';
import { CommonModule } from '@angular/common';
import { HttpClient, HttpClientModule } from '@angular/common/http';
import { Router } from '@angular/router';
import { AuthService } from '../../auth.service';
import { MessageService } from '../../../../shared/message.service';
import { FormsModule } from '@angular/forms';

// Interfaces para tipagem
interface Agendamento {
  id: number;
  data: string;
  horaInicio: string;
  horaFim: string;
  status: string;
}

interface Paciente {
  id: number;
  nome: string;
  sobrenome: string;
  sexo: string;
  dataNascimento: string;
  email: string;
  telefone: string;
  imagemBase64?: string;
  imagemTipo?: string;
  ativo?: boolean;
  agendamentos?: Agendamento[];
}

@Component({
  selector: 'app-lista-pacientes',
  templateUrl: './lista-pacientes.component.html',
  styleUrl: './lista-pacientes.component.scss',
  standalone: true,
  imports: [CabecalhoModule, CommonModule, HttpClientModule, FormsModule]
})
export class ListaPacientesComponent implements OnInit {
  // Controle
  pacientes: Paciente[] = [];
  pacientesFiltrados: Paciente[] = [];
  erro = '';
  sucesso = '';

  // Filtros
  termoBusca = '';
  statusFiltro = 'ativos'; // Valores possíveis: 'ativos', 'inativos', 'todos'

  // Usuário logado
  psicologoId: number | null = null;

  constructor(
    private http: HttpClient,
    private router: Router,
    private authService: AuthService,
    private messageService: MessageService
  ) {}

  ngOnInit() {
    // Verifica se o usuário está autenticado
    if (!this.authService.isAuthenticated()) {
      this.router.navigate(['/login']);
      return;
    }

    // Subscreve aos observáveis de mensagens
    this.messageService.successMessage$.subscribe(message => {
      this.sucesso = message;
    });

    this.messageService.errorMessage$.subscribe(message => {
      this.erro = message;
    });

    // Recupera o usuário logado
    const usuario = this.authService.getUser();
    if (usuario) {
      this.psicologoId = usuario.id;
      this.carregarPacientes();
    } else {
      // Tenta buscar as informações do usuário novamente
      this.authService.getUserInfo().subscribe({
        next: (user) => {
          this.psicologoId = user.id;
          this.carregarPacientes();
        },
        error: () => {
          this.erro = 'Usuário não autenticado';
          this.router.navigate(['/login']);
        }
      });
    }
  }

  carregarPacientes() {
    if (!this.psicologoId) return;

    this.http.get<Paciente[]>(`http://localhost:8080/api/pacientes/psicologo/${this.psicologoId}`).subscribe({
      next: (pacientes) => {
        // Para cada paciente, buscar seus agendamentos agendados
        this.pacientes = [];

        if (pacientes.length === 0) {
          this.pacientesFiltrados = [];
          return;
        }

        let pacientesProcessados = 0;

        pacientes.forEach(paciente => {
          this.http.get<Agendamento[]>(`http://localhost:8080/api/pacientes/${paciente.id}/agendamentos/ativos-agendados`).subscribe({
            next: (agendamentos) => {
              // Adicionar informações de agendamento ao paciente
              paciente.agendamentos = agendamentos;
              this.pacientes.push(paciente);

              pacientesProcessados++;
              if (pacientesProcessados === pacientes.length) {
                // Ordenar pacientes por nome
                this.pacientes.sort((a, b) => a.nome.localeCompare(b.nome));
                // Aplicar filtros iniciais
                this.aplicarFiltros();
              }
            },
            error: (err) => {
              console.error('Erro ao carregar agendamentos do paciente:', err);
              paciente.agendamentos = [];
              this.pacientes.push(paciente);

              pacientesProcessados++;
              if (pacientesProcessados === pacientes.length) {
                // Ordenar pacientes por nome
                this.pacientes.sort((a, b) => a.nome.localeCompare(b.nome));
                // Aplicar filtros iniciais
                this.aplicarFiltros();
              }
            }
          });
        });
      },
      error: (err) => {
        this.erro = 'Erro ao carregar pacientes: ' + (err.error?.erro || 'Erro desconhecido');
      }
    });
  }

  aplicarFiltros() {
    // Filtra por status (ativo/inativo)
    let pacientesFiltradosPorStatus = this.pacientes;
    if (this.statusFiltro === 'ativos') {
      pacientesFiltradosPorStatus = this.pacientes.filter(p => p.ativo !== false);
    } else if (this.statusFiltro === 'inativos') {
      pacientesFiltradosPorStatus = this.pacientes.filter(p => p.ativo === false);
    }

    // Filtra por termo de busca
    if (this.termoBusca.trim()) {
      const termo = this.termoBusca.toLowerCase().trim();
      this.pacientesFiltrados = pacientesFiltradosPorStatus.filter(p =>
        p.nome.toLowerCase().includes(termo) ||
        p.sobrenome.toLowerCase().includes(termo) ||
        (p.email && p.email.toLowerCase().includes(termo)) ||
        (p.telefone && p.telefone.includes(termo))
      );
    } else {
      this.pacientesFiltrados = pacientesFiltradosPorStatus;
    }
  }

  buscarPacientes() {
    this.aplicarFiltros();
  }

  navegarParaCadastro() {
    this.router.navigate(['/pacientes/cadastrar']);
  }

  editarPaciente(pacienteId: number) {
    this.router.navigate([`/pacientes/editar/${pacienteId}`]);
  }

  inativarPaciente(pacienteId: number) {
    if (confirm('Tem certeza que deseja inativar este paciente? Todos os agendamentos vinculados serão inativados.')) {
      this.http.post(`http://localhost:8080/api/pacientes/${pacienteId}/inativar`, {}).subscribe({
        next: (response) => {
          this.sucesso = 'Paciente inativado com sucesso!';
          this.carregarPacientes(); // Recarrega a lista de pacientes
        },
        error: (err) => {
          this.erro = 'Erro ao inativar paciente: ' + (err.error?.erro || 'Erro desconhecido');
        }
      });
    }
  }

  reativarPaciente(pacienteId: number) {
    if (confirm('Deseja reativar este paciente?')) {
      this.http.post(`http://localhost:8080/api/pacientes/${pacienteId}/reativar`, {}).subscribe({
        next: (response) => {
          this.sucesso = 'Paciente reativado com sucesso!';
          this.carregarPacientes(); // Recarrega a lista de pacientes
        },
        error: (err) => {
          this.erro = 'Erro ao reativar paciente: ' + (err.error?.erro || 'Erro desconhecido');
        }
      });
    }
  }

  criarSessoesAdicionais(pacienteId: number) {
    if (confirm('Deseja criar sessões adicionais para este paciente?')) {
      this.http.post(`http://localhost:8080/api/pacientes/${pacienteId}/criar-sessoes-adicionais`, {}).subscribe({
        next: (response) => {
          this.sucesso = 'Sessões adicionais criadas com sucesso!';
          this.carregarPacientes(); // Recarrega a lista de pacientes
        },
        error: (err) => {
          this.erro = 'Erro ao criar sessões adicionais: ' + (err.error?.erro || 'Erro desconhecido');
        }
      });
    }
  }

  obterNomeDiaSemana(dia: string): string {
    const dias: {[key: string]: string} = {
      'MONDAY': 'Segunda-feira',
      'TUESDAY': 'Terça-feira',
      'WEDNESDAY': 'Quarta-feira',
      'THURSDAY': 'Quinta-feira',
      'FRIDAY': 'Sexta-feira',
      'SATURDAY': 'Sábado',
      'SUNDAY': 'Domingo'
    };
    return dias[dia] || dia;
  }

  formatarHora(hora: string): string {
    if (!hora) return '';
    const [h, m] = hora.split(':');
    return `${h}:${m}`;
  }
}
