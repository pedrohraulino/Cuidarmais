import { HttpClient, HttpHeaders } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { AuthService } from '../features/auth/auth.service';

export interface PsicologoDTO {
  id: number;
  nome: string;
  email: string;
  perfis: string[];
  crp: string;
  imagemDataUrl: string;
}

@Injectable({
  providedIn: 'root'
})
export class PsicologoService {
  private apiUrl = 'http://localhost:8080/psicologo/me';

  constructor(
    private http: HttpClient,
    private authService: AuthService
  ) {}

  getDadosPsicologo(): Observable<PsicologoDTO> {
    const token = this.authService.getToken();
    const headers = new HttpHeaders({
      'Authorization': `Bearer ${token}`
    });

    console.log('Token being used for getting psicologo data:', token);

    return this.http.get<PsicologoDTO>(this.apiUrl, { headers });
  }


  uploadImagemBase64(usuarioId: number, imagemBase64: string): Observable<any> {
    const token = this.authService.getToken();
    const headers = new HttpHeaders({
      'Authorization': `Bearer ${token}`
    });

    console.log('Token being used for upload:', token);

    return this.http.post(`http://localhost:8080/psicologo/${usuarioId}/imagem-base64`, {
      imagem: imagemBase64
    }, { headers });
  }

  obterImagem(usuarioId: number): Observable<any> {
    const token = this.authService.getToken();
    const headers = new HttpHeaders({
      'Authorization': `Bearer ${token}`
    });

    console.log('Token being used for image retrieval:', token);

    return this.http.get(`http://localhost:8080/psicologo/${usuarioId}/imagem`, { headers });
  }

}
