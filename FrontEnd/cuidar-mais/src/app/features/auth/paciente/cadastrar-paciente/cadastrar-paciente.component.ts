
import { Component, OnInit, AfterViewChecked, ChangeDetectorRef } from '@angular/core';
import { CabecalhoModule } from '../../../../cabecalho/cabecalho.module';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { HttpClient, HttpClientModule } from '@angular/common/http';
import { Router, ActivatedRoute } from '@angular/router';
import { AuthService } from '../../auth.service';
import { MessageService } from '../../../../shared/message.service';

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
  agendamentos?: Agendamento[];
}

// Interface estendida para criação de paciente
interface PacienteCreate extends Omit<Paciente, 'id'> {
  id?: number;
  psicologoId: number | null;
  totalSessoes: number;
  ativo: boolean;
  diaSemana?: string;
  horarioDisponivelId?: string;
  dataInicio?: string;
  // Campos para rastreamento de alterações em edição
  diaSemanaAntigo?: string;
  horarioDisponivelIdAntigo?: string;
  serieId?: string;
}

@Component({
  selector: 'app-cadastrar-paciente',
  templateUrl: './cadastrar-paciente.component.html',
  styleUrl: './cadastrar-paciente.component.scss',
  standalone: true,
  imports: [CabecalhoModule, CommonModule, FormsModule, HttpClientModule]
})
export class CadastrarPacienteComponent implements OnInit, AfterViewChecked {
  // Paciente
  nome = '';
  sobrenome = '';
  sexo = '';
  dataNascimento = '';
  email = '';
  telefone = '';
  imagemBase64 = '';
  imagemTipo = '';

  // Agendamento
  diaSemana = '';
  horarioId = '';
  totalSessoes = 1;

  // Rastreamento de alterações
  diaSemanaAntigo = '';
  horarioIdAntigo = '';
  serieId = '';

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

          // Marca como verificado para evitar loop infinito
          this.checkHorarioIdInView = false;

          // Força a detecção de mudanças
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

  // ... resto dos métodos permanecem iguais
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

    // Se estamos em modo de edição e temos um horarioIdAntigo, vamos usá-lo
    if (this.modoEdicao && this.horarioIdAntigo) {
      console.log('Modo edição com horarioIdAntigo:', this.horarioIdAntigo);

      // Primeiro carregamos os horários disponíveis para o dia selecionado
      this.http.get<any[]>(`http://localhost:8080/api/horarios-disponiveis/psicologo/${this.psicologoId}/dia-semana/${this.diaSemana}`).subscribe({
        next: (horarios) => {
          console.log('Horários carregados em modo edição:', horarios);

          // Mapeia os horários para o formato esperado
          this.horarios = horarios.map(h => ({
            id: h.id,
            horario: `${this.formatarHora(h.horaInicio)} - ${this.formatarHora(h.horaFim)}`
          }));

          console.log('Horários mapeados em modo edição:', this.horarios);

          // Se estamos em modo de edição e temos um pacienteId, buscamos o horário atual
          if (this.modoEdicao && this.pacienteId) {
            this.buscarHorarioAtual(this.pacienteId).subscribe({
              next: (horarioAtual) => {
                console.log('Horário atual recebido do backend em onDiaSemanaChange:', horarioAtual);

                // Inclui o horário atual na lista de horários
                this.incluirHorarioAtualNoSelect(horarioAtual);

                // Após incluir o horário atual, definimos o horário
                setTimeout(() => {
                  console.log('Definindo horarioId em modo edição:', this.horarioIdAntigo);
                  this.horarioId = this.horarioIdAntigo;

                  // Configura as flags para verificar o horarioId no DOM no próximo ciclo de detecção de mudanças
                  this.horarioIdToCheck = this.horarioIdAntigo;
                  this.checkHorarioIdInView = true;

                  // Força a detecção de mudanças
                  this.cdr.detectChanges();

                  // Tenta definir o valor diretamente no elemento select
                  const selectElement = document.getElementById('horarioId') as HTMLSelectElement;
                  if (selectElement) {
                    console.log('Elemento select encontrado, definindo valor diretamente:', this.horarioIdAntigo);
                    selectElement.value = this.horarioIdAntigo;
                  }
                }, 100);
              },
              error: (err) => {
                console.error('Erro ao buscar horário atual em onDiaSemanaChange:', err);

                // Mesmo sem o horário atual, tentamos definir o horário antigo
                setTimeout(() => {
                  this.horarioId = this.horarioIdAntigo;
                  this.horarioIdToCheck = this.horarioIdAntigo;
                  this.checkHorarioIdInView = true;
                  this.cdr.detectChanges();
                }, 100);
              }
            });
          } else {
            // Se não temos um pacienteId, apenas definimos o horário antigo
            setTimeout(() => {
              console.log('Definindo horarioId em modo edição:', this.horarioIdAntigo);
              this.horarioId = this.horarioIdAntigo;

              // Configura as flags para verificar o horarioId no DOM no próximo ciclo de detecção de mudanças
              this.horarioIdToCheck = this.horarioIdAntigo;
              this.checkHorarioIdInView = true;

              // Força a detecção de mudanças
              this.cdr.detectChanges();

              // Tenta definir o valor diretamente no elemento select
              const selectElement = document.getElementById('horarioId') as HTMLSelectElement;
              if (selectElement) {
                console.log('Elemento select encontrado, definindo valor diretamente:', this.horarioIdAntigo);
                selectElement.value = this.horarioIdAntigo;
              }
            }, 100);
          }
        },
        error: (err) => {
          console.error('Erro ao carregar horários em modo edição:', err);
          this.erro = 'Erro ao carregar horários: ' + (err.error?.erro || 'Erro desconhecido');
          this.messageService.setErrorMessage(this.erro);
        }
      });
      return;
    }

