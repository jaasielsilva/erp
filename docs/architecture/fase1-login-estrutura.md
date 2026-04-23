# Fase 1 - Estrutura e responsabilidades

## Objetivo
Criar a base do ERP com autenticacao por sessao, tela de login em Thymeleaf e organizacao de pacotes pronta para evoluir para multi-tenant e Angular.

## Pastas principais
- `src/main/java/com/erpcorporativo/api`
  - Responsavel por regras de backend e endpoints REST.
- `src/main/java/com/erpcorporativo/web`
  - Responsavel por controllers MVC e telas Thymeleaf.
- `src/main/resources/templates`
  - HTML renderizado no servidor.
- `src/main/resources/db/migration`
  - Scripts de migracao do banco (Flyway).
- `src/main/resources/static`
  - CSS, JS e imagens.

## Responsabilidade das classes da Fase 1
- `api/domain/usuario/Usuario`
  - Entidade persistida no MySQL.
- `api/domain/usuario/UserRole`
  - Define papeis de acesso (`SUPER_ADMIN`, `ADMIN`, `USER`).
- `api/repository/usuario/UsuarioRepository`
  - Camada de acesso a dados dos usuarios.
- `api/service/auth/ErpUserDetailsService`
  - Integra usuarios do banco com Spring Security.
- `api/service/auth/SeedSuperAdminService`
  - Cria SUPER_ADMIN inicial se ainda nao existir.
- `api/config/security/SecurityConfig`
  - Configura login, logout e rotas protegidas.
- `api/controller/auth/AuthController`
  - Endpoint REST inicial (`/api/v1/auth/me`) para validar sessao.
- `web/controller/auth/LoginController`
  - Exibe a tela de login.
- `web/controller/DashboardController`
  - Exibe a pagina interna apos autenticacao.

## Como isso prepara o futuro
- Login e seguranca ficam centralizados e reutilizaveis.
- O modulo `web` ja esta separado por responsabilidade de UI.
- A API ja possui padrao versionado (`/api/v1`), facilitando Angular depois.
