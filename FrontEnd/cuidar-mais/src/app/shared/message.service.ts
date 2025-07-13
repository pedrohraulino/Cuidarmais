import { Injectable } from '@angular/core';
import { BehaviorSubject } from 'rxjs';

@Injectable({
  providedIn: 'root'
})
export class MessageService {
  private successMessageSource = new BehaviorSubject<string>('');
  private errorMessageSource = new BehaviorSubject<string>('');

  successMessage$ = this.successMessageSource.asObservable();
  errorMessage$ = this.errorMessageSource.asObservable();

  setSuccessMessage(message: string) {
    this.successMessageSource.next(message);
  }

  setErrorMessage(message: string) {
    this.errorMessageSource.next(message);
  }

  clearMessages() {
    this.successMessageSource.next('');
    this.errorMessageSource.next('');
  }
}
