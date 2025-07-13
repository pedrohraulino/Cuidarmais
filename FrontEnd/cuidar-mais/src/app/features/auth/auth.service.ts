import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Router } from '@angular/router';
import { Observable, tap, switchMap } from 'rxjs';

@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly API_URL = 'http://localhost:8080/auth/login';
  private readonly USER_API_URL = 'http://localhost:8080/usuarios/me';

  constructor(private http: HttpClient, private router: Router) {}

  login(email: string, senha: string): Observable<any> {
    return this.http.post<any>(this.API_URL, { email, senha }).pipe(
      tap(response => {
        localStorage.setItem('token', response.token);
      }),
      switchMap(() => this.getUserInfo())
    );
  }

  getUserInfo(): Observable<any> {
    return this.http.get<any>(this.USER_API_URL).pipe(
      tap(user => {
        localStorage.setItem('usuario', JSON.stringify(user));
      })
    );
  }

  logout() {
    localStorage.removeItem('token');
    localStorage.removeItem('usuario');
    this.router.navigate(['/login']);
  }

  getUser(): any {
    const user = localStorage.getItem('usuario');
    return user ? JSON.parse(user) : null;
  }

  getToken(): string | null {
    return localStorage.getItem('token');
  }

  isAuthenticated(): boolean {
    return !!this.getToken();
  }
}
