import { Component, OnInit } from '@angular/core';
import { CabecalhoModule } from '../../../../cabecalho/cabecalho.module';
import { CommonModule } from '@angular/common';
import { HttpClient, HttpClientModule } from '@angular/common/http';
import { Router } from '@angular/router';
import { AuthService } from '../../auth.service';
import { MessageService } from '../../../../shared/message.service';
import { FormsModule } from '@angular/forms';

// Interfaces para tipagem atualizadas
interface Sessao {
  id: number;
  dataSessao: string;
  horaInicio: string;
  horaFim: string;
  numeroSessao: number;
  status: 'AGENDADA' | 'REALIZADA' | 'CANCELADA';
  observacoes?: string;
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
  psicologoId: number;
  diaSemana?: string;
  horarioInicio?: string;
  horarioFim?: string;
  sessoesPorPacote: number;
  ativo: boolean;  // Removido o "?" para tornar obrigatório
  sessoes?: Sessao[];
  sessoesRestantes?: number;
  sessoesRealizadas?: number;
  horarioDisponivel?: {
    id: number;
    diaSemana: string;
    horaInicio: string;
    horaFim: string;
  };
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

    this.http.get<Paciente[]>(`http://localhost:8080/api/pacientes/psicologo/${this.psicologoId}/todos`).subscribe({
      next: (pacientes) => {
        // Para cada paciente, buscar suas sessões
        this.pacientes = [];

        if (pacientes.length === 0) {
          this.pacientesFiltrados = [];
          return;
        }

        let pacientesProcessados = 0;

        pacientes.forEach(paciente => {
          this.http.get<Sessao[]>(`http://localhost:8080/api/sessoes/paciente/${paciente.id}`).subscribe({
            next: (sessoes) => {
              // Adicionar informações de sessões ao paciente
              paciente.sessoes = sessoes;
              paciente.sessoesRestantes = sessoes.filter(s => s.status === 'AGENDADA').length;
              paciente.sessoesRealizadas = sessoes.filter(s => s.status === 'REALIZADA').length;
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
              paciente.sessoes = [];
              paciente.sessoesRestantes = 0;
              paciente.sessoesRealizadas = 0;
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
      pacientesFiltradosPorStatus = this.pacientes.filter(p => p.ativo === true);
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

    console.log('Aplicando filtros:');
    console.log('- Todos os pacientes:', this.pacientes.length);
    console.log('- Status filtro:', this.statusFiltro);
    console.log('- Pacientes após filtro de status:', pacientesFiltradosPorStatus.length);
    console.log('- Pacientes finais após busca:', this.pacientesFiltrados.length);
    
    // Log para debug dos status dos pacientes
    console.log('Status dos pacientes:', this.pacientes.map(p => ({ nome: p.nome, ativo: p.ativo })));
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

  obterNomeDiaSemana(entrada: string): string {
    // Se for uma data no formato ISO, extrair o dia da semana
    if (entrada.includes('-')) {
      const data = new Date(entrada);
      const diasSemana = ['Domingo', 'Segunda-feira', 'Terça-feira', 'Quarta-feira', 'Quinta-feira', 'Sexta-feira', 'Sábado'];
      return diasSemana[data.getDay()];
    }

    // Se for o nome do dia em inglês (formato enum)
    const dias: {[key: string]: string} = {
      'MONDAY': 'Segunda-feira',
      'TUESDAY': 'Terça-feira',
      'WEDNESDAY': 'Quarta-feira',
      'THURSDAY': 'Quinta-feira',
      'FRIDAY': 'Sexta-feira',
      'SATURDAY': 'Sábado',
      'SUNDAY': 'Domingo'
    };
    return dias[entrada.toUpperCase()] || entrada;
  }

  formatarHora(hora: string): string {
    if (!hora) return '';
    const [h, m] = hora.split(':');
    return `${h}:${m}`;
  }

  calcularIdade(dataNascimento: string): number {
    const nascimento = new Date(dataNascimento);
    const hoje = new Date();
    let idade = hoje.getFullYear() - nascimento.getFullYear();
    const mes = hoje.getMonth() - nascimento.getMonth();

    if (mes < 0 || (mes === 0 && hoje.getDate() < nascimento.getDate())) {
      idade--;
    }

    return idade;
  }

  formatarDataNascimento(dataNascimento: string): string {
    const data = new Date(dataNascimento);
    const dia = data.getDate().toString().padStart(2, '0');
    const mes = (data.getMonth() + 1).toString().padStart(2, '0');
    const ano = data.getFullYear();
    return `${dia}/${mes}/${ano}`;
  }

  calcularSessoesRestantes(paciente: Paciente): number {
    // Retorna o número de sessões restantes já calculado
    return paciente.sessoesRestantes || 0;
  }

  obterDiaEHorario(paciente: Paciente): string {
    // Primeira opção: usar dados diretamente do paciente (nova estrutura)
    if (paciente.diaSemana && paciente.horarioInicio && paciente.horarioFim) {
      const dia = this.obterNomeDiaSemana(paciente.diaSemana);
      const horaInicio = this.formatarHora(paciente.horarioInicio);
      const horaFim = this.formatarHora(paciente.horarioFim);
      return `${dia} - ${horaInicio} às ${horaFim}`;
    }

    // Segunda opção: usar dados do horário disponível (compatibilidade)
    if (paciente.horarioDisponivel) {
      const dia = this.obterNomeDiaSemana(paciente.horarioDisponivel.diaSemana);
      const horaInicio = this.formatarHora(paciente.horarioDisponivel.horaInicio);
      const horaFim = this.formatarHora(paciente.horarioDisponivel.horaFim);
      return `${dia} - ${horaInicio} às ${horaFim}`;
    }

    // Terceira opção: usar informações da primeira sessão se disponível
    if (paciente.sessoes && paciente.sessoes.length > 0) {
      const primeiraSessao = paciente.sessoes[0];
      const data = new Date(primeiraSessao.dataSessao);
      const diaSemana = data.toLocaleDateString('pt-BR', { weekday: 'long' });
      const horaInicio = this.formatarHora(primeiraSessao.horaInicio);
      const horaFim = this.formatarHora(primeiraSessao.horaFim);
      return `${diaSemana} - ${horaInicio} às ${horaFim}`;
    }

    return 'Não definido';
  }

  getInitials(nome: string, sobrenome: string): string {
    const primeiraLetraNome = nome?.charAt(0)?.toUpperCase() || '';
    const primeiraLetraSobrenome = sobrenome?.charAt(0)?.toUpperCase() || '';
    return primeiraLetraNome + primeiraLetraSobrenome;
  }

  limparBusca(): void {
    this.termoBusca = '';
    this.aplicarFiltros();
  }

  // Método para otimização do trackBy no *ngFor
  trackByPatientId(index: number, paciente: Paciente): number {
    return paciente.id;
  }
}
