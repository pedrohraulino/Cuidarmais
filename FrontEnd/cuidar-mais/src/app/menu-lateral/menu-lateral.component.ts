import { Component } from '@angular/core';
import { AuthService } from './../features/auth/auth.service';
import { Router } from '@angular/router';

@Component({
  selector: 'app-menu-lateral',
  templateUrl: './menu-lateral.component.html',
  styleUrls: ['./menu-lateral.component.scss']
})
export class MenuLateralComponent {
  constructor(private authService: AuthService, public router: Router) {}

  logout() {
    this.authService.logout(); // limpa o token e redireciona
  }
}
