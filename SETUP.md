# Como subir o projeto

## O que instalar

| Ferramenta | Versão | Link |
|---|---|---|
| JDK | 25 | https://jdk.java.net/25/ |
| Docker Desktop | qualquer | https://www.docker.com/products/docker-desktop |
| VS Code | qualquer | https://code.visualstudio.com |

---

## Extensões do VS Code

Instale as 3 extensões abaixo (pesquise pelo ID na aba de extensões):

- `vscjava.vscode-java-pack` — suporte a Java
- `vmware.vscode-spring-boot` — suporte ao Spring Boot
- `redhat.java` — language server Java

---

## Passo a passo

**1. Clone o repositório**
```bash
git clone <url-do-repo>
cd sich-backend
```

**2. Suba o banco de dados (Docker)**
```bash
docker compose up -d
```
> O PostgreSQL vai rodar na porta `5432`. Só precisa fazer isso uma vez (ou quando reiniciar o PC).

**3. Rode a aplicação**

Abra o VS Code, pressione `F5` e selecione **"Sich Backend"**.

Ou pelo terminal:
```bash
./mvnw spring-boot:run
```

**4. Pronto!**

A API estará disponível em: `http://localhost:4100`

---

## Parar tudo

```bash
docker compose down
```

---

## Dicas rápidas

- **Banco travado / não conecta** → verifique se o Docker Desktop está aberto e rode `docker compose up -d` novamente.
- **Porta 5432 ocupada** → algum PostgreSQL local já está rodando. Pare-o ou mude a porta no `docker-compose.yml`.
- **Hot reload ativo** → use o perfil **"Sich Backend (debug com hot reload)"** no `F5`.
