# RTM — Matriz de Rastreabilidade de Requisitos
## Gerenciador de Biblioteca Pessoal

---

## RF01 — Cadastro de Usuário

| Campo | Detalhe |
|---|---|
| Descrição | O sistema deve permitir o cadastro de novos usuários com username, email e senha |
| Implementação | `AuthService#register`, `AuthController#register`, `User` |
| Testes | `AuthServiceTest`, `AuthControllerTest`, `BookValidationTest` |
| Cobertura | Campos obrigatórios, senhas divergentes, username/email duplicado |

### Diagrama UML de Sequência — RF01

```mermaid
sequenceDiagram
    actor U as Usuário
    participant C as AuthController
    participant S as AuthService
    participant R as UserRepository
    participant DB as MongoDB

    U->>C: POST /register {username, email, password, confirmPassword}
    C->>C: Valida @Valid (Bean Validation)
    alt Dados inválidos
        C-->>U: 200 register.html (com erros de campo)
    end
    C->>S: register(RegisterRequest)
    S->>S: Verifica password == confirmPassword
    alt Senhas divergem
        S-->>C: throws BusinessException
        C-->>U: 200 register.html (com erro)
    end
    S->>R: existsByUsername(username)
    alt Username existe
        S-->>C: throws BusinessException("Username já em uso")
        C-->>U: 200 register.html (com erro)
    end
    S->>R: existsByEmail(email)
    alt Email existe
        S-->>C: throws BusinessException("Email já em uso")
        C-->>U: 200 register.html (com erro)
    end
    S->>S: BCrypt.encode(password)
    S->>R: save(User)
    R->>DB: insert users
    DB-->>R: User{id, ...}
    R-->>S: User salvo
    S-->>C: User
    C-->>U: redirect /login?success
```

---

## RF02 — Autenticação e Gerenciamento de Sessão

| Campo | Detalhe |
|---|---|
| Descrição | O sistema deve autenticar usuários com username/senha e manter sessão HTTP |
| Implementação | `SecurityConfig`, `UserDetailsServiceImpl`, Spring Security Form Login |
| Testes | `AuthControllerTest#shouldRedirectDashboardWhenUnauthenticated` |
| Cobertura | Login válido, credenciais inválidas, acesso sem sessão, logout |

### Diagrama UML de Sequência — RF02

```mermaid
sequenceDiagram
    actor U as Usuário
    participant F as Spring Security Filter
    participant UDS as UserDetailsServiceImpl
    participant R as UserRepository
    participant DB as MongoDB

    U->>F: POST /login {username, password}
    F->>UDS: loadUserByUsername(username)
    UDS->>R: findByUsername(username)
    R->>DB: query users
    DB-->>R: User | null
    alt Usuário não encontrado
        UDS-->>F: throws UsernameNotFoundException
        F-->>U: redirect /login?error=true
    end
    R-->>UDS: User
    UDS-->>F: UserDetails{username, encodedPwd, roles}
    F->>F: BCrypt.matches(rawPwd, encodedPwd)
    alt Senha incorreta
        F-->>U: redirect /login?error=true
    end
    F->>F: Cria HttpSession
    F-->>U: redirect /dashboard (Set-Cookie: JSESSIONID)

    Note over U,F: Requisições subsequentes usam JSESSIONID
    U->>F: GET /dashboard (Cookie: JSESSIONID=...)
    F->>F: Recupera Authentication da sessão
    F-->>U: 200 dashboard.html
```

---

## RF03 — Cadastro de Livro (CREATE)

| Campo | Detalhe |
|---|---|
| Descrição | Usuário autenticado pode adicionar livros com título, autor, ISBN, gênero, status, avaliação |
| Implementação | `BookService#create`, `BookController#create`, `WebController#newBookPage` |
| Testes | `BookServiceTest#shouldCreateBook`, `BookControllerTest#shouldCreateBook`, `BookValidationTest` |
| Cobertura | Criação válida, campos obrigatórios, ISBN duplicado, status padrão |

### Diagrama UML de Sequência — RF03

