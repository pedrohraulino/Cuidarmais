import {
  PsicologoDTO,
  PsicologoService,
} from './../../services/psicologo.service';
import { Router, NavigationEnd } from '@angular/router';
import { Component, OnInit } from '@angular/core';
import { filter, map } from 'rxjs';
import { MessageService } from '../../shared/message.service';

@Component({
  selector: 'app-cabecalho',
  templateUrl: './cabecalho.component.html',
  styleUrl: './cabecalho.component.scss',
})
export class CabecalhoComponent implements OnInit {
  pageTitle: string = '';
  psicologo: PsicologoDTO | null = null;
  imagemPreview: string | null = null;

  constructor(
    private router: Router,
    private psicologoService: PsicologoService,
    private messageService: MessageService
  ) {}

  ngOnInit() {
    this.router.events
      .pipe(
        filter((event) => event instanceof NavigationEnd),
        map(() => this.extractTitleFromRoute())
      )
      .subscribe((title) => {
        this.pageTitle = title;
        // Clear messages when navigating between pages
        this.messageService.clearMessages();
      });
    this.psicologoService.getDadosPsicologo().subscribe({
      next: (res) => {
        this.psicologo = res;
        // Usar a imagem que já vem nos dados do psicólogo
        if (res.imagemDataUrl) {
          this.imagemPreview = res.imagemDataUrl;
        }
      },
      error: (err) => {
        // Error handled silently
      },
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

   onFileSelected(event: any) {
    const arquivo = event.target.files[0];
    if (arquivo) {
      this.converterParaBase64(arquivo);
    }
  }

  converterParaBase64(arquivo: File) {
    const reader = new FileReader();
    reader.onload = (e: any) => {
      this.imagemPreview = e.target.result;

      if (this.psicologo && this.psicologo.id && this.imagemPreview) {
        this.psicologoService.uploadImagemBase64(this.psicologo.id, this.imagemPreview)
          .subscribe(
            response => {
              // Atualizar o imagemDataUrl no objeto psicologo
              if (this.psicologo) {
                this.psicologo.imagemDataUrl = this.imagemPreview!;
              }
            },
            error => {
              // Error handled silently
            }
          );
      }
    };
    reader.readAsDataURL(arquivo);
  }


}
