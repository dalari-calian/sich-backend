# Instruções para o Claude neste projeto

## Convenção de nomes de colunas no banco

Todas as colunas de tabelas devem ter nome **sem underscore** — palavras juntas em minúsculo (lowercase concatenado).

- ✅ Correto: `usertype`, `cnpjcpf`, `createdat`, `updatedat`, `userid`
- ❌ Errado: `user_type`, `cnpj_cpf`, `created_at`, `updated_at`, `user_id`

Aplica-se a:
- `@Column(name = "...")` em qualquer campo de `@Entity`
- `@JoinColumn(name = "...")` em relacionamentos
- Nomes de tabela em `@Table(name = "...")` — também sem underscore
- Colunas herdadas de `@MappedSuperclass` (ex.: `AbstractEntity` → `createdat`, `updatedat`)

### Configuração global

Para garantir que o Hibernate **não converta camelCase para snake_case automaticamente**, o `application.properties` usa a estratégia de nomes física padrão (sem transformação):

```properties
spring.jpa.hibernate.naming.physical-strategy=org.hibernate.boot.model.naming.PhysicalNamingStrategyStandardImpl
```

Sempre que criar uma nova entidade ou coluna, siga essa convenção sem perguntar.

Sempre que criar alguma service com alguma lógica nova, escrever os testes dele para queo sistema esteja 100% coberto com testes. Os testes a nível de banco de dados
ou componentes de interface, que não é o caso, não precisa implementar.

## Regras de negócio — Administrador (`admins`)

- `UserType.ADMIN` tem apenas login (email), senha e nome (`users` + `admins`).
- **Não há endpoint para criar admin**: ele é inserido direto no banco com `docs/sql/admin.sql`. O `/auth/register` recusa `userType = ADMIN`.
- O admin faz login normalmente pelo `/auth/login` e acessa as demais rotas autenticadas; `/auth/me` retorna o nome e os demais campos nulos.
- O Hibernate (`ddl-auto=update`) não atualiza a CHECK `users_usertype_check` ao adicionar valores no enum `UserType`; ao incluir um novo tipo, atualize-a via SQL.

## Regras de negócio — Categorias (`categories`)

- Colunas: `id`, `name` (único), `description`, `createdat`, `updatedat`. Valores salvos exatamente como vieram do front-end.
- Criar, editar e excluir categorias é permitido **apenas ao ADMIN** (`@PreAuthorize("hasRole('ADMIN')")`); demais usuários recebem 403. A leitura (`GET /categories`) é liberada a qualquer usuário autenticado.
- Nome repetido retorna 409. Exclusão só é permitida quando **nenhum produto** usa a categoria (senão 422).
- Não há árvore/subcategorias: a categoria é uma lista simples.

## Regras de negócio — Produtos do prestador (`products`)

- O produto/serviço é cadastrado **apenas pelo PROVIDER** e pertence somente a ele (`providerid`). Rotas em `/providers/me/products` (`@PreAuthorize("hasRole('PROVIDER')")`).
- O prestador é sempre identificado pelo token, nunca pelo corpo/URL; um prestador não enxerga nem altera produtos de outro (retorna 404).
- Campos: `categoryid` (categoria cadastrada pelo admin, obrigatória), `name` (obrigatório), `description` (opcional), `price` (obrigatório, > 0, até 2 casas decimais) e tempo médio.
- **Tempo médio**: na API/tela é `duration` no formato `HH:MM` (de `00:01` a `99:59`); no banco fica em minutos (`durationminutes`). `00:00` retorna 422. Conversão em `common/util/DurationFormat`.
- O prestador pode ter vários produtos na mesma categoria.

## Diagrama do banco

- O diagrama fica em `docs/database.puml` e deve ser atualizado sempre que uma tabela ou coluna for criada ou alterada.
- O Hibernate (`ddl-auto=update`) não remove tabelas/colunas: ao remover ou reestruturar, crie um script em `docs/sql/`.
