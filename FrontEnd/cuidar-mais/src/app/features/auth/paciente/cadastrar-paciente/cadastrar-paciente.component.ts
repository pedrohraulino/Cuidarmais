import { Component, OnInit, AfterViewChecked, ChangeDetectorRef } from '@angular/core';
import { CabecalhoModule } from '../../../../cabecalho/cabecalho.module';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { HttpClient, HttpClientModule } from '@angular/common/http';
import { Router, ActivatedRoute } from '@angular/router';
import { AuthService } from '../../auth.service';
import { MessageService } from '../../../../shared/message.service';

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
  ativo: boolean;
  sessoesRestantes?: number;
  sessoesRealizadas?: number;
}

// Interface para criação/atualização de paciente
interface PacienteRequest {
  nome: string;
  sobrenome: string;
  sexo: string;
  dataNascimento: string;
  email: string;
  telefone: string;
  imagemBase64?: string;
  imagemTipo?: string;
  psicologoId: number | null;
  horarioDisponivelId?: number;
  sessoesPorPacote: number;
  ativo: boolean;
}

@Component({
  selector: 'app-cadastrar-paciente',
  templateUrl: './cadastrar-paciente.component.html',
  styleUrl: './cadastrar-paciente.component.scss',
  standalone: true,
  imports: [CabecalhoModule, CommonModule, FormsModule, HttpClientModule]
})
export class CadastrarPacienteComponent implements OnInit, AfterViewChecked {
  // Dados do paciente
  nome = '';
  sobrenome = '';
  sexo = '';
  dataNascimento = '';
  email = '';
  telefone = '';
  imagemBase64 = '';
  imagemTipo = '';

  // Propriedades para upload de imagem
  imagemSelecionada: File | null = null;
  imagemPreview: string | null = null;

  // Dados de agendamento/horário
  diaSemana = '';
  horarioId = '';
  sessoesPorPacote = 1;

  // Controle
  diasSemana: any[] = [];
  horarios: any[] = [];
  configuracoes: any[] = [];
  erro = '';
  sucesso = '';

  // Usuário logado
  psicologoId: number | null = null;

  // Modo de edição
  modoEdicao = false;
  pacienteId: number | null = null;

  // Propriedades para títulos dinâmicos
  get tituloFormulario(): string {
    return this.modoEdicao ? 'Editar Paciente' : 'Cadastrar Paciente';
  }

  get textoBotao(): string {
    return this.modoEdicao ? 'Atualizar Paciente' : 'Cadastrar Paciente';
  }

  // Flag para controlar a verificação do horarioId no DOM
  private checkHorarioIdInView = false;
  private horarioIdToCheck = '';

  constructor(
    private http: HttpClient,
    private router: Router,
    private route: ActivatedRoute,
    private authService: AuthService,
    private messageService: MessageService,
    private cdr: ChangeDetectorRef
  ) {}

  ngAfterViewChecked() {
    // Verifica se precisamos verificar o horarioId no DOM
    if (this.checkHorarioIdInView && this.horarioIdToCheck) {
      console.log('ngAfterViewChecked: Verificando se o horarioId está definido corretamente no DOM');
      const selectElement = document.getElementById('horarioId') as HTMLSelectElement;
      if (selectElement) {
        console.log('ngAfterViewChecked: Elemento select encontrado, valor atual:', selectElement.value);
        console.log('ngAfterViewChecked: Valor esperado:', this.horarioIdToCheck);

        if (selectElement.value !== this.horarioIdToCheck) {
          console.log('ngAfterViewChecked: Valor do select não corresponde ao horarioId, tentando definir novamente');
          selectElement.value = this.horarioIdToCheck;
          this.horarioId = this.horarioIdToCheck;
          this.checkHorarioIdInView = false;
          this.cdr.detectChanges();
        } else {
          console.log('ngAfterViewChecked: Valor do select corresponde ao horarioId, tudo ok');
          this.checkHorarioIdInView = false;
        }
      } else {
        console.log('ngAfterViewChecked: Elemento select não encontrado');
      }
    }
  }

