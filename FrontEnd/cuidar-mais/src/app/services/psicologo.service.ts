import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';

export interface PsicologoDTO {
  id: number;
  nome: string;
  email: string;
  perfis: string[];
}

@Injectable({
  providedIn: 'root'
})
export class PsicologoService {
  private apiUrl = 'http://localhost:8080/psicologo/me';

  constructor(private http: HttpClient) {}

  getDadosPsicologo(): Observable<PsicologoDTO> {
    return this.http.get<PsicologoDTO>(this.apiUrl);
  }
}
