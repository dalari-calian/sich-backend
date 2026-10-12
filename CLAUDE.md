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

## Regras de negócio — Catálogo de produtos (`products`)

- Criar, editar e excluir produtos é permitido **apenas ao ADMIN** (`@PreAuthorize("hasRole('ADMIN')")`); demais usuários recebem 403. A leitura é liberada a qualquer usuário autenticado.
- O catálogo é cadastrado pelo **admin** e garante nomenclatura padronizada; o prestador apenas seleciona itens dele (sem criar nomes livres).
- Estrutura em **árvore** numa única tabela recursiva: `parentid` aponta para o produto do nível acima; `parentid` nulo = produto raiz. Não há limite de níveis.
- Colunas atuais: `id`, `parentid`, `name`, `description`, `sku`, `createdat`, `updatedat`. `name` e `description` são salvos exatamente como vieram do front-end (sem tratamento).
- **SKU** gerada pelo sistema, única, com níveis separados por `.`:
  - raiz: sequencial simples (`1`, `2`, `3`...);
  - filho: SKU do pai + `.` + sequência de dois dígitos entre os irmãos (`1.01`, `1.01.02`);
  - a sequência é o maior segmento já usado entre os irmãos + 1;
  - não muda ao editar nome/descrição. O pai de um produto não é alterável (mudar o pai mudaria a SKU dele e de todos os descendentes).
- Referências a produtos em outras tabelas devem usar o `id`, nunca a SKU.
- A SKU também serve para consultas de árvore: descendentes = `sku LIKE '<sku>.%'`; ancestrais = prefixos da SKU.
- Exclusão só é permitida quando o produto **não possui filhos** e **não é prestado por nenhum prestador**.
- Leitura da árvore: uma consulta ordenada por SKU e montagem em memória (não percorrer relacionamentos `@OneToMany` recursivamente).
- O diagrama do banco fica em `docs/database.puml` e deve ser atualizado sempre que uma tabela ou coluna for criada ou alterada.

## Regras de negócio — Produtos do prestador (`providerproducts`)

- O prestador escolhe, entre os produtos do catálogo (`products`), quais ele presta, informando **descrição** (opcional, salva como enviada) e **preço** (obrigatório, > 0, até 2 casas decimais).
- Pode selecionar qualquer item da árvore do catálogo (raiz ou filho).
- Cada produto aparece **uma vez por prestador** (UNIQUE `providerid` + `productid`); repetir retorna 409.
- Rotas em `/providers/me/products`, **apenas para PROVIDER**. O prestador é sempre identificado pelo token, nunca pelo corpo/URL; um prestador não enxerga nem altera registros de outro (retorna 404).
- Pode adicionar, listar, consultar, alterar descrição/preço e remover. O produto do catálogo de um registro não é alterável: para trocar, remove e adiciona outro.
