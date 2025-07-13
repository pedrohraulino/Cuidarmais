import { Component, NgModule, OnInit } from '@angular/core';
import { CabecalhoModule } from '../../../../cabecalho/cabecalho.module';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { HttpClient, HttpClientModule } from '@angular/common/http';

interface ConfiguracaoHorario {
  id?: number;
  psicologoId?: number;
  diaSemana: string; // DayOfWeek enum as string: "MONDAY", "TUESDAY", etc.
  nomeDiaSemana?: string; // Nome em português do dia da semana
  ativo: boolean;
  horarioInicio: string; // Format: "HH:MM"
  horarioFim: string; // Format: "HH:MM"
  inicioPausa: string; // Format: "HH:MM"
  fimPausa: string; // Format: "HH:MM"
  intervaloMinutos: number;
  dataAtualizacao?: string; // ISO date string
}


@Component({
  selector: 'app-configurar',
  templateUrl: './configurar.component.html',
  styleUrls: ['./configurar.component.scss'],
  standalone: true,
  imports: [CabecalhoModule, CommonModule, FormsModule, HttpClientModule]
})

export class ConfigurarComponent implements OnInit {
  diasSemana = [
    { id: 'MONDAY', nome: 'Segunda-feira' },
    { id: 'TUESDAY', nome: 'Terça-feira' },
    { id: 'WEDNESDAY', nome: 'Quarta-feira' },
    { id: 'THURSDAY', nome: 'Quinta-feira' },
    { id: 'FRIDAY', nome: 'Sexta-feira' },
    { id: 'SATURDAY', nome: 'Sábado' },
    { id: 'SUNDAY', nome: 'Domingo' }
  ];

  configuracoes: ConfiguracaoHorario[] = [];
  // Cache para armazenar as configurações por dia da semana
  configuracoesCache: { [key: string]: ConfiguracaoHorario } = {};

  carregandoConfiguracoes = false;
  salvando = false;

  mensagemErro = '';
  mensagemSucesso = '';
  resetando = false;

  constructor(private http: HttpClient) {}

  ngOnInit() {
    this.carregarConfiguracoes();
    // Não carregamos mais os horários disponíveis, pois essa seção foi removida
  }

  carregarConfiguracoes() {
    this.carregandoConfiguracoes = true;
    this.mensagemErro = '';

    // Limpa o cache de configurações
    this.configuracoesCache = {};

    this.http.get<ConfiguracaoHorario[]>('http://localhost:8080/api/configuracao-agenda')
      .subscribe({
        next: (data) => {
          this.configuracoes = data;

          // Pré-popula o cache com as configurações carregadas
          data.forEach(config => {
            this.configuracoesCache[config.diaSemana] = config;
          });

          this.carregandoConfiguracoes = false;
        },
        error: (error) => {
          console.error('Erro ao carregar configurações:', error);
          this.carregandoConfiguracoes = false;

          if (error.status === 404) {
            // Inicializar configurações padrão se não houver dados
            this.inicializarConfiguracoes();
          } else {
            this.mensagemErro = 'Erro ao carregar configurações. Tente novamente mais tarde.';
          }
        }
      });
  }

  inicializarConfiguracoes() {
    this.carregandoConfiguracoes = true;
    this.mensagemErro = '';

    this.http.get('http://localhost:8080/api/configuracao-agenda/inicializar')
      .subscribe({
        next: () => {
          console.log('Configurações inicializadas com sucesso');
          this.mensagemSucesso = 'Configurações inicializadas com sucesso!';
          this.carregarConfiguracoes();
        },
        error: (error) => {
          console.error('Erro ao inicializar configurações:', error);
          this.carregandoConfiguracoes = false;
          this.mensagemErro = 'Erro ao inicializar configurações. Tente novamente mais tarde.';
        }
      });
  }


  getConfiguracao(diaSemana: string): ConfiguracaoHorario {
    // Verifica se já existe no cache
    if (this.configuracoesCache[diaSemana]) {
      return this.configuracoesCache[diaSemana];
    }

    // Procura na lista de configurações
    const config = this.configuracoes.find(c => c.diaSemana === diaSemana);

    if (config) {
      // Armazena no cache e retorna
      this.configuracoesCache[diaSemana] = config;
      return config;
    } else {
      // Cria uma configuração padrão, armazena no cache e retorna
      const defaultConfig: ConfiguracaoHorario = {
        diaSemana: diaSemana,
        ativo: diaSemana !== 'SATURDAY' && diaSemana !== 'SUNDAY', // Desativa sábado e domingo por padrão
        horarioInicio: '08:00',
        horarioFim: '18:00',
        inicioPausa: '12:00',
        fimPausa: '13:00',
        intervaloMinutos: 60
      };
      this.configuracoesCache[diaSemana] = defaultConfig;
      return defaultConfig;
    }
  }