```mermaid
sequenceDiagram
    actor U as Usuário
    participant C as BookController
    participant S as BookService
    participant R as BookRepository
    participant DB as MongoDB

    U->>C: POST /api/books {title, author, isbn, ...}
    C->>C: @Valid BookRequest
    alt Dados inválidos
        C-->>U: 400 Bad Request {errors}
    end
    C->>C: getUserId(principal) → userId
    C->>S: create(BookRequest, userId)
    alt ISBN preenchido
        S->>R: existsByIsbnAndUserId(isbn, userId)
        R->>DB: count query
        DB-->>R: count
        alt ISBN já existe
            S-->>C: throws BusinessException
            C-->>U: 400 {"message": "ISBN já existe..."}
        end
    end
    S->>S: mapToBook(request) + userId
    S->>R: save(Book)
    R->>DB: insert books
    DB-->>R: Book{id, ...}
    R-->>S: Book salvo
    S-->>C: BookResponse
    C-->>U: 201 Created {id, title, author, ...}
```

---

## RF04 — Listagem e Busca de Livros (READ)

| Campo | Detalhe |
|---|---|
| Descrição | Usuário pode listar, filtrar por status e pesquisar livros do seu acervo |
| Implementação | `BookService#findAll/findByStatus/search`, `BookController#list`, `WebController#dashboard` |
| Testes | `BookServiceTest#shouldListBooks/shouldFilterByStatus/shouldSearch`, `BookControllerTest#shouldFilterByStatusParam` |
| Cobertura | Lista vazia, filtro por status, busca por título/autor, isolamento por userId |

### Diagrama UML de Sequência — RF04

```mermaid
sequenceDiagram
    actor U as Usuário
    participant C as BookController
    participant S as BookService
    participant R as BookRepository
    participant DB as MongoDB

    U->>C: GET /api/books?status=READING&q=tolkien
    C->>C: getUserId(principal)
    alt q preenchido
        C->>S: search(userId, q)
        S->>R: searchByUserIdAndQuery(userId, q)
        R->>DB: regex query {userId, $or:[title,author]}
    else status preenchido
        C->>S: findByStatus(userId, status)
        S->>R: findByUserIdAndStatus(userId, status)
        R->>DB: query {userId, status}
    else sem filtro
        C->>S: findAll(userId)
        S->>R: findByUserId(userId)
        R->>DB: query {userId}
    end
    DB-->>R: List<Book>
    R-->>S: List<Book>
    S-->>C: List<BookResponse>
    C-->>U: 200 OK [...]
```

---

## RF05 — Edição de Livro (UPDATE)

| Campo | Detalhe |
|---|---|
| Descrição | Usuário pode editar todos os campos de um livro do seu acervo |
| Implementação | `BookService#update`, `BookController#update` |
| Testes | `BookServiceTest#shouldUpdateBook`, `BookControllerTest#shouldUpdateBook`, `BookServiceWhiteboxTest#shouldUpdateTimestamp` |
| Cobertura | Atualização válida, livro de outro usuário, updatedAt atualizado |

### Diagrama UML de Sequência — RF05

```mermaid
sequenceDiagram
    actor U as Usuário
    participant C as BookController
    participant S as BookService
    participant R as BookRepository
    participant DB as MongoDB

    U->>C: PUT /api/books/{id} {title, author, status, ...}
    C->>S: update(id, BookRequest, userId)
    S->>R: findByIdAndUserId(id, userId)
    R->>DB: query {_id, userId}
    alt Livro não encontrado / outro dono
        R-->>S: Optional.empty()
        S-->>C: throws ResourceNotFoundException
        C-->>U: 404 Not Found
    end
    DB-->>R: Book
    R-->>S: Optional<Book>
    S->>S: Atualiza campos + updatedAt = now()
    S->>R: save(Book)
    R->>DB: update
    DB-->>R: Book atualizado
    R-->>S: Book
    S-->>C: BookResponse
    C-->>U: 200 OK {id, title, ...}
```

---

## RF06 — Remoção de Livro (DELETE)

