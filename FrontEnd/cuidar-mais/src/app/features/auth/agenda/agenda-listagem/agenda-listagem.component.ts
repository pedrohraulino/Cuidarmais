import { Component, OnInit } from '@angular/core';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { AuthService } from '../../../auth/auth.service';

interface Sessao {
  id: number;
  pacienteNome: string;
  pacienteImagem: string;
  status: string;
  horarioInicio: string;
  horarioFim: string;
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
}
