# jerokysys-auth

Microservicio de autenticación y gestión de usuarios de **JerokySys**. Valida credenciales con BCrypt, emite tokens JWT (HS256) y expone el CRUD de usuarios. Se registra en Eureka.

- Puerto por defecto: `9000`
- Swagger UI: `http://localhost:9000/swagger-ui.html`
- OpenAPI JSON: `http://localhost:9000/v3/api-docs`

## Stack

| Componente | Versión |
|---|---|
| Java | 17 |
| Spring Boot | 4.1.2 |
| Spring Security + BCrypt | starter |
| Spring Data JPA | starter |
| PostgreSQL (Neon) | driver runtime |
| jjwt | 0.13.0 |
| springdoc-openapi | 3.1.1 |
| commons JerokySys | 0.0.3-SNAPSHOT |

## Requisitos previos

1. **JDK 17**.
2. **commons instalado** en el repositorio Maven local (auth depende de `com.jerokysys:commons:0.0.3-SNAPSHOT`):

   ```bash
   cd ../jerokysys-commons
   ./mvnw install -DskipTests
   ```

3. **Base de datos** accesible con el schema `auth`. El DDL lo aplica el changelog de Liquibase de `commons`; auth arranca con `ddl-auto=validate`, así que la tabla `auth.users` debe existir.
4. **Eureka** en `http://localhost:8761` es opcional en local: sin él la app arranca igual y solo registra warnings de descubrimiento.

## Configuración

El servicio importa `classpath:config/private.properties`, que está **ignorado por git**. Para empezar:

```bash
cp src/main/resources/config/private.example.properties src/main/resources/config/private.properties
```

| Propiedad | Obligatoria | Descripción |
|---|---|---|
| `JWT_SECRET` | sí | Clave de firma, 32 bytes en Base64. Debe ser la misma en todos los servicios que validen el token. Si falta, la app no arranca. |
| `AUTH_PORT` | no (`9000`) | Puerto HTTP. |
| `db.url` | sí | JDBC URL de PostgreSQL. |
| `db.username` | sí | Usuario de la base. |
| `db.password` | sí | Password de la base. |

Configuración funcional en `application.properties`:

| Propiedad | Valor | Descripción |
|---|---|---|
| `jwt.algorithm` | `HS256` | Algoritmo de firma (debe ser HMAC). |
| `jwt.expiration-minutes` | `10` | Vigencia del token en minutos. |
| `jwt.subject` | texto fijo | Valor del claim `sub`. |
| `jwt.issuer` | vacío | Claim `iss`; si está vacío no se emite. |
| `jwt.claim.user` | `username` | Nombre del claim que lleva el usuario. |
| `jwt.claim.role` | `role` | Nombre del claim que lleva el rol. |
| `login.max-attempts` | `5` | Intentos fallidos antes de bloquear. |
| `login.lock-minutes` | `15` | Duración del bloqueo. |

## Ejecución

```bash
# Linux / macOS
./mvnw spring-boot:run

# Windows
mvnw.cmd spring-boot:run
```

Build del artefacto:

```bash
./mvnw clean package
java -jar target/auth-0.0.1-SNAPSHOT.jar
```

## API

Todas las respuestas usan el envelope `Response<T>` de `commons` (`data`, `message`, `code`, `status`, `success`), **excepto** `POST /login`, que devuelve el token como texto plano.

| Método | Ruta | Acceso | Descripción |
|---|---|---|---|
| `POST` | `/login` | Público | Autentica y devuelve el JWT. |
| `POST` | `/users` | Público | Auto-registro con rol `PROFESSOR` o `SECRETARY`. |
| `GET` | `/users` | `ADMIN` | Listado paginado. |
| `GET` | `/users/{id}` | `ADMIN` | Detalle de un usuario. |
| `PUT` | `/users/{id}` | `ADMIN` | Actualización parcial. |
| `DELETE` | `/users/{id}` | `ADMIN` | Baja lógica (`200`). |

### Login

```bash
curl.exe -X POST http://localhost:9000/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"admin123"}'
```

| Código | Significado |
|---|---|
| `200` | Token JWT en el body (`text/plain`). |
| `401` | Credenciales inválidas o usuario inactivo. |
| `429` | Cuenta bloqueada; incluye header `Retry-After` en segundos. |
| `400` | Body inválido o campos vacíos. |

### Registro