  validarConfiguracao(configuracao: ConfiguracaoHorario): string | null {
    // Verificar campos obrigatórios
    if (!configuracao.horarioInicio) {
      return 'O campo Horário Início é obrigatório.';
    }
    if (!configuracao.horarioFim) {
      return 'O campo Horário Fim é obrigatório.';
    }
    if (configuracao.intervaloMinutos === undefined || configuracao.intervaloMinutos === null) {
      return 'O campo Intervalo entre atendimentos é obrigatório.';
    }

    // Verificar se horário início é menor que horário fim
    if (configuracao.horarioInicio >= configuracao.horarioFim) {
      return 'O Horário Início deve ser menor que o Horário Fim.';
    }

    // Verificar se horário de pausa início é menor que horário de pausa fim (se ambos estiverem preenchidos)
    if (configuracao.inicioPausa && configuracao.fimPausa) {
      if (configuracao.inicioPausa >= configuracao.fimPausa) {
        return 'O Início da Pausa deve ser menor que a Volta da Pausa.';
      }
    }

    return null; // Sem erros
  }

  salvarConfiguracao(configuracao: ConfiguracaoHorario) {
    this.mensagemErro = '';
    this.mensagemSucesso = '';

    // Validar configuração antes de salvar
    const erro = this.validarConfiguracao(configuracao);
    if (erro) {
      this.mensagemErro = erro;
      return;
    }

    this.salvando = true;
    const config = { ...configuracao };
    const diaIndex = this.diasSemana.findIndex(d => d.id === config.diaSemana);

    if (config.id) {
      // Atualizar configuração existente
      this.http.put<ConfiguracaoHorario>(`http://localhost:8080/api/configuracao-agenda/${config.id}`, config)
        .subscribe({
          next: (configuracaoAtualizada) => {
            console.log('Configuração atualizada com sucesso', configuracaoAtualizada);

            // Atualizar a configuração na lista local
            const index = this.configuracoes.findIndex(c => c.id === configuracaoAtualizada.id);
            if (index !== -1) {
              this.configuracoes[index] = configuracaoAtualizada;
            }

            // Atualizar o cache
            this.configuracoesCache[configuracaoAtualizada.diaSemana] = configuracaoAtualizada;

            this.mensagemSucesso = 'Configuração atualizada com sucesso!';
            this.salvando = false;
          },
          error: (error) => {
            console.error('Erro ao atualizar configuração:', error);
            this.salvando = false;
            this.mensagemErro = 'Erro ao atualizar configuração. Tente novamente mais tarde.';
          }
        });
    } else {
      // Criar nova configuração
      this.http.post<ConfiguracaoHorario>('http://localhost:8080/api/configuracao-agenda', config)
        .subscribe({
          next: (configuracaoNova) => {
            console.log('Configuração criada com sucesso', configuracaoNova);

            // Adicionar a nova configuração à lista local
            this.configuracoes.push(configuracaoNova);

            // Atualizar o cache
            this.configuracoesCache[configuracaoNova.diaSemana] = configuracaoNova;

            this.mensagemSucesso = 'Configuração criada com sucesso!';
            this.salvando = false;
          },
          error: (error) => {
            console.error('Erro ao criar configuração:', error);
            this.salvando = false;
            this.mensagemErro = 'Erro ao criar configuração. Tente novamente mais tarde.';
          }
        });
    }
  }

  // Formata a data para exibição no formato brasileiro
  formatarData(dataString?: string): string {
    if (!dataString) return '';

    const data = new Date(dataString);

    // Formatar data e hora no formato brasileiro
    return data.toLocaleString('pt-BR', {
      day: '2-digit',
      month: '2-digit',
      year: 'numeric',
      hour: '2-digit',
      minute: '2-digit'
    });
  }

  verificarPodeResetarConfiguracao(configuracaoId: number) {
    return this.http.get<any>(`http://localhost:8080/api/configuracao-agenda/${configuracaoId}/pode-excluir`);
  }

  resetarConfiguracao(configuracaoId: number) {
    this.resetando = true;
    this.mensagemErro = '';
    this.mensagemSucesso = '';

    // Encontra a configuração pelo ID para obter o dia da semana
    const configuracao = this.configuracoes.find(c => c.id === configuracaoId);
    if (!configuracao) {
      this.mensagemErro = 'Configuração não encontrada';
      this.resetando = false;
      return;
    }

    const diaSemana = configuracao.diaSemana;

    this.verificarPodeResetarConfiguracao(configuracaoId).subscribe({
      next: (response) => {
        if (response.podeExcluir) {
          this.http.delete(`http://localhost:8080/api/configuracao-agenda/${configuracaoId}`).subscribe({
            next: () => {
              this.mensagemSucesso = 'Configuração resetada com sucesso!';

              // Remove a configuração do cache
              if (this.configuracoesCache[diaSemana]) {
                delete this.configuracoesCache[diaSemana];
              }

              // Recarrega as configurações do servidor
              this.carregarConfiguracoes();
              this.resetando = false;
            },
            error: (err) => {
              this.mensagemErro = 'Erro ao resetar configuração: ' + (err.error?.erro || 'Erro desconhecido');
              this.resetando = false;
            }
          });
        } else {
          this.mensagemErro = 'Não é possível resetar esta configuração porque existem pacientes vinculados aos horários.';
          this.resetando = false;
        }
      },
      error: (err) => {
        this.mensagemErro = 'Erro ao verificar se configuração pode ser resetada: ' + (err.error?.erro || 'Erro desconhecido');
        this.resetando = false;
      }
    });
  }
}
