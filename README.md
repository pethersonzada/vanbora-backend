# Documentação Técnica do Backend da Solução Van Bora

![Java](https://img.shields.io/badge/Java-ED8B00?style=for-the-badge&logo=openjdk&logoColor=white)
![Spring Boot](https://img.shields.io/badge/Spring_Boot-6DB33F?style=for-the-badge&logo=spring-boot&logoColor=white)
![PostgreSQL](https://img.shields.io/badge/PostgreSQL-4169E1?style=for-the-badge&logo=postgresql&logoColor=white)
![Maven](https://img.shields.io/badge/Maven-C71A36?style=for-the-badge&logo=apache-maven&logoColor=white)

## 1. Visão Geral da Arquitetura do Backend e Padrões de Projeto

O ecossistema backend do Rota Estudantil é estruturado como uma API RESTful de alta performance e escalabilidade horizontal, desenvolvida em Java utilizando o ecossistema Spring Boot. O núcleo do sistema é responsável por processar o fluxo transacional crítico de rotas de transporte escolar, gerenciar vínculos relacionais complexos entre motoristas, turmas, escolas e passageiros, e garantir a persistência íntegra, segura e acidizada (ACID) dos dados operacionais em tempo real.

A arquitetura segue estritamente o padrão de camadas desacopladas (Presentation/Controller, Business/Service, Persistence/Repository, e Domain/Entity), promovendo inversão de controle e injeção de dependência gerenciada pelo container Spring. Isso garante baixo acoplamento entre os componentes, alta testabilidade unitária e de integração, e facilidade de manutenção evolutiva do código-fonte.

## 2. Stack Tecnológica, Frameworks e Dependências Principais

* **Linguagem:** Java 17+ (utilizando recursos modernos como records, sealed classes e pattern matching)
* **Framework Principal:** Spring Boot (versão 3.x)
* **Persistência de Dados:** Spring Data JPA / Hibernate ORM para mapeamento objeto-relacional avançado
* **Banco de Dados Relacional:** PostgreSQL (com suporte a índices espaciais e constraints complexas de integridade)
* **Segurança, Autenticação e Autorização:** Spring Security, JSON Web Tokens (JWT) baseados em claims, e suporte a OAuth2/OIDC para federação de identidade
* **Mapeamento de Objetos e Redução de Boilerplate:** Lombok
* **Gerenciamento de Migrações de Banco:** Flyway para controle versionado e idempotente de scripts SQL estruturados
* **Documentação e Teste de API:** SpringDoc OpenAPI 3 (Swagger UI integrado) para geração automática de contratos REST
* **Gerenciamento de Build:** Maven com plugins de empacotamento otimizados para containers Docker

## 3. Modelo de Domínio, Mapeamento Objeto-Relacional e Estrutura do Banco de Dados

O banco de dados relacional (PostgreSQL) é composto por entidades fortemente tipadas, normalizadas até a terceira forma normal (3NF), e interconectadas por chaves estrangeiras com restrições rigorosas de integridade referencial e deleção em cascata controlada.

### 3.1 Entidades Principais e Relacionamentos

* **Usuário (Tabela: `usuarios`):**
  * Centraliza e armazena dados cadastrais unificados para todos os perfis do sistema (motoristas, passageiros/alunos e gestores institucionais).
  * Campos críticos e restrições: `id` (PK, Long, Auto-increment), `nome` (String, Not Null), `email` (String, Unique, Not Null), `senha` (String, Hash seguro via BCrypt), `telefone` (String), `tipo_usuario` (Enum persistido como String: MOTORISTA, PASSAGEIRO, GESTOR), e timestamps de auditoria (`created_at`, `updated_at`).

* **Turma (Tabela: `turmas`):**
  * Representa o agrupamento lógico institucional de alunos vinculados a uma rota de transporte específica.
  * Campos críticos e restrições: `id` (PK, Long), `nome` (String, Not Null), `turno` (Enum/String: MANHA, TARDE, NOITE), `motorista_id` (FK referenciando `usuarios.id`, permitindo nulos se a turma estiver temporariamente sem alocação de frota).

* **Relacionamento Turma-Aluno (Tabela de Junção Associativa: `turma_alunos`):**
  * Implementa um relacionamento estrutural de muitos-para-muitos (`ManyToMany`) entre as entidades Turmas e Passageiros.
  * Campos de controle: `turma_id` (FK, Not Null), `aluno_id` (FK, Not Null), chave composta otimizada para evitar duplicidade de vínculo para o mesmo aluno na mesma turma.

* **Sessão de Rota / Viagem Ativa (Tabela: `sessoes_rota`):**
  * Gerencia o ciclo de vida operacional do trajeto em tempo real, registrando quando o motorista inicia e finaliza uma linha.
  * Campos críticos: `id` (PK, Long), `turma_id` (FK), `sentido` (Enum: IDA, VOLTA), `status` (Enum: ATIVA, PAUSADA, FINALIZADA), `timestamp_inicio` (DateTime), `timestamp_fim` (DateTime).

* **Presença e Status Diário (Tabela: `presencas`):**
  * Controla o fluxo de confirmação de embarque, desistência ou status de resposta de cada passageiro individual por viagem agendada.

## 4. Arquitetura de Endpoints da API REST (Mapeamento de Rotas e Contratos)

A API expõe recursos padronizados seguindo rigorosamente as restrições arquiteturais REST, utilizando verbos HTTP semânticos e códigos de status padronizados (2xx, 4xx, 5xx).

### 4.1 Módulo de Autenticação e Controle de Acesso (`/api/auth`)
* `POST /api/auth/login`: Autentica credenciais de usuário (email e senha), valida contra o banco via Spring Security e emite um token de acesso JWT assinado com chave criptográfica simétrica/assimétrica.
* `POST /api/auth/registro`: Processa o cadastro inicial de novos usuários no sistema, aplicando validações de formato e unicidade de email.
* `POST /api/auth/refresh`: Renova um token JWT expirado utilizando um token de atualização válido.

### 4.2 Módulo de Gestão de Turmas e Vínculos (`/api/turmas`)
* `GET /api/turmas/motorista/{motoristaId}`: Retorna a listagem otimizada de turmas atribuídas a um motorista específico, incluindo metadados de status.
* `GET /api/turmas/{turmaId}/passageiros`: Retorna a relação detalhada de todos os alunos vinculados a uma turma, acompanhados de seus status de confirmação diária.
* `POST /api/turmas/{turmaId}/alunos/{alunoId}`: Cria um vínculo relacional entre um aluno específico e uma turma ativa, validando se o aluno já não pertence a outra turma no mesmo horário.
* `DELETE /api/turmas/{turmaId}/alunos/{alunoId}`: Remove o vínculo associativo de um aluno com a turma de forma segura (soft delete ou remoção física controlada por transação).

### 4.3 Módulo de Usuários e Alunos Livres (`/api/usuarios`)
* `GET /api/usuarios/passageiros`: Retorna a listagem filtrada de todos os usuários cadastrados com o perfil de passageiro que ainda não possuem vínculo com nenhuma turma (endpoint essencial para alimentar a interface de seleção do motorista).

### 4.4 Módulo de Rotas e Monitoramento Operacional (`/api/rota`)
* `GET /api/rota/status-atual`: Realiza uma checagem de estado operacional global para identificar se há alguma sessão de viagem ativa no momento para o contexto do usuário logado.
* `POST /api/rota/iniciar`: Dispara a transação de abertura de uma nova sessão de rota para uma determinada turma e sentido, alterando o estado global no banco.
* `POST /api/rota/finalizar`: Encerra uma sessão de rota ativa, gravando métricas de duração e desativando o rastreamento em tempo real.

## 5. Estratégias de Tratamento de Erros, Resiliência e Segurança Transacional

* **Global Exception Handler (`@ControllerAdvice` e `@ExceptionHandler`):**
  * Intercepta centralizadamente todas as exceções não tratadas em nível de serviço, repositório ou validação de payload, convertendo-as de maneira uniforme em respostas HTTP padronizadas com payloads JSON descritivos contendo código de erro, timestamp, mensagem amigável e caminho da requisição (ex: `400 Bad Request`, `401 Unauthorized`, `404 Not Found`, `409 Conflict`, `500 Internal Server Error`).

* **Segurança Transacional Robusta (`@Transactional`):**
  * Uso extensivo e criterioso da anotação `@Transactional` nos métodos de serviço de negócio para garantir propriedades ACID completas (Atomicidade, Consistência, Isolamento e Durabilidade) em operações críticas de banco de dados (por exemplo, vínculos múltiplos, exclusões em cascata ou inicialização de rotas que alteram múltiplas tabelas simultaneamente).

* **Validação de Dados de Entrada (Bean Validation / Jakarta Validation):**
  * Aplicação de anotações como `@NotNull`, `@NotBlank`, `@Email`, `@Size` diretamente nos DTOs de entrada da API, bloqueando requisições malformadas antes mesmo que alcancem a camada de persistência.

### Visualizar o Frontend da Aplicação

https://github.com/pethersonzada/rota-estudantil-frontend

## 🔒 Licença e Direitos Autorais

Copyright© 2026 Miguel Petherson Silva. Todos os direitos reservados.

Este software e sua documentação associada (o "Projeto Rota Estudantil") são de propriedade exclusiva do autor. 

É expressamente proibida a cópia, modificação, distribuição, comercialização ou utilização total ou parcial deste código-fonte sem a autorização prévia e expressa por escrito do autor.
