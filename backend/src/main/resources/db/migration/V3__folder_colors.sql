-- Color opcional (hex #RRGGBB) para identificar visualmente carpetas y subcarpetas.
ALTER TABLE folders ADD COLUMN color VARCHAR(7) NULL;
ALTER TABLE folder_directories ADD COLUMN color VARCHAR(7) NULL;
