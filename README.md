# Gerenciador de Biblioteca Pessoal

Aplicação web completa para gerenciamento de acervo pessoal de livros com autenticação de usuários.

## Stack

| Camada | Tecnologia |
|---|---|
| Backend | Spring Boot 3.3, Java 21 |
| Banco de dados | MongoDB 7 (NoSQL) |
| Frontend | Thymeleaf + Bootstrap 5 |
| Testes | JUnit 5, Testcontainers, WireMock (VCR) |
| Cobertura | JaCoCo ≥ 80% |
| Qualidade | SonarQube / SonarCloud |
| CI/CD | GitHub Actions |
| Container | Docker + Docker Compose |

## Funcionalidades

- Cadastro e autenticação de usuários (sessão HTTP)
- CRUD completo de livros (título, autor, ISBN, gênero, ano, sinopse, capa)
- Filtro por status: Quero Ler / Lendo / Lido
- Busca por título ou autor
- Avaliação de 1 a 5 estrelas
- Busca automática de metadados por ISBN (Open Library API)
- Dashboard com estatísticas do acervo
- Design responsivo (mobile-first)

## Pré-requisitos

- Java 21+
- Maven 3.9+
- Docker (para rodar MongoDB local)

## Executar localmente

```bash
# 1. Subir MongoDB
docker-compose up -d mongodb

# 2. Rodar a aplicação
mvn spring-boot:run

# Acesse: http://localhost:8080
```

## Executar com Docker Compose (full stack)

```bash
mvn package -DskipTests
docker-compose up --build
```

## Rodar os Testes

```bash
# Todos os testes + relatório de cobertura
mvn verify

# Relatório JaCoCo gerado em:
# target/site/jacoco/index.html
```

> **Atenção:** Os testes usam **Testcontainers** (MongoDB real via Docker) e **WireMock** (VCR para API externa). Nenhum mock é utilizado.

## Estrutura do Projeto

```
src/
├── main/
│   ├── java/com/biblioteca/
│   │   ├── config/          # SecurityConfig, RestTemplateConfig
│   │   ├── controller/      # AuthController, BookController, ExternalBookController, WebController
│   │   ├── dto/             # BookRequest, BookResponse, RegisterRequest, ExternalBookInfo
│   │   ├── exception/       # GlobalExceptionHandler, ResourceNotFoundException, BusinessException
│   │   ├── model/           # User, Book, BookStatus
│   │   ├── repository/      # UserRepository, BookRepository
│   │   └── service/         # AuthService, BookService, ExternalBookService, UserDetailsServiceImpl
│   └── resources/
│       ├── templates/       # Thymeleaf (login, register, dashboard, books/*)
│       └── static/          # CSS, JS
└── test/
    └── java/com/biblioteca/
        ├── config/          # MongoTestContainer (shared Testcontainers config)
        ├── controller/      # AuthControllerTest, BookControllerTest (caixa preta)
        ├── integration/     # BookIntegrationTest (fluxo completo)
        ├── parameterized/   # BookValidationTest (múltiplos cenários)
        ├── service/         # AuthServiceTest, BookServiceTest (com Testcontainers)
        ├── vcr/             # ExternalBookVcrTest (WireMock/VCR)
        └── whitebox/        # BookServiceWhiteboxTest (lógica interna)
```

## Estratégia de Testes

| Tipo | Classe | Descrição |
|---|---|---|
| Unitário/Integração | `AuthServiceTest`, `BookServiceTest` | Serviços com MongoDB real (Testcontainers) |
| Caixa Preta (E2E) | `AuthControllerTest`, `BookControllerTest` | Controllers via MockMvc |
| Parametrizados | `BookValidationTest` | Múltiplos cenários de validação |
| Caixa Branca | `BookServiceWhiteboxTest` | Branches internos do BookService |
| VCR (WireMock) | `ExternalBookVcrTest` | Cassetes gravados para Open Library API |
| Integração completa | `BookIntegrationTest` | Fluxo usuário + livros end-to-end |

> **Regra:** mocks (`@Mock`, `@MockBean`) são **proibidos**. Toda persistência usa Testcontainers; toda API externa usa WireMock.

## CI/CD

O pipeline `.github/workflows/ci.yml` executa automaticamente em push/PR:

1. Build com Maven
2. Testes com Testcontainers + WireMock
3. Geração do relatório JaCoCo
4. Análise SonarQube

### Configurar SonarCloud

1. Criar projeto no [sonarcloud.io](https://sonarcloud.io)
2. Adicionar secrets no repositório GitHub:
   - `SONAR_TOKEN` — token gerado no SonarCloud
3. Configurar variáveis opcionais:
   - `SONAR_PROJECT_KEY` e `SONAR_ORG`

## API REST

| Método | Endpoint | Descrição |
|---|---|---|
| GET | `/api/books` | Listar livros (filtros: `?status=`, `?q=`) |
| POST | `/api/books` | Criar livro |
| GET | `/api/books/{id}` | Detalhar livro |
| PUT | `/api/books/{id}` | Atualizar livro |
| DELETE | `/api/books/{id}` | Remover livro |
| GET | `/api/books/stats` | Estatísticas do acervo |
| GET | `/api/external/lookup?isbn={isbn}` | Buscar metadados por ISBN |

Todas as rotas `/api/**` exigem autenticação (cookie de sessão).

## Documentação

- [RTM.md](RTM.md) — Matriz de Rastreabilidade com Diagramas UML de Sequência (Mermaid)
