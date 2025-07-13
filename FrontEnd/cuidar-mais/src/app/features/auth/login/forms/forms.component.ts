import { Component } from '@angular/core';
import { AuthService } from '../../auth.service';
import { Router } from '@angular/router';
@Component({
  selector: 'app-forms',
  templateUrl: './forms.component.html',
  styleUrl: './forms.component.scss',
})
export class FormsComponent {
  email = '';
  senha = '';
  erro = '';

  constructor(private authService: AuthService, private router: Router) {}

  login() {
    this.authService.login(this.email, this.senha).subscribe({
      next: () => {
        this.router.navigate(['/pacientes']);
      },
      error: (err) => {
        if (err.status === 400 || err.status === 401 || err.status === 403) {
          this.erro = err.error?.erro || 'Credenciais inválidas';
        } else {
          this.erro = 'Erro inesperado. Tente novamente.';
        }
      }
    });
  }
}
