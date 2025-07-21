import { Injectable } from '@angular/core';
import {
  HttpInterceptor,
  HttpRequest,
  HttpHandler,
  HttpEvent,
  HttpErrorResponse
} from '@angular/common/http';
import { Observable, throwError } from 'rxjs';
import { catchError } from 'rxjs/operators';
import { Router } from '@angular/router';
import { AuthService } from '../auth/auth.service';

@Injectable()
export class TokenInterceptor implements HttpInterceptor {
  constructor(private authService: AuthService, private router: Router) {}

  intercept(
    req: HttpRequest<any>,
    next: HttpHandler
  ): Observable<HttpEvent<any>> {
    const token = this.authService.getToken();

    if (token) {
      const authReq = req.clone({
        setHeaders: {
          Authorization: `Bearer ${token}`
        }
      });
      return next.handle(authReq).pipe(
        catchError((error: HttpErrorResponse) => {
          console.log('Erro HTTP interceptado:', error.status, error.url);
          if (error.status === 401) {
            // Token inválido ou expirado - redireciona para login
            console.log('Token inválido (401), fazendo logout...');
            this.authService.logout();
          } else if (error.status === 403) {
            // Acesso negado - mas não necessariamente token expirado
            console.log('Acesso negado (403), mas mantendo sessão...');
            // Não fazer logout automático para 403, apenas logar o erro
          }
          return throwError(() => error);
        })
      );
    }

    return next.handle(req).pipe(
      catchError((error: HttpErrorResponse) => {
        console.log('Erro HTTP sem token:', error.status, error.url);
        if (error.status === 401) {
          // Não autenticado - redireciona para login
          console.log('Não autenticado (401), redirecionando para login...');
          this.router.navigate(['/login']);
        }
        return throwError(() => error);
      })
    );
  }
}