  ngOnInit() {
    // Verifica se o usuário está autenticado
    if (!this.authService.isAuthenticated()) {
      this.router.navigate(['/login']);
      return;
    }

    // Verifica se está em modo de edição através dos parâmetros da rota
    this.route.params.subscribe(params => {
      if (params['id']) {
        this.modoEdicao = true;
        this.pacienteId = +params['id'];
      }
    });

    // Também verifica query params para compatibilidade com código existente
    this.route.queryParams.subscribe(params => {
      if (params['id'] && params['modo'] === 'edicao') {
        this.modoEdicao = true;
        this.pacienteId = +params['id'];
      }
    });

    // Recupera o usuário logado
    const usuario = this.authService.getUser();
    if (usuario) {
      this.psicologoId = usuario.id;
      this.carregarDiasSemana();
      this.carregarConfiguracoes();

      // Se estiver em modo de edição, carrega os dados do paciente
      if (this.modoEdicao && this.pacienteId) {
        this.carregarDadosPaciente(this.pacienteId);
      }
    } else {
      // Tenta buscar as informações do usuário novamente
      this.authService.getUserInfo().subscribe({
        next: (user) => {
          this.psicologoId = user.id;
          this.carregarDiasSemana();
          this.carregarConfiguracoes();

          // Se estiver em modo de edição, carrega os dados do paciente
          if (this.modoEdicao && this.pacienteId) {
            this.carregarDadosPaciente(this.pacienteId);
          }
        },
        error: () => {
          this.erro = 'Usuário não autenticado';
          this.router.navigate(['/login']);
        }
      });
    }
  }

  carregarConfiguracoes() {
    if (!this.psicologoId) return;

    this.http.get<any[]>(`http://localhost:8080/api/configuracao-agenda/psicologo/${this.psicologoId}`).subscribe({
      next: (configuracoes) => {
        this.configuracoes = configuracoes;
      },
      error: (err) => {
        this.erro = 'Erro ao carregar configurações: ' + (err.error?.erro || 'Erro desconhecido');
      }
    });
  }

  carregarDiasSemana() {
    if (!this.psicologoId) return;

    this.http.get<any[]>(`http://localhost:8080/api/configuracao-agenda/psicologo/${this.psicologoId}`).subscribe({
      next: (configuracoes) => {
        // Filtra apenas as configurações ativas
        const configuracoesAtivas = configuracoes.filter(config => config.ativo === true);
        this.diasSemana = configuracoesAtivas.map(config => ({
          valor: config.diaSemana,
          nome: this.obterNomeDiaSemana(config.diaSemana)
        }));
      },
      error: (err) => {
        this.erro = 'Erro ao carregar dias da semana: ' + (err.error?.erro || 'Erro desconhecido');
      }
    });
  }

  onDiaSemanaChange() {
    if (!this.psicologoId || !this.diaSemana) return;

    console.log('Carregando horários para o dia:', this.diaSemana);

    // Limpa o horarioId atual
    this.horarioId = '';

    // Busca horários livres (não ocupados) para o dia selecionado
    this.http.get<any[]>(`http://localhost:8080/api/horarios-disponiveis/psicologo/${this.psicologoId}/livres`).subscribe({
      next: (horarios) => {
        // Filtra apenas os horários do dia selecionado
        const horariosDoDia = horarios.filter(h => h.diaSemana === this.diaSemana);

        console.log('Horários livres carregados:', horariosDoDia);

        this.horarios = horariosDoDia.map(h => ({
          id: h.id,
          horario: `${this.formatarHora(h.horaInicio)} - ${this.formatarHora(h.horaFim)}`
        }));

        console.log('Horários mapeados:', this.horarios);
      },
      error: (err) => {
        console.error('Erro ao carregar horários:', err);
        this.erro = 'Erro ao carregar horários: ' + (err.error?.erro || 'Erro desconhecido');
        this.messageService.setErrorMessage(this.erro);
      }
    });
  }

