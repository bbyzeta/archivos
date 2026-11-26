-- Instante del último cambio o restablecimiento de contraseña: los tokens emitidos antes dejan de ser válidos.
ALTER TABLE users ADD COLUMN password_changed_at DATETIME(6) NULL;
