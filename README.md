# ERP Corporativo

Plataforma ERP multi-tenant com foco em governanca de acessos, configuracao de e-mail por empresa (tenant) e fluxos seguros de autenticacao.

## Visao Geral

Este projeto implementa:

- autenticacao com Spring Security
- gestao de usuarios com papeis (`SUPER_ADMIN`, `ADMIN`, `USER`)
- isolamento por tenant (`empresa_id`)
- modulo de empresas para onboarding de novos clientes
- configuracao de SMTP por tenant (Gmail + senha de app)
- envio assíncrono com RabbitMQ (fila + retry + DLQ)
- fluxo "esqueci minha senha" com token de uso unico e expiracao

## Stack Tecnologica

- Java 17
- Spring Boot 4
- Spring MVC + Thymeleaf
- Spring Security
- Spring Data JPA (Hibernate)
- Flyway
- MySQL 8
- RabbitMQ
- Maven

## Arquitetura (resumo)

- **Web layer**: controllers Thymeleaf para dashboard, usuarios, empresas, configuracoes e autenticacao.
- **Service layer**: regras de negocio (tenancy, seguranca, provisionamento, recuperacao de senha).
- **Persistence layer**: entidades JPA + repositórios.
- **Messaging**: eventos de e-mail publicados em RabbitMQ e consumidos por worker interno.

## Requisitos

- JDK 17+
- MySQL 8+ em execucao
- RabbitMQ em execucao
- Maven (ou usar wrapper `mvnw`)

## Configuracao de Ambiente

As principais configuracoes estao em `src/main/resources/application.properties` e podem ser sobrescritas por variaveis de ambiente.

### Banco de dados

- `DB_URL` (ex.: `jdbc:mysql://localhost:3306/erp_platform?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC`)
- `DB_USERNAME`
- `DB_PASSWORD`

### Seed de super admin

- `SEED_SUPER_ADMIN_NAME`
- `SEED_SUPER_ADMIN_EMAIL`
- `SEED_SUPER_ADMIN_PASSWORD`

### Seguranca e links

- `APP_REMEMBER_ME_KEY`
- `APP_SECRETS_CREDENTIAL_ENCRYPTION_KEY`
- `APP_PUBLIC_BASE_URL` (ex.: `http://localhost:8080` ou URL publica de producao)

### E-mail

- `APP_MAIL_SEND_ON_ADMIN_PASSWORD_RESET` (`true|false`)
- `APP_MAIL_INCLUDE_TEMP_PASSWORD_IN_EMAIL` (`true|false`)

### RabbitMQ

- `RABBITMQ_HOST` (padrao: `localhost`)
- `RABBITMQ_PORT` (padrao: `5672`)
- `RABBITMQ_USERNAME` (padrao: `guest`)
- `RABBITMQ_PASSWORD` (padrao: `guest`)

## Subindo dependencias locais com Docker

### MySQL

```bash
docker run -d --name mysql-erp -e MYSQL_ROOT_PASSWORD=12345 -e MYSQL_DATABASE=erp_platform -p 3306:3306 mysql:8
```

### RabbitMQ (com painel)

```bash
docker run -d --name rabbitmq-erp -p 5672:5672 -p 15672:15672 rabbitmq:3-management
```

Painel RabbitMQ: `http://localhost:15672` (`guest` / `guest`).

## Executando a Aplicacao

### Com Maven Wrapper

```bash
./mvnw spring-boot:run
```

No Windows PowerShell:

```powershell
.\mvnw.cmd spring-boot:run
```

Aplicacao: `http://localhost:8080`

## Primeiro Acesso

1. Suba MySQL e RabbitMQ.
2. Rode a aplicacao.
3. Faça login com o super admin seed (`SEED_SUPER_ADMIN_*`).
4. Acesse **Empresas** e cadastre tenants clientes.
5. Configure SMTP de cada tenant em **Configuracoes > E-mail**.

## Multi-tenancy e Provisionamento

- `SUPER_ADMIN` pode criar empresas (tenants) e provisionar admin inicial.
- Cada `ADMIN` fica vinculado ao `empresa_id`.
- Fluxos e dados sensiveis seguem escopo do tenant.

## Recuperacao de Senha (Seguro)

Fluxo implementado:

1. Usuario solicita em `/esqueci-senha`.
2. Sistema responde mensagem neutra (anti-enumeracao).
3. Se usuario existir/ativo, cria token de uso unico (expira em 30 min).
4. Evento vai para fila RabbitMQ.
5. Consumer envia e-mail com link de redefinicao.
6. Usuario redefine em `/redefinir-senha`.

## Mensageria (RabbitMQ)

Fila principal e DLQ configuradas para envio de e-mails transacionais:

- exchange: `erp.email.exchange`
- queue: `erp.email.password-reset.queue`
- dlq: `erp.email.password-reset.dlq`

Comportamento:

- publish apos commit da transacao
- retry com backoff no consumer
- fallback para DLQ em falhas persistentes

## Migracoes e Banco

As migracoes Flyway estao em:

- `src/main/resources/db/migration`

Principais blocos:

- usuarios/auditoria/reset token
- empresas (tenants)
- configuracao de e-mail por tenant

## Build e Testes

### Build rapido

```bash
./mvnw -DskipTests compile
```

### Testes

```bash
./mvnw test
```

## Estrutura de Pastas (resumo)

```text
src/main/java/com/erpcorporativo
  api/
    config/
    controller/
    domain/
    dto/
    repository/
    service/
  web/
    controller/
    viewmodel/
src/main/resources
  db/migration/
  templates/
  static/
```

## Roadmap Sugerido

- observabilidade de fila (metricas/alertas)
- governanca de DLQ (reprocessamento)
- testes automatizados de fluxos criticos
- hardening de seguranca (rate limit em recuperacao de senha)

## Licenca

Defina a licenca comercial/open-source conforme estrategia do produto.
