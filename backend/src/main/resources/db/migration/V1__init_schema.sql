-- Esquema inicial del Gestor de Archivos Web (MySQL 8, UUID como CHAR(36))
CREATE TABLE users (
    id            CHAR(36)     NOT NULL,
    username      VARCHAR(100) NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    full_name     VARCHAR(150) NOT NULL,
    role          VARCHAR(30)  NOT NULL,
    active        BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at    DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    CONSTRAINT uq_users_username UNIQUE (username)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE folders (
    id          CHAR(36)     NOT NULL,
    public_slug VARCHAR(64)  NOT NULL,
    name        VARCHAR(150) NOT NULL,
    disk_path   VARCHAR(500) NOT NULL,
    created_by  CHAR(36)     NOT NULL,
    created_at  DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    CONSTRAINT uq_folders_public_slug UNIQUE (public_slug),
    CONSTRAINT fk_folders_created_by FOREIGN KEY (created_by) REFERENCES users (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE folder_files (
    id            CHAR(36)     NOT NULL,
    folder_id     CHAR(36)     NOT NULL,
    filename      VARCHAR(255) NOT NULL,
    relative_path VARCHAR(300) NOT NULL DEFAULT '',
    mime_type     VARCHAR(150) NOT NULL,
    size_bytes    BIGINT       NOT NULL,
    uploaded_at   DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    -- Evita duplicados por carpeta + ruta relativa + nombre; además indexa por folder_id
    CONSTRAINT uq_folder_files_path UNIQUE (folder_id, relative_path, filename),
    CONSTRAINT fk_folder_files_folder FOREIGN KEY (folder_id) REFERENCES folders (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
