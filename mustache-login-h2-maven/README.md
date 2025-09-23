# Mustache + Spring Boot + H2 + Login (con Maven)

Este proyecto incluye:
- Spring Boot 3.3.3
- Mustache para vistas
- H2 (en memoria)
- Spring Security con BCrypt

## Requisitos
- Java 17+
- Maven (`mvn -v` para verificar)

## Ejecutar
```bash
mvn spring-boot:run
```

Abrir: http://localhost:8080/login  
Consola H2: http://localhost:8080/h2-console (JDBC URL: `jdbc:h2:mem:demo`)

## Notas
- Usa BCrypt para las contraseñas (no texto plano).
- Regístrate en `/register` y luego inicia sesión en `/login`.
