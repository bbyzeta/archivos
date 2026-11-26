-- Subcarpetas registradas explícitamente (para conservar carpetas vacías).
-- Las subcarpetas con archivos se siguen derivando de folder_files.relative_path.
CREATE TABLE folder_directories (
    id         CHAR(36)     NOT NULL,
    folder_id  CHAR(36)     NOT NULL,
    path       VARCHAR(300) NOT NULL,
    created_at DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    CONSTRAINT uq_folder_directories_path UNIQUE (folder_id, path),
    CONSTRAINT fk_folder_directories_folder FOREIGN KEY (folder_id) REFERENCES folders (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