```bash
curl.exe -X POST http://localhost:9000/users \
  -H "Content-Type: application/json" \
  -d '{"username":"profesor1","password":"secreto123","role":"PROFESSOR"}'
```

- `username`: 3–100 caracteres. Se normaliza a minúsculas.
- `password`: 8–72 caracteres.
- `role`: `PROFESSOR` o `SECRETARY`. `ADMIN` se rechaza con `INVALID_ROLE`.
- Usuario duplicado: `409 CONFLICT`.

### Listado paginado

```bash
curl.exe -H "Authorization: Bearer $TOKEN" \
  "http://localhost:9000/users?page=0&size=10&sort=username&direction=ASC"
```

| Parámetro | Default | Límites |
|---|---|---|
| `page` | `0` | ≥ 0 |
| `size` | `10` | 1–100 |
| `sort` | `id` | `id`, `username`, `role`, `active`, `createdAt` |
| `direction` | `ASC` | `ASC` o `DESC` |

Respuesta: `PageResult<UserResponse>` con `content`, `totalElements`, `totalPages`, `page`, `size`.

### Actualización y baja

```bash
curl.exe -X PUT http://localhost:9000/users/2 \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"password":"nuevaClave123","active":false}'

curl.exe -X DELETE http://localhost:9000/users/2 \
  -H "Authorization: Bearer $TOKEN"
```

`PUT` es parcial: solo actualiza los campos no nulos (`password`, `role`, `active`). El borrado es lógico: marca `deleted = true` y `active = false`, y el usuario deja de aparecer en todas las consultas.

## Autenticación

Enviar el token en cada request protegido:

```
Authorization: Bearer <token>
```

Claims del token:

| Claim | Contenido |
|---|---|
| `username` | Nombre de usuario (configurable con `jwt.claim.user`). |
| `role` | `ADMIN`, `PROFESSOR` o `SECRETARY`. |
| `sub` | Texto fijo definido en `jwt.subject`. |
| `iat` / `exp` | Emisión y vencimiento (10 minutos). |

Reglas de acceso: `/login`, `POST /users` y la documentación son públicos; **cualquier otra ruta requiere `ROLE_ADMIN`**. Los errores de seguridad (`401`/`403`) se devuelven sin body.

### Bloqueo por intentos fallidos

Tras 5 fallos consecutivos el usuario queda bloqueado 15 minutos y el login responde `429` con `Retry-After`. El contador es por username y vive en memoria del proceso: no se comparte entre instancias.

## Roles

| Rol | Auto-registro | Descripción |
|---|---|---|
| `ADMIN` | no | Acceso total al CRUD de usuarios. |
| `PROFESSOR` | sí | Rol de profesor. |
| `SECRETARY` | sí | Rol de secretaría. |

## Errores

Formato de error (salvo `401`/`403` de seguridad):

```json
{
  "data": null,
  "message": "Username already exists: profesor1",
  "code": "CONFLICT",
  "status": 409,
  "success": false
}
```

| Código | HTTP |
|---|---|
| `VALIDATION_ERROR`, `MALFORMED_REQUEST`, `INVALID_ROLE`, `TYPE_MISMATCH` | `400` |
| `ENTITY_NOT_FOUND`, `NOT_FOUND` | `404` |
| `CONFLICT` | `409` |
| `INTERNAL_ERROR` | `500` |

## Estructura

```
src/main/java/com/jerokysys/auth/
├── config/          SecurityConfig, OpenApiConfig
├── controller/      AuthController, UserController
├── domain/          User, Role
├── dto/             Credential, CreateUserRequest, UpdateUserRequest, UserResponse
├── repo/            UserRepository
├── security/        JwtAuthenticationFilter
├── service/         AuthService, JWTService, LoginAttempts, UserService
└── JerokysysAuthApplication.java
```

Usa H2 en memoria con `ddl-auto=none` y Eureka deshabilitado. Por ahora solo se ejecuta `contextLoads` (verificación de arranque del contexto).

## Notas y limitaciones

- **Cambiar el usuario `admin` sembrado por el changelog de `commons`** (`admin` / `admin123`) antes de cualquier despliegue.
- Rotar `JWT_SECRET` y la password de la base si se compartió el repositorio.
- No hay refresh token, logout ni revocación: al vencer (10 min) hay que volver a loguearse.
- No hay CORS configurado y el gateway todavía no tiene ruta hacia `jerokysys-auth`; hoy se accede directo al puerto `9000`.
- El bloqueo de intentos es local al proceso (no distribuido).