| Campo | Detalhe |
|---|---|
| Descrição | Usuário pode remover livros do seu acervo |
| Implementação | `BookService#delete`, `BookController#delete` |
| Testes | `BookServiceTest#shouldDeleteBook`, `BookControllerTest#shouldDeleteBook`, `BookServiceWhiteboxTest#shouldThrowOnDeleteWrongOwner` |
| Cobertura | Remoção válida, livro de outro usuário |

### Diagrama UML de Sequência — RF06

```mermaid
sequenceDiagram
    actor U as Usuário
    participant C as BookController
    participant S as BookService
    participant R as BookRepository
    participant DB as MongoDB

    U->>C: DELETE /api/books/{id}
    C->>S: delete(id, userId)
    S->>R: findByIdAndUserId(id, userId)
    R->>DB: query {_id, userId}
    alt Não encontrado
        S-->>C: throws ResourceNotFoundException
        C-->>U: 404 Not Found
    end
    DB-->>R: Book
    S->>R: delete(Book)
    R->>DB: deleteOne
    DB-->>R: OK
    C-->>U: 204 No Content
```

---

## RF07 — Busca de Livro por ISBN (API Externa)

| Campo | Detalhe |
|---|---|
| Descrição | O sistema consulta a API Open Library para obter metadados de um livro pelo ISBN |
| Implementação | `ExternalBookService#lookupByIsbn`, `ExternalBookController#lookup` |
| Testes | `ExternalBookVcrTest` (WireMock/VCR) |
| Cobertura | ISBN encontrado, não encontrado, erro 500, JSON malformado, 1 chamada por requisição |

### Diagrama UML de Sequência — RF07

```mermaid
sequenceDiagram
    actor U as Usuário
    participant C as ExternalBookController
    participant S as ExternalBookService
    participant RT as RestTemplate
    participant OL as Open Library API

    U->>C: GET /api/external/lookup?isbn=9780132350884
    C->>S: lookupByIsbn("9780132350884")
    S->>RT: GET {baseUrl}/api/books?bibkeys=ISBN:...&format=json&jscmd=data
    RT->>OL: HTTP GET
    alt Erro de rede / 5xx
        OL-->>RT: Exception
        RT-->>S: Exception capturada
        S-->>C: Optional.empty()
        C-->>U: 404 Not Found
    end
    OL-->>RT: 200 {"ISBN:...": {...}}
    RT-->>S: JSON String
    S->>S: parseOpenLibraryResponse(isbn, json)
    alt JSON vazio / chave não encontrada
        S-->>C: Optional.empty()
        C-->>U: 404 Not Found
    end
    S->>S: Preenche ExternalBookInfo
    S-->>C: Optional<ExternalBookInfo>
    C-->>U: 200 OK {title, author, publishedYear, coverUrl, ...}
```

---

## RF08 — Estatísticas do Acervo

| Campo | Detalhe |
|---|---|
| Descrição | O sistema exibe contadores por status (total, lendo, lido, quero ler) |
| Implementação | `BookService#getStats`, `BookController#stats` |
| Testes | `BookServiceTest#shouldReturnStats`, `BookControllerTest#shouldReturnStats`, `BookServiceWhiteboxTest#shouldReturnZeroStatsWhenEmpty` |
| Cobertura | Acervo com livros mistos, acervo vazio |

---

## Cobertura por Tipo de Teste

| Tipo | Classe(s) | RFs cobertos |
|---|---|---|
| Unitário / Integração (Testcontainers) | `AuthServiceTest`, `BookServiceTest` | RF01, RF03–RF08 |
| Caixa Preta / E2E Controller | `AuthControllerTest`, `BookControllerTest` | RF01–RF06, RF08 |
| Parametrizado | `BookValidationTest` | RF01, RF03 |
| Caixa Branca | `BookServiceWhiteboxTest` | RF03–RF08 |
| VCR (WireMock) | `ExternalBookVcrTest` | RF07 |
| Integração completa | `BookIntegrationTest` | RF01, RF03–RF08 |

> **Meta:** ≥ 80% de cobertura de instruções, validada pelo `jacoco-maven-plugin:check` em `mvn verify`.
