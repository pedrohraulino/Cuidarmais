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
  imagemPreview: string | null = null;

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
      next: (res) => {
        console.log('Dados do psicólogo recebidos:', res);
        console.log('CRP recebido:', res.crp);
        console.log('Imagem recebida:', res.imagemDataUrl);

        this.psicologo = res;
        // Usar a imagem que já vem nos dados do psicólogo
        if (res.imagemDataUrl) {
          console.log('Definindo imagemPreview com a imagem recebida');
          this.imagemPreview = res.imagemDataUrl;
        } else {
          console.log('Imagem não recebida ou nula');
        }
      },
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
              console.log('Upload realizado com sucesso:', response);
              // Atualizar o imagemDataUrl no objeto psicologo
              if (this.psicologo) {
                this.psicologo.imagemDataUrl = this.imagemPreview!;
              }
            },
            error => {
              console.error('Erro no upload:', error);
            }
          );
      } else {
        console.error('Erro: ID do psicólogo não disponível ou imagem não carregada');
      }
    };
    reader.readAsDataURL(arquivo);
  }


}
