import { Component, EventEmitter, OnInit, Output } from '@angular/core';

@Component({
  selector: 'app-custom-calendar',
  templateUrl: './custom-calendar.component.html',
  styleUrls: ['./custom-calendar.component.scss']
})
export class CustomCalendarComponent implements OnInit {
  weekDays = ['Dom', 'Seg', 'Ter', 'Qua', 'Qui', 'Sex', 'Sáb'];
  currentDate = new Date();
  days: number[] = [];
  blanks: any[] = [];
  selectedDay: number | null = null;
  today: number | null = null;

  @Output() dateChange = new EventEmitter<Date>();

  ngOnInit() {
    this.updateToday();
    this.generateCalendar();
  }

  updateToday() {
    const now = new Date();
    if (
      now.getMonth() === this.currentDate.getMonth() &&
      now.getFullYear() === this.currentDate.getFullYear()
    ) {
      this.today = now.getDate();
    } else {
      this.today = null;
    }
  }

  generateCalendar() {
    const year = this.currentDate.getFullYear();
    const month = this.currentDate.getMonth();
    const firstDay = new Date(year, month, 1).getDay();
    const daysInMonth = new Date(year, month + 1, 0).getDate();

    this.blanks = Array(firstDay).fill(0);
    this.days = Array.from({ length: daysInMonth }, (_, i) => i + 1);
  }

  selectDay(day: number) {
    this.selectedDay = day;
    const selectedDate = new Date(this.currentDate.getFullYear(), this.currentDate.getMonth(), day);
    this.dateChange.emit(selectedDate);
  }

  prevMonth() {
    this.currentDate = new Date(this.currentDate.getFullYear(), this.currentDate.getMonth() - 1, 1);
    this.updateToday();
    this.generateCalendar();
    this.selectedDay = null;
  }

  nextMonth() {
    this.currentDate = new Date(this.currentDate.getFullYear(), this.currentDate.getMonth() + 1, 1);
    this.updateToday();
    this.generateCalendar();
    this.selectedDay = null;
  }
}
