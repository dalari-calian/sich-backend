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
