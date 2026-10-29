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
- **Situação** (`active`): `true` = ativo, `false` = inativo. Nasce ativo; o prestador altera via `PATCH /providers/me/products/{id}/status`. O prestador continua vendo os inativos na própria lista.
- **Fotos** (`productimages`): até **5 por produto**, cada uma salva como data URL base64 (`data:image/png|jpeg|webp;base64,...`, até 2 MB decodificados). Rotas em `/providers/me/products/{productId}/images` (adicionar, listar, remover). **Toda resposta de produto** (cadastrar, alterar, alterar situação, consultar e listar) traz o campo `images` com as fotos; na listagem, as fotos de todos os produtos são buscadas em uma única consulta (`findByProductIdInOrderByIdAsc`), nunca uma por produto. Ao excluir o produto, as fotos são apagadas em cascata (`ON DELETE CASCADE`).

## Regras de negócio — Agenda (`providerworkinghours`, `appointments`, `appointmentitems`)

- **Grade de 15 minutos**: expediente, início do agendamento e duração dos produtos são sempre múltiplos de 15 (`common/util/TimeGrid`). Produto com duração fora da grade retorna 422.
- **Expediente** (`/providers/me/working-hours`, só PROVIDER): uma linha por intervalo de trabalho (`weekday` 1 = segunda ... 7 = domingo). O almoço é o espaço entre dois intervalos; dia sem linha é folga. O `PUT` substitui a semana inteira e não cancela agendamentos já marcados.
- **Horários são hora local** (`LocalDateTime`, sem fuso). O "agora" vem do bean `Clock` (`app.timezone`, padrão `America/Sao_Paulo`); nos testes, use `Clock.fixed`.
- **O backend calcula o fim**: a requisição traz só `startAt` e `productIds`; `endAt`, `totalMinutes` e `totalPrice` vêm dos produtos. Cada item copia nome, preço e duração do produto (histórico não muda; produto excluído vira `productid` nulo).
- **Validações** ficam em `AppointmentService.book`, usado pelos dois fluxos: produtos do prestador e ativos (404/422), sem repetidos, início na grade, não no passado, até 60 dias à frente (`MAX_DAYS_AHEAD`), começa e termina no mesmo dia, sem sobrepor outro `SCHEDULED` do prestador (409, com a linha do prestador travada via `ProviderService.lockById`).
- **Prestador** (`/providers/me/appointments`): agenda para cliente cadastrado (`customerId`, achado em `/providers/me/customers/lookup` por email ou telefone exatos, nunca por nome) ou sem cadastro (`customerName`). Pode agendar fora do expediente (encaixe).
- **Cliente** (`/customers/me/appointments`): só dentro de um intervalo do expediente (422) e sem outro agendamento dele no mesmo horário (422). Vê também o que o prestador marcou para ele.
- **Disponibilidade** (`/providers/{id}/availability` e `/providers/me/availability`): calculada pela função pura `SlotCalculator` a partir do expediente, dos agendamentos `SCHEDULED` e de `TimeGrid.ceil(agora)`.
- **Cancelamento**: `PATCH .../{id}/cancel` muda para `CANCELED` e grava `canceledat`/`canceledby`; não é permitido depois do início. Cancelado libera o horário.
- A constraint de exclusão e as CHECKs ficam em `docs/sql/appointments-constraints.sql` (o Hibernate não as cria).

## Diagrama do banco

- O diagrama fica em `docs/database.puml` e deve ser atualizado sempre que uma tabela ou coluna for criada ou alterada.
- O Hibernate (`ddl-auto=update`) não remove tabelas/colunas: ao remover ou reestruturar, crie um script em `docs/sql/`.
