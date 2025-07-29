import { Component, OnInit } from '@angular/core';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { AuthService } from '../../../auth/auth.service';

interface Sessao {
  id: number;
  nomePaciente: string;
  pacienteSobrenome?: string;
  status: string;
  horaInicio: string;
  horaFim: string;
  pacienteImagem: string;
  pacienteEmail?: string;
  pacienteTelefone?: string;
  presenca: boolean;
}

@Component({
  selector: 'app-agenda-listagem',
  templateUrl: './agenda-listagem.component.html',
  styleUrls: ['./agenda-listagem.component.scss']
})
export class AgendaListagemComponent implements OnInit {
  sessoes: Sessao[] = [];
  selectedDate: Date = new Date();
  psicologoId: number | null = null;

  constructor(private http: HttpClient, private authService: AuthService) {}

  ngOnInit() {
    const user = this.authService.getUser();
    this.psicologoId = user?.id || null;
    this.loadSessoes(this.selectedDate);
  }

  loadSessoes(date: Date) {
    if (!this.psicologoId) return;
    const token = this.authService.getToken();
    const headers = new HttpHeaders({ 'Authorization': `Bearer ${token}` });
    const dataStr = date.toISOString().split('T')[0];
    this.http.get<Sessao[]>(`http://localhost:8080/api/sessoes/psicologo/${this.psicologoId}/data/${dataStr}`, { headers })
      .subscribe(sessoes => this.sessoes = sessoes);
  }

  onDateChange(date: Date) {
    this.selectedDate = date;
    this.loadSessoes(date);
  }

  confirmarSessao(sessao: Sessao) {
    // Chamada para API para confirmar sessão
    const token = this.authService.getToken();
    const headers = new HttpHeaders({ 'Authorization': `Bearer ${token}` });
    this.http.put(`http://localhost:8080/api/sessoes/${sessao.id}/confirmar`, {}, { headers })
      .subscribe(() => this.loadSessoes(this.selectedDate));
  }

  marcarFaltou(sessao: Sessao) {
    // Chamada para API para marcar falta
    const token = this.authService.getToken();
    const headers = new HttpHeaders({ 'Authorization': `Bearer ${token}` });
    this.http.put(`http://localhost:8080/api/sessoes/${sessao.id}/faltou`, {}, { headers })
      .subscribe(() => this.loadSessoes(this.selectedDate));
  }

  cancelarSessao(sessao: Sessao) {
    // Chamada para API para cancelar sessão
    const token = this.authService.getToken();
    const headers = new HttpHeaders({ 'Authorization': `Bearer ${token}` });
    this.http.put(`http://localhost:8080/api/sessoes/${sessao.id}/cancelar`, {}, { headers })
      .subscribe(() => this.loadSessoes(this.selectedDate));
  }

  getImagemSrc(imagem: string): string {
    if (!imagem) return '';
    // Detecta SVG pelo início do base64
    if (imagem.trim().startsWith('PD94bWwgdmVyc2lvbj0iMS4wIi') || imagem.includes('<svg')) {
      return 'data:image/svg+xml;base64,' + imagem;
    }
    // Se já tem prefixo, retorna direto
    if (imagem.startsWith('data:image')) {
      return imagem;
    }
    // Default para PNG
    return 'data:image/png;base64,' + imagem;
  }

  getWhatsappLink(telefone: string): string {
    if (!telefone) return '';
    // Remove caracteres não numéricos
    const numeroLimpo = telefone.replace(/\D/g, '');
    // Garante que o número tenha DDI (Brasil = 55)
    let numeroComDDI = numeroLimpo;
    if (numeroLimpo.length === 11) {
      // Se não começa com 55, adiciona
      if (!numeroLimpo.startsWith('55')) {
        numeroComDDI = '55' + numeroLimpo;
      }
    }
    return `https://wa.me/${numeroComDDI}`;
  }
}
