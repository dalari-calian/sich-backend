# Resumo do Projeto SICH Backend

**Responsável:** Calian Dalari
**Período:** 03/09/2026 a 01/10/2026
**Total:** 21 commits (incluindo 1 merge de Pull Request)

---

## Visão geral das entregas

### 1. Estrutura inicial do projeto (03/09 e 04/09)

- Criação do projeto backend (Java/Spring) com configurações básicas e banner personalizado.
- Banco de dados PostgreSQL rodando em container Docker.
- Configuração para rodar e depurar o projeto no VS Code.
- Documentação de como configurar e rodar o projeto (SETUP/README).

### 2. Autenticação e cadastro de usuários (04/09 e 05/09)

- Cadastro e login de usuários com token de acesso seguro (JWT).
- Dois tipos de usuário: **Cliente** e **Prestador**, cada um com seu perfil.
- O tipo do usuário vai no login e na resposta de autenticação.
- Tratamento padronizado de erros (ex.: e-mail já cadastrado, recurso não encontrado, acesso não autorizado).

### 3. Documentação da API e qualidade (05/09)

- Interface visual para consultar e testar a API (Scalar/OpenAPI).
- Pipeline de CI/CD no GitHub, que valida o projeto automaticamente a cada alteração (PR #1).
- Definição de padrões do projeto: nomes de colunas no banco e obrigatoriedade de testes automatizados nas regras de negócio.

### 4. Perfil do usuário e endereço do cliente (11/09 e 12/09)

- Nova consulta **"Meu perfil"** (`auth/me`), que retorna os dados do usuário logado.
- Endereço no cadastro do cliente (cidade, estado, rua e complemento).
- Primeira bateria de testes automatizados dos serviços de usuário, cliente, prestador e autenticação.

### 5. Dados completos do prestador e melhorias no cadastro (25/09)

- Endereço completo com **bairro, CEP e número**, para clientes e prestadores.
- O prestador passou a ter CPF/CNPJ e endereço, igual ao cliente.
- CEP salvo sempre só com números, mesmo que venha com ponto ou traço.
- "Meu perfil" passou a mostrar todos os dados, também para prestadores.
- Cadastro reorganizado para facilitar a inclusão de novos campos no futuro.
- Testes ampliados para cobrir as novidades.

---

## Lista de commits

| Data  | Commit    | Descrição                                                                   |
|-------|-----------|-----------------------------------------------------------------------------|
| 03/09 | `93527a8` | Criação da base do projeto                                                  |
| 03/09 | `91b543c` | Banner e configurações da aplicação                                         |
| 03/09 | `992b0bf` | Banco PostgreSQL no Docker e configuração do VS Code                        |
| 04/09 | `81de8c4` | Documentação de setup e ajuste da porta do banco                            |
| 04/09 | `3f9497b` | Ajuste na documentação                                                      |
| 04/09 | `340727a` | Entidades de usuário, cliente e prestador, autenticação e JWT               |
| 05/09 | `362db23` | Correção no retorno de acesso não autorizado                                |
| 05/09 | `f364b4a` | Interface de documentação da API (Scalar)                                   |
| 05/09 | `ee243eb` | Dependência da documentação da API                                          |
| 05/09 | `46e64d0` | Tipo de usuário no login e padrão de nomes no banco                         |
| 05/09 | `067381d` | Pipeline de CI/CD                                                           |
| 05/09 | `54e0109` | Regra de 100% de cobertura de testes                                        |
| 05/09 | `f95f944` | Merge do PR #1 (CI/CD)                                                      |
| 05/09 | `5bdd146` | Tipo de usuário na resposta de autenticação                                 |
| 11/09 | `6549a8f` | Campos de endereço do cliente                                               |
| 11/09 | `2375a77` | Consulta "Meu perfil" e testes automatizados                                |
| 11/09 | `ca95f72` | Remoção de dados desnecessários do "Meu perfil"                             |
| 11/09 | `cb6366c` | Endereço do cliente no cadastro                                             |
| 11/09 | `6e58582` | Correção de teste                                                           |
| 12/09 | `f79b2b7` | Gravação do endereço do cliente                                             |
| 25/09 | `1454a18` | Dados completos do prestador, CEP/bairro/número e reorganização do cadastro |
