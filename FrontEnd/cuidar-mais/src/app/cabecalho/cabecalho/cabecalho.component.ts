import {
  PsicologoDTO,
  PsicologoService,
} from './../../services/psicologo.service';
import { Router, NavigationEnd } from '@angular/router';
import { Component, OnInit } from '@angular/core';
import { filter, map } from 'rxjs';

@Component({
  selector: 'app-cabecalho',
  templateUrl: './cabecalho.component.html',
  styleUrl: './cabecalho.component.scss',
})
export class CabecalhoComponent implements OnInit {
  pageTitle: string = '';
  psicologo: PsicologoDTO | null = null;

  constructor(
    private router: Router,
    private psicologoService: PsicologoService
  ) {}

  ngOnInit() {
    this.router.events
      .pipe(
        filter((event) => event instanceof NavigationEnd),
        map(() => this.extractTitleFromRoute())
      )
      .subscribe((title) => {
        this.pageTitle = title;
      });
    this.psicologoService.getDadosPsicologo().subscribe({
      next: (res) => (this.psicologo = res),
      error: (err) => console.error('Erro ao buscar psicólogo:', err),
    });
    this.pageTitle = this.extractTitleFromRoute();
  }

  private extractTitleFromRoute(): string {
    const route = this.router.routerState.root;
    let child = route.firstChild;

    while (child) {
      if (child.snapshot.data && child.snapshot.data['title']) {
        return child.snapshot.data['title'];
      }
      child = child.firstChild;
    }

    return 'Cuidar+';
  }
}