  cadastrarPaciente() {
    if (!this.validarFormulario()) return;

    const pacienteData: PacienteRequest = {
      nome: this.nome,
      sobrenome: this.sobrenome,
      sexo: this.sexo,
      dataNascimento: this.dataNascimento,
      email: this.email,
      telefone: this.telefone,
      imagemBase64: this.imagemBase64,
      imagemTipo: this.imagemTipo,
      psicologoId: this.psicologoId,
      sessoesPorPacote: this.sessoesPorPacote,
      ativo: true
    };

    // Adiciona dados de horário apenas se foram selecionados
    if (this.diaSemana && this.horarioId) {
      // Adiciona o ID do horário disponível para vincular ao paciente
      pacienteData.horarioDisponivelId = parseInt(this.horarioId);
    }

    this.enviarDadosPaciente(pacienteData);
  }

  private enviarDadosPaciente(pacienteData: PacienteRequest) {
    if (this.modoEdicao && this.pacienteId) {
      // Modo de edição
      this.http.put(`http://localhost:8080/api/pacientes/${this.pacienteId}`, pacienteData).subscribe({
        next: (response) => {
          this.messageService.setSuccessMessage('Paciente atualizado com sucesso!');
          this.router.navigate(['/pacientes']);
        },
        error: (err) => {
          this.erro = 'Erro ao atualizar paciente: ' + (err.error?.erro || 'Erro desconhecido');
          this.messageService.setErrorMessage(this.erro);
        }
      });
    } else {
      // Modo de criação
      this.http.post('http://localhost:8080/api/pacientes', pacienteData).subscribe({
        next: (response) => {
          this.limparFormulario();
          this.messageService.setSuccessMessage('Paciente cadastrado com sucesso!');
          this.router.navigate(['/pacientes']);
        },
        error: (err) => {
          this.erro = 'Erro ao cadastrar paciente: ' + (err.error?.erro || 'Erro desconhecido');
          this.messageService.setErrorMessage(this.erro);
        }
      });
    }
  }

  carregarDadosPaciente(pacienteId: number) {
    this.http.get<Paciente>(`http://localhost:8080/api/pacientes/${pacienteId}`).subscribe({
      next: (paciente) => {
        console.log('Dados do paciente carregados:', paciente);

        // Preenche os campos do formulário
        this.nome = paciente.nome;
        this.sobrenome = paciente.sobrenome;
        this.sexo = paciente.sexo;
        this.dataNascimento = paciente.dataNascimento;
        this.email = paciente.email;
        this.telefone = paciente.telefone;
        this.imagemBase64 = paciente.imagemBase64 || '';
        this.imagemTipo = paciente.imagemTipo || '';
        // Exibe preview se houver imagem
        if (paciente.imagemBase64) {
          this.imagemPreview = `data:${paciente.imagemTipo || 'image/png'};base64,${paciente.imagemBase64}`;
        } else {
          this.imagemPreview = null;
        }
        this.sessoesPorPacote = paciente.sessoesPorPacote;

        // Carrega dados de horário se existirem
        if (paciente.diaSemana) {
          this.diaSemana = paciente.diaSemana;

          // Carrega horários para o dia e seleciona o horário atual
          this.carregarHorariosParaEdicao(paciente);
        }
      },
      error: (err) => {
        this.erro = 'Erro ao carregar dados do paciente: ' + (err.error?.erro || 'Erro desconhecido');
      }
    });
  }

