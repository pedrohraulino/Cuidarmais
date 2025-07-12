import { TestBed } from '@angular/core/testing';

import { PsicologoService } from './psicologo.service';

describe('PsicologoService', () => {
  let service: PsicologoService;

  beforeEach(() => {
    TestBed.configureTestingModule({});
    service = TestBed.inject(PsicologoService);
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });
});