    // Caso contrário, seguimos com o comportamento padrão
    // Limpa o horarioId atual para evitar seleções incorretas
    this.horarioId = '';

    this.http.get<any[]>(`http://localhost:8080/api/horarios-disponiveis/psicologo/${this.psicologoId}/dia-semana/${this.diaSemana}`).subscribe({
      next: (horarios) => {
        console.log('Horários carregados:', horarios);

        this.horarios = horarios.map(h => ({
          id: h.id,
          horario: `${this.formatarHora(h.horaInicio)} - ${this.formatarHora(h.horaFim)}`
        }));

        console.log('Horários mapeados:', this.horarios);

        // Se estamos em modo de edição e temos um pacienteId, buscamos o horário atual
        if (this.modoEdicao && this.pacienteId) {
          this.buscarHorarioAtual(this.pacienteId).subscribe({
            next: (horarioAtual) => {
              console.log('Horário atual recebido do backend em onDiaSemanaChange (comportamento padrão):', horarioAtual);

              // Inclui o horário atual na lista de horários
              this.incluirHorarioAtualNoSelect(horarioAtual);
            },
            error: (err) => {
              console.error('Erro ao buscar horário atual em onDiaSemanaChange (comportamento padrão):', err);
            }
          });
        }

        // Se estamos em modo de edição e temos apenas um horário disponível, selecionamos automaticamente
        if (this.modoEdicao && this.horarios.length === 1) {
          console.log('Apenas um horário disponível, selecionando automaticamente:', this.horarios[0].id);

          // Configura as flags para verificar o horarioId no DOM no próximo ciclo de detecção de mudanças
          this.horarioIdToCheck = this.horarios[0].id.toString();
          this.horarioId = this.horarioIdToCheck;
          this.checkHorarioIdInView = true;

          // Força a detecção de mudanças
          this.cdr.detectChanges();
        }
      },
      error: (err) => {
        console.error('Erro ao carregar horários:', err);
        this.erro = 'Erro ao carregar horários: ' + (err.error?.erro || 'Erro desconhecido');
        this.messageService.setErrorMessage(this.erro);
      }
    });
  }

  // Método para carregar horários e retornar um Observable
  carregarHorarios(diaSemana: string) {
    console.log('Carregando horários para o dia (método Observable):', diaSemana);

    return this.http.get<any[]>(`http://localhost:8080/api/horarios-disponiveis/psicologo/${this.psicologoId}/dia-semana/${diaSemana}`);
  }

  // Método para buscar o horário atual de um paciente
  buscarHorarioAtual(pacienteId: number) {
    console.log('Buscando horário atual para o paciente:', pacienteId);

    return this.http.get<any>(`http://localhost:8080/api/pacientes/${pacienteId}/horario-atual`);
  }

  // Método para incluir o horário atual no select de horários
  incluirHorarioAtualNoSelect(horarioAtual: any) {
    console.log('Incluindo horário atual no select:', horarioAtual);

    if (!horarioAtual || !horarioAtual.id) {
      console.log('Horário atual inválido, não será incluído no select');
      return;
    }

    // Verifica se o dia atual selecionado corresponde ao dia do horário atual
    if (horarioAtual.diaSemana && this.diaSemana !== horarioAtual.diaSemana) {
      console.log('Dia selecionado não corresponde ao dia do horário atual, não será incluído no select');
      return;
    }

    // Verifica se o horário já existe na lista
    const horarioExistente = this.horarios.find(h => h.id === horarioAtual.id);

    if (!horarioExistente) {
      // Adiciona o horário atual à lista de horários
      this.horarios.push({
        id: horarioAtual.id,
        horario: horarioAtual.horario || `${this.formatarHora(horarioAtual.horaInicio)} - ${this.formatarHora(horarioAtual.horaFim)}`
      });

      console.log('Horário atual adicionado à lista:', this.horarios);
    } else {
      console.log('Horário atual já existe na lista');
    }
  }

  // Método para carregar horários e definir o horarioId
  carregarHorariosEDefinirHorario(diaSemana: string, horarioDisponivelId: string) {
    console.log('Carregando horários e definindo horário para o dia:', diaSemana, 'horarioId:', horarioDisponivelId);

    return new Promise<void>((resolve, reject) => {
      this.http.get<any[]>(`http://localhost:8080/api/horarios-disponiveis/psicologo/${this.psicologoId}/dia-semana/${diaSemana}`)
        .subscribe({
          next: (horarios) => {
            console.log('Horários carregados para definir horário:', horarios);

            // Mapeia os horários para o formato esperado
            this.horarios = horarios.map(h => ({
              id: h.id,
              horario: `${this.formatarHora(h.horaInicio)} - ${this.formatarHora(h.horaFim)}`
            }));

            console.log('Horários mapeados para definir horário:', this.horarios);

            // Força a atualização do DOM antes de definir o horarioId
            setTimeout(() => {
              console.log('Definindo horarioId após timeout:', horarioDisponivelId);
              this.horarioId = horarioDisponivelId;

              // Configura as flags para verificar o horarioId no DOM no próximo ciclo de detecção de mudanças
              this.horarioIdToCheck = horarioDisponivelId;
              this.checkHorarioIdInView = true;

              // Força a detecção de mudanças
              this.cdr.detectChanges();

              // Resolve a Promise após um breve delay para dar tempo ao Angular de processar as mudanças
              setTimeout(() => {
                resolve();
              }, 200);
            }, 500);
          },
          error: (err) => {
            console.error('Erro ao carregar horários para definir horário:', err);
            this.erro = 'Erro ao carregar horários: ' + (err.error?.erro || 'Erro desconhecido');
            this.messageService.setErrorMessage(this.erro);
            reject(err);
          }
        });
    });
  }

  cadastrarPaciente() {
    if (!this.validarFormulario()) return;

    const pacienteBase = {
      nome: this.nome,
      sobrenome: this.sobrenome,
      sexo: this.sexo,
      dataNascimento: this.dataNascimento,
      email: this.email,
      telefone: this.telefone,
      imagemBase64: this.imagemBase64,
      imagemTipo: this.imagemTipo,
      psicologoId: this.psicologoId,
      totalSessoes: this.totalSessoes,
      ativo: true
    };

    if (this.modoEdicao && this.pacienteId) {
      // Modo de edição - atualiza o paciente existente
      const pacienteAtualizado: PacienteCreate = {
        ...pacienteBase,
        diaSemana: this.diaSemana,
        horarioDisponivelId: this.horarioId,
        diaSemanaAntigo: this.diaSemanaAntigo,
        horarioDisponivelIdAntigo: this.horarioIdAntigo,
        serieId: this.serieId
      };

      this.http.put(`http://localhost:8080/api/pacientes/${this.pacienteId}`, pacienteAtualizado).subscribe({
        next: (response) => {
          // Atualiza os valores antigos para refletir a mudança
          this.diaSemanaAntigo = this.diaSemana;
          this.horarioIdAntigo = this.horarioId;

          // Define a mensagem de sucesso no serviço de mensagens
          this.messageService.setSuccessMessage('Paciente atualizado com sucesso!');

          // Navega para a lista de pacientes imediatamente
          this.router.navigate(['/pacientes']);
        },
        error: (err) => {
          this.erro = 'Erro ao atualizar paciente: ' + (err.error?.erro || 'Erro desconhecido');
          this.messageService.setErrorMessage(this.erro);
        }
      });
    } else {
      // Modo de criação - adiciona campos específicos para criação
      const paciente: PacienteCreate = {
        ...pacienteBase,
        diaSemana: this.diaSemana,
        horarioDisponivelId: this.horarioId,
        dataInicio: new Date().toISOString().split('T')[0] // Data atual como início
      };

      this.http.post('http://localhost:8080/api/pacientes', paciente).subscribe({
        next: (response) => {
          this.limparFormulario();

          // Define a mensagem de sucesso no serviço de mensagens
          this.messageService.setSuccessMessage('Paciente cadastrado com sucesso!');

          // Navega para a lista de pacientes imediatamente
          this.router.navigate(['/pacientes']);
        },
        error: (err) => {
          this.erro = 'Erro ao cadastrar paciente: ' + (err.error?.erro || 'Erro desconhecido');
          this.messageService.setErrorMessage(this.erro);
        }
      });
    }
  }

  verificarPodeResetarConfiguracao(configuracaoId: number) {
    return this.http.get<any>(`http://localhost:8080/api/configuracao-agenda/${configuracaoId}/pode-excluir`);
  }

  carregarDadosPaciente(pacienteId: number) {
    this.http.get<any>(`http://localhost:8080/api/pacientes/${pacienteId}`).subscribe({
      next: (paciente) => {
        console.log('Dados do paciente carregados:', paciente);

        // Preenche os campos do formulário com os dados do paciente
        this.nome = paciente.nome;
        this.sobrenome = paciente.sobrenome;
        this.sexo = paciente.sexo;
        this.dataNascimento = paciente.dataNascimento;
        this.email = paciente.email;
        this.telefone = paciente.telefone;
        this.imagemBase64 = paciente.imagemBase64;
        this.imagemTipo = paciente.imagemTipo;
        this.totalSessoes = paciente.totalSessoes;

        // Armazena o serieId para atualização dos agendamentos
        if (paciente.serieId) {
          this.serieId = paciente.serieId;
        }

        // Carrega os dados de agendamento
        if (paciente.diaSemana) {
          console.log('Dia da semana do paciente:', paciente.diaSemana);

          // Definimos o dia da semana e o dia da semana antigo
          this.diaSemanaAntigo = paciente.diaSemanaAntigo || paciente.diaSemana;
          this.diaSemana = paciente.diaSemana;

          // Armazena o serieId para rastreamento
          if (paciente.serieId) {
            this.serieId = paciente.serieId;
          }

          // Busca o horário atual diretamente do backend
          this.buscarHorarioAtual(pacienteId).subscribe({
            next: (horarioAtual) => {
              console.log('Horário atual recebido do backend:', horarioAtual);

              // Armazena o horarioIdAntigo
              this.horarioIdAntigo = horarioAtual.id.toString();

              // Carrega os horários disponíveis para o dia selecionado
              this.http.get<any[]>(`http://localhost:8080/api/horarios-disponiveis/psicologo/${this.psicologoId}/dia-semana/${this.diaSemana}`).subscribe({
                next: (horarios) => {
                  console.log('Horários carregados na inicialização:', horarios);

                  // Mapeia os horários para o formato esperado
                  this.horarios = horarios.map(h => ({
                    id: h.id,
                    horario: `${this.formatarHora(h.horaInicio)} - ${this.formatarHora(h.horaFim)}`
                  }));

                  console.log('Horários mapeados na inicialização:', this.horarios);

                  // Inclui o horário atual na lista de horários
                  this.incluirHorarioAtualNoSelect(horarioAtual);

                  // Define o horário atual
                  this.horarioId = horarioAtual.id.toString();

                  // Força a detecção de mudanças
                  this.cdr.detectChanges();

                  // Verifica se o horário está corretamente selecionado no DOM
                  setTimeout(() => {
                    const selectElement = document.getElementById('horarioId') as HTMLSelectElement;
                    if (selectElement && selectElement.value !== this.horarioId) {
                      console.log('Definindo horário diretamente no elemento select:', this.horarioId);
                      selectElement.value = this.horarioId;

                      // Dispara um evento de change para garantir que o Angular detecte a mudança
                      const event = new Event('change', { bubbles: true });
                      selectElement.dispatchEvent(event);
                    }
                  }, 100);
                },
                error: (err) => {
                  console.error('Erro ao carregar horários na inicialização:', err);
                  this.erro = 'Erro ao carregar horários: ' + (err.error?.erro || 'Erro desconhecido');
                  this.messageService.setErrorMessage(this.erro);
                }
              });
            },
            error: (err) => {
              console.error('Erro ao buscar horário atual:', err);
              this.erro = 'Erro ao buscar horário atual: ' + (err.error?.erro || 'Erro desconhecido');
              this.messageService.setErrorMessage(this.erro);

              // Fallback para o método antigo se o endpoint não existir
              const horarioDisponivelId = paciente.horarioDisponivelId;
              if (horarioDisponivelId) {
                this.horarioIdAntigo = paciente.horarioDisponivelIdAntigo?.toString() || horarioDisponivelId.toString();
                this.carregarHorariosEDefinirHorario(this.diaSemana, horarioDisponivelId.toString())
                  .catch(error => console.error('Erro no fallback:', error));
              }
            }
          });
        }
      },
      error: (err) => {
        this.erro = 'Erro ao carregar dados do paciente: ' + (err.error?.erro || 'Erro desconhecido');
      }
    });
  }

  voltarParaListaPacientes() {
    this.router.navigate(['/pacientes']);
  }

  resetarConfiguracao(configuracaoId: number) {
    this.verificarPodeResetarConfiguracao(configuracaoId).subscribe({
      next: (response) => {
        if (response.podeExcluir) {
          this.http.delete(`http://localhost:8080/api/configuracao-agenda/${configuracaoId}`).subscribe({
            next: () => {
              this.sucesso = 'Configuração resetada com sucesso!';
              this.carregarConfiguracoes();
              this.carregarDiasSemana();
            },
            error: (err) => {
              this.erro = 'Erro ao resetar configuração: ' + (err.error?.erro || 'Erro desconhecido');
            }
          });
        } else {
          this.erro = 'Não é possível resetar esta configuração porque existem pacientes vinculados aos horários.';
        }
      },
      error: (err) => {
        this.erro = 'Erro ao verificar se configuração pode ser resetada: ' + (err.error?.erro || 'Erro desconhecido');
      }
    });
  }

  validarFormulario(): boolean {
    if (!this.nome) {
      this.erro = 'Nome é obrigatório';
      return false;
    }
    if (!this.sobrenome) {
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

    // Validações específicas para o modo de criação
    if (!this.modoEdicao) {
      if (!this.diaSemana) {
        this.erro = 'Dia de atendimento é obrigatório';
        return false;
      }
      if (!this.horarioId) {
        this.erro = 'Horário é obrigatório';
        return false;
      }
      if (!this.totalSessoes || this.totalSessoes < 1) {
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
    this.totalSessoes = 1;
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
    const file = event.target.files[0];
    if (file) {
      this.imagemTipo = file.type;
      const reader = new FileReader();
      reader.onload = (e: any) => {
        const base64 = e.target.result;
        // Remove o prefixo "data:image/jpeg;base64," para armazenar apenas os dados
        this.imagemBase64 = base64.split(',')[1];
      };
      reader.readAsDataURL(file);
    }
  }
}