  private carregarHorariosParaEdicao(paciente: Paciente) {
    // Para edição, precisamos incluir tanto horários livres quanto o horário atual do paciente
    this.http.get<any[]>(`http://localhost:8080/api/horarios-disponiveis/psicologo/${this.psicologoId}/ocupados`).subscribe({
      next: (horariosOcupados) => {
        // Busca o horário do paciente atual
        const horarioDoPaciente = horariosOcupados.find(h =>
          h.pacienteId === paciente.id &&
          h.diaSemana === paciente.diaSemana
        );

        // Carrega horários livres
        this.http.get<any[]>(`http://localhost:8080/api/horarios-disponiveis/psicologo/${this.psicologoId}/livres`).subscribe({
          next: (horariosLivres) => {
            // Filtra horários do dia selecionado
            const horariosLivresDoDia = horariosLivres.filter(h => h.diaSemana === paciente.diaSemana);

            // Monta lista de horários incluindo o atual e os livres
            this.horarios = horariosLivresDoDia.map(h => ({
              id: h.id,
              horario: `${this.formatarHora(h.horaInicio)} - ${this.formatarHora(h.horaFim)}`
            }));

            // Adiciona o horário atual do paciente se não estiver na lista
            if (horarioDoPaciente) {
              const horarioExiste = this.horarios.find(h => h.id === horarioDoPaciente.id);
              if (!horarioExiste) {
                this.horarios.push({
                  id: horarioDoPaciente.id,
                  horario: `${this.formatarHora(horarioDoPaciente.horaInicio)} - ${this.formatarHora(horarioDoPaciente.horaFim)}`
                });
              }

              // Seleciona o horário atual
              this.horarioId = horarioDoPaciente.id.toString();
            }

            console.log('Horários carregados para edição:', this.horarios);
            console.log('Horário selecionado:', this.horarioId);
          },
          error: (err) => {
            console.error('Erro ao carregar horários livres:', err);
          }
        });
      },
      error: (err) => {
        console.error('Erro ao carregar horários ocupados:', err);
      }
    });
  }

  voltarParaListaPacientes() {
    this.router.navigate(['/pacientes']);
  }

  validarFormulario(): boolean {
    if (!this.nome.trim()) {
      this.erro = 'Nome é obrigatório';
      return false;
    }
    if (!this.sobrenome.trim()) {
      this.erro = 'Sobrenome é obrigatório';
      return false;
    }
    if (!this.sexo) {
      this.erro = 'Sexo é obrigatório';
      return false;
    }
    if (!this.dataNascimento) {
      this.erro = 'Data de nascimento é obrigatória';
      return false;
    }
    if (!this.email.trim()) {
      this.erro = 'Email é obrigatório';
      return false;
    }
    if (!this.telefone.trim()) {
      this.erro = 'Telefone é obrigatório';
      return false;
    }

    // Validações específicas para novo paciente
    if (!this.modoEdicao) {
      if (!this.diaSemana) {
        this.erro = 'Dia de atendimento é obrigatório';
        return false;
      }
      if (!this.horarioId) {
        this.erro = 'Horário é obrigatório';
        return false;
      }
      if (!this.sessoesPorPacote || this.sessoesPorPacote < 1) {
        this.erro = 'Total de sessões deve ser maior que zero';
        return false;
      }
    }

    this.erro = '';
    return true;
  }

  limparFormulario() {
    this.nome = '';
    this.sobrenome = '';
    this.sexo = '';
    this.dataNascimento = '';
    this.email = '';
    this.telefone = '';
    this.imagemBase64 = '';
    this.imagemTipo = '';
    this.diaSemana = '';
    this.horarioId = '';
    this.sessoesPorPacote = 1;
    this.horarios = [];
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

  // Método para lidar com o upload de imagem
  onImagemSelecionada(event: any) {
    const file = event.target.files && event.target.files[0];
    this.imagemSelecionada = file;
    if (file) {
      const reader = new FileReader();
      reader.onload = (e: any) => {
        this.imagemPreview = e.target.result;
        this.imagemBase64 = e.target.result.split(',')[1];
        this.imagemTipo = file.type;
      };
      reader.readAsDataURL(file);
    } else {
      this.imagemPreview = null;
      this.imagemBase64 = '';
      this.imagemTipo = '';
      this.imagemSelecionada = null;
    }
  }
}
