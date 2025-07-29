import { Component, Output, EventEmitter } from '@angular/core';

@Component({
  selector: 'app-custom-calendar',
  templateUrl: './custom-calendar.component.html',
  styleUrls: ['./custom-calendar.component.scss']
})
export class CustomCalendarComponent {
  @Output() dateChange = new EventEmitter<Date>();
  currentDate: Date = new Date();

  weekDays: string[] = ['Dom', 'Seg', 'Ter', 'Qua', 'Qui', 'Sex', 'Sáb'];

  today: number = new Date().getDate();
  selectedDay: number | null = null;

  mesesPt: string[] = [
    'Janeiro', 'Fevereiro', 'Março', 'Abril', 'Maio', 'Junho',
    'Julho', 'Agosto', 'Setembro', 'Outubro', 'Novembro', 'Dezembro'
  ];

  get month(): number { return this.currentDate.getMonth(); }
  get year(): number { return this.currentDate.getFullYear(); }

  get nomeMes(): string {
    return this.mesesPt[this.month];
  }

  get days(): number[] {
    const daysInMonth = new Date(this.year, this.month + 1, 0).getDate();
    return Array.from({ length: daysInMonth }, (_, i) => i + 1);
  }

  get blanks(): any[] {
    const firstDay = new Date(this.year, this.month, 1).getDay();
    return Array(firstDay).fill(0);
  }

  selectDay(day: number) {
    this.selectedDay = day;
    const selected = new Date(this.year, this.month, day);
    this.dateChange.emit(selected);
  }

  prevMonth() {
    this.currentDate = new Date(this.year, this.month - 1, 1);
  }

  nextMonth() {
    this.currentDate = new Date(this.year, this.month + 1, 1);
  }

  isPastDay(day: number): boolean {
    const today = new Date();
    const date = new Date(this.year, this.month, day);
    // Ignora hora/minuto/segundo
    today.setHours(0,0,0,0);
    date.setHours(0,0,0,0);
    return date < today;
  }

  isToday(day: number): boolean {
    const now = new Date();
    return (
      day === now.getDate() &&
      this.month === now.getMonth() &&
      this.year === now.getFullYear()
    );
  }

  ngOnInit() {
    // Seleciona o dia atual automaticamente ao carregar o componente
    const now = new Date();
    if (this.month === now.getMonth() && this.year === now.getFullYear()) {
      this.selectedDay = now.getDate();
      this.dateChange.emit(new Date(this.year, this.month, this.selectedDay));
    } else {
      this.selectedDay = null;
    }
  }
}
