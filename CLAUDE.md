# Gestor de Archivos Web — Arquitectura

## Stack
- **Backend:** Java 21, Spring Boot 3.x. Capas: Controller, Service, Repository, Entity, DTOs como Java Records. Spring Security + JWT (jjwt).
- **Base de datos:** MySQL con Flyway (`src/main/resources/db/migration`). `spring.jpa.hibernate.ddl-auto=validate`; el esquema solo cambia vía migraciones.
- **Frontend:** Angular 21. SIEMPRE Standalone Components, Signals (`signal`, `computed`, `input`, `output`), control flow moderno (`@if`, `@for`, `@switch`) e inyección con `inject()`. Prohibido NgModules y suscripciones manuales de RxJS innecesarias.
- **Idioma:** clases, métodos, variables, tablas y endpoints en inglés; textos visibles al usuario y comentarios explicativos en español.
- **Contratos:** mantener sincronizados los DTOs Java con las interfaces TypeScript de Angular.

## Configuración (variables de entorno del backend)
| Variable | Por defecto (dev) | Uso |
|---|---|---|
| `DB_URL` | `jdbc:mysql://localhost:3306/archivos_db?createDatabaseIfNotExist=true` | Base de datos `archivos_db` |
| `DB_USERNAME` / `DB_PASSWORD` | `root` / vacío | Credenciales MySQL |
| `JWT_SECRET` | valor de desarrollo | Base64 de ≥32 bytes; obligatorio en producción |
| `JWT_EXPIRATION_MS` | `86400000` | Vigencia del token |
| `ADMIN_USERNAME` / `ADMIN_PASSWORD` | `admin` / vacío | Admin inicial (solo si `users` está vacía; sin password no se crea) |
| `APP_BASE_URL` | `http://localhost:8081` | Base de las URLs públicas |
| `STORAGE_BASE_PATH` | `./uploads` | Ruta base en disco |
| `CORS_ALLOWED_ORIGINS` | `http://localhost:4200` | Orígenes permitidos (separados por coma) |
| `SERVER_PORT` | `8081` | Puerto |

## Modelo de datos
1. `users`: `id` (UUID PK), `username` (UNIQUE), `password_hash`, `full_name`, `role`, `active`, `created_at`.
2. `folders`: `id` (UUID PK), `public_slug` (UNIQUE), `name`, `disk_path` (bajo la ruta base configurable `app.storage.base-path`, p. ej. `/var/data/uploads`, o ruta local en dev), `created_by` (FK a `users`), `created_at`.
3. `folder_files`: `id` (UUID PK), `folder_id` (FK a `folders`, ON DELETE CASCADE), `filename`, `relative_path`, `mime_type`, `size_bytes`, `uploaded_at`. Índice por `folder_id` y restricción UNIQUE (`folder_id`, `relative_path`, `filename`).

## Contrato de endpoints (backend)
**Públicos (`permitAll`)**
- `POST /api/auth/login`
- `GET /api/public/{slug}` — listado público de la carpeta.
- `GET /api/public/{slug}/file/**` — descarga/streaming vía `FileSystemResource` con el `Content-Type` real.

**Privados (JWT Bearer, `/api/admin/**`)**
- `GET /api/admin/folders`
- `POST /api/admin/folders`
- `GET /api/admin/folders/{id}`
- `POST /api/admin/folders/{id}/files` — `MultipartFile` + `relativePath`, sanitizado contra Path Traversal con `Path.normalize()` (el resultado debe seguir dentro del `disk_path` de la carpeta).

Las URLs públicas se arman dinámicamente con la variable de entorno `APP_BASE_URL` (IP o dominio). Secretos (BD, JWT) también por variables de entorno.

## Rutas frontend (Angular 21)
- `/login` — pública. Tras iniciar sesión redirige a `/home`.
- `/home` — `authGuard`. Cuadrícula/listado de carpetas + modal "Nueva Carpeta".
- `/folders/:id` — `authGuard`. Cola de subida Drag & Drop con 3 archivos concurrentes en paralelo + tabla de archivos con link público y botón "Copiar link".
- Navbar con botón "Cerrar sesión". Interceptor HTTP que añade el Bearer y hace auto-logout ante respuestas `401`.

## Reglas de consistencia
- **Seguir la estructura existente:** el código nuevo (Spring Boot y Angular) respeta la organización de carpetas, la arquitectura en capas, las convenciones de nombres y el estilo ya establecidos.
- **No romper lo funcional:** no modificar migraciones Flyway ya existentes (los cambios de esquema van en migraciones nuevas `V2__...`), la configuración de BD (`archivos_db`), la autenticación JWT ni los flujos probados (`/login` y `/home`).
- **Alcance controlado:** limitar los cambios al módulo o tarea solicitada y reutilizar servicios, modelos, estilos y endpoints existentes antes de crear nuevas abstracciones.
