# Arquitetura: permissões granulares, multi-tenant e configuração de e-mail

Este documento descreve o **modelo alvo** (evolução do RBAC atual) e o **fluxo de configuração de e-mail por tenant** (Gmail com OAuth2 / token de refresh).

## 1. Visão geral: camadas

```mermaid
flowchart TB
  subgraph plataforma["Plataforma (SUPER_ADMIN)"]
    SA[SUPER_ADMIN]
  end
  subgraph tenants["Tenants (empresas)"]
    E1[Empresa A]
    E2[Empresa B]
  end
  SA -->|provisiona| E1
  SA -->|provisiona| E2
  E1 --> U1[Usuários, dados, configurações]
  E2 --> U2[Usuários, dados, configurações]
```

- **Super Admin**: dono da plataforma, escopo global (e gestão de tenants).
- **Tenant (empresa)**: unidade de isolamento; cada um tem `empresa_id` em dados e **configurações próprias** (ex.: e-mail).

## 2. Hoje (fase atual) vs. fase alvo (permissões)

```mermaid
flowchart LR
  subgraph hoje["Hoje: RBAC por papel"]
    R1[ROLE_SUPER_ADMIN]
    R2[ROLE_ADMIN]
    R3[ROLE_USER]
  end
  subgraph alvo["Alvo: RBAC + permissões (módulo/ação)"]
    P[Recurso: ex. USUARIOS:READ]
    A[Ação: ex. USUARIOS:DELETE]
    M[Mapa: usuário/role → permissões]
  end
  R2 -->|evolui| M
  P --> M
  A --> M
```

| Conceito | Hoje | Alvo (profissional) |
|----------|------|----------------------|
| Quem acessa o quê | Apenas 3 **papéis** | Papéis **e/ou** catálogo de **permissões** ( strings estáveis, ex. `FATURAMENTO:EMITIR_NFE`) |
| Módulo novo | Ajuste manual em `@PreAuthorize` e menus | Registro de permissão + atribuição a perfis ou usuário |
| Auditoria | Já existe em usuários | Estender padrão para quem ativou qual permissão |

**Implementação típica (próxima fase de produto):**

- Tabelas: `permissao`, `papel_permissao`, opcional `usuario_permissao` (exceção).
- `ErpUserDetailsService` carrega `GrantedAuthority` a partir de `ROLE_*` **e** de `permissao`.
- Controllers: `@PreAuthorize("hasAuthority('USUARIOS:WRITE')")` além de roles quando necessário.

O **módulo de Configurações** descrito abaixo é independente e já nasce **por `empresa_id`**.

## 3. Configuração de e-mail por tenant (Gmail / OAuth2)

Não se armazena "senha do Gmail" em texto plano. O fluxo profissional é **OAuth2**:

```mermaid
sequenceDiagram
  participant Admin
  participant ERP as ERP (Configurações)
  participant G as Google OAuth
  participant DB as Banco (cifrado)
  participant Mail as Envio (SMTP / Gmail API)
  Admin->>ERP: Informa Client ID, Client secret, cola token de refresh
  ERP->>DB: Grava cifrado (AES + chave em env)
  Note over ERP,Mail: Em runtime: refresh token gera access token de curta duração
  ERP->>Mail: Envia e-mail em nome do tenant
```

- **Client ID / Secret**: vêm do [Google Cloud Console](https://console.cloud.google.com/) (projeto, OAuth client).
- **Refresh token**: obtido após o fluxo de consentimento (uma vez); é o que **persistimos cifrado** para renovar o access token sem interação.
- A **tela atual** de configuração aceita esses três (e remetente); o refinamento "botão conectar com Google" pode ser a próxima iteração (redirect OAuth no ERP).

## 4. Modelo de dados (configuração de e-mail)

```mermaid
erDiagram
  empresas ||--o| configuracao_email_empresa : possui
  empresas {
    bigint id PK
    string nome_fantasia
    boolean ativo
  }
  configuracao_email_empresa {
    bigint id PK
    bigint empresa_id FK
    boolean ativo
    string provedor
    string endereco_remetente
    text oauth_client_id
    text oauth_client_secret_ciphertext
    text oauth_refresh_token_ciphertext
  }
```

## 5. Segurança

- Segredos **nunca** retornam inteiros para o front: apenas máscara (`****`) ou vazio = "não alterar".
- Chave de cifra: `CREDENTIAL_ENCRYPTION_KEY` / `app.secrets.credential-encryption-key` (definir em produção, nunca no repositório).
- **SUPER_ADMIN** escolhe o tenant (`empresaId`); **ADMIN** só vê o próprio `empresa_id`.

---

*Documento alinhado à implementação inicial em `com.erpcorporativo` (módulo Configurações + entidades `Empresa` e `ConfiguracaoEmailEmpresa`).*
