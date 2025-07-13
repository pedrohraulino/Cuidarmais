import { Injectable } from '@angular/core';
import {
  HttpInterceptor,
  HttpRequest,
  HttpHandler,
  HttpEvent
} from '@angular/common/http';
import { Observable } from 'rxjs';
import { AuthService } from '../auth/auth.service';

@Injectable()
export class TokenInterceptor implements HttpInterceptor {
  constructor(private authService: AuthService) {}

  intercept(
    req: HttpRequest<any>,
    next: HttpHandler
  ): Observable<HttpEvent<any>> {
    const token = this.authService.getToken();

    // Log the request URL and whether a token is present
    console.log(`[TokenInterceptor] Request to ${req.url}, Token present: ${!!token}`);

    if (token) {
      const authReq = req.clone({
        setHeaders: {
          Authorization: `Bearer ${token}`
        }
      });
      console.log(`[TokenInterceptor] Added token to request: ${req.url}`);
      return next.handle(authReq);
    }

    console.log(`[TokenInterceptor] No token available for request: ${req.url}`);
    return next.handle(req);
  }
}
