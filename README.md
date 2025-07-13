# Database Schema Update

## Issue Description

The application is encountering a foreign key constraint violation when trying to create a patient:

```
Erro ao cadastrar paciente: could not execute statement [ERROR: insert or update on table "agendamento" violates foreign key constraint "fk81wc065s6yhapkxh4lwbfsr0m" Detalhe: Key (paciente_id)=(23) is not present in table "usuario".]
```

This error occurs because the database schema expects the `paciente_id` field in the `agendamento` table to reference the `usuario` table, but the entity model has been updated to reference the `paciente` table instead.

## Solution

To fix this issue, we need to update the database schema to match the entity model. The included SQL script (`update_schema.sql`) will:

1. Drop the existing foreign key constraint `fk81wc065s6yhapkxh4lwbfsr0m` on the `agendamento` table
2. Add a new foreign key constraint `fk_agendamento_paciente` that references the `paciente` table

## Instructions

1. Stop the application if it's running
2. Execute the SQL script against your PostgreSQL database:

   ```bash
   psql -U postgres -d postgres -f update_schema.sql
   ```

   Or use your preferred database management tool to execute the script.

3. Restart the application

## Verification

After applying the fix, try creating a patient again. The foreign key constraint violation should no longer occur.