-- Drop the existing foreign key constraint
ALTER TABLE agendamento DROP CONSTRAINT fk81wc065s6yhapkxh4lwbfsr0m;

-- Add a new foreign key constraint that references the paciente table
ALTER TABLE agendamento ADD CONSTRAINT fk_agendamento_paciente FOREIGN KEY (paciente_id) REFERENCES paciente(id);