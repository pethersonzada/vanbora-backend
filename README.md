# BeVans - Documentação Técnica do Backend

![Java](https://img.shields.io/badge/Java-ED8B00?style=for-the-badge&logo=openjdk&logoColor=white)
![Spring Boot](https://img.shields.io/badge/Spring_Boot-6DB33F?style=for-the-badge&logo=spring-boot&logoColor=white)
![PostgreSQL](https://img.shields.io/badge/PostgreSQL-4169E1?style=for-the-badge&logo=postgresql&logoColor=white)
![Maven](https://img.shields.io/badge/Maven-C71A36?style=for-the-badge&logo=apache-maven&logoColor=white)

> ⚠️ **Aviso de Propriedade Intelectual:** Este repositório é de uso estritamente pessoal e acadêmico, mantido no GitHub apenas para controle de versão do projeto **BeVans**. A cópia, distribuição, comercialização ou uso não autorizado do código-fonte por terceiros é expressamente proibida.

## 1. Visão Geral da Arquitetura do Backend e Padrões de Projeto

O ecossistema backend do **BeVans** é estruturado como uma API RESTful de alta performance e escalabilidade horizontal, desenvolvida em Java utilizando o ecossistema Spring Boot. O núcleo do sistema é responsável por processar o fluxo transacional crítico de rotas de transporte escolar, gerenciar vínculos relacionais complexos entre motoristas, turmas, escolas e passageiros, e garantir a persistência íntegra, segura e acidizada (ACID) dos dados operacionais em tempo real.

A arquitetura segue estritamente o padrão de camadas desacopladas (Presentation/Controller, Business/Service, Persistence/Repository, e Domain/Entity), promovendo inversão de controle e injeção de dependência gerenciada pelo container Spring. Isso garante baixo acoplamento entre os componentes, testabilidade unitária e facilidade de manutenção evolutiva.

## 2. Autenticação, Segurança e Regras de Acesso

Todas as rotas protegidas exigem obrigatoriamente o envio do token JWT emitido pelo Firebase Authentication no cabeçalho HTTP de autorização:

```
Authorization: Bearer <FIREBASE_ID_TOKEN>
```

### Regras Críticas de Negócio:
* **Validação de Papel (*Role-Based Access Control*):** O backend NUNCA confia cegamente no tipo de usuário enviado pelo payload do cliente. O registro e privilégios de motoristas passam por validação estrita de chaves de autorização ou regras de negócio no servidor.
* **Idempotência:** Endpoints transacionais críticos implementam tratamento contra duplicidade, evitando registros órfãos no banco de dados em caso de instabilidade na rede mobile.

## 3. Principais Endpoints (Contratos de API)

### 3.1 Usuários
| Método | Endpoint | Descrição |
| :--- | :--- | :--- |
| `POST` | `/usuarios/cadastrar` | Registra o perfil do usuário no banco relacional após autenticação no Firebase. |
| `GET` | `/usuarios/por-uid/{uid}` | Retorna as informações cadastrais do usuário filtradas por UID. |
| `GET` | `/usuarios/passageiros` | Lista todos los passageiros vinculados e seus respectivos status de presença. |
| `DELETE` | `/usuarios/{id}` | Remove o registro do usuário (exclusão de conta). |

### 3.2 Turmas e Vínculos
| Método | Endpoint | Descrição |
| :--- | :--- | :--- |
| `POST` | `/turmas` | Cria uma nova turma (Nome, Turno, ID do Motorista). |
| `PUT` | `/turmas/{id}` | Atualiza dados cadastrais, origem (garagem) e destino da turma. |
| `POST` | `/turmas/entrar/{userId}?codigo=XXXXXX` | Processa a solicitação de vínculo do passageiro via código de convite. |
| `PUT` | `/turmas/analisar/{vinculoId}?status=APROVADO` | Aprova ou rejeita o vínculo do aluno requisitante. |
| `PUT` | `/turmas/{id}/regenerar-codigo` | Invalida a chave atual e gera um novo código alfanumérico de 6 caracteres. |

### 3.3 Rotas e Geolocalização
| Método | Endpoint | Descrição |
| :--- | :--- | :--- |
| `GET` | `/rota/status-atual?turmaId=` | Identifica se há viagem em andamento e o sentido (`IDA` / `VOLTA`). |
| `GET` | `/rota/otimizar?sentido=&turmaId=` | Executa o algoritmo de ordenação de paradas com base nas confirmações. |
| `POST` | `/rota/iniciar` | Inicializa o ciclo de viagem para a turma informada. |
| `POST` | `/rota/localizacao-van` | Persiste a coordenada geográfica atual da van enviada pelo motorista (~2s). |
| `GET` | `/rota/localizacao-van` | Fornece a posição em tempo real para os clientes (radar dos passageiros). |
| `POST` | `/rota/confirmar?usuarioId=&status=&enderecoId=` | Atualiza a intenção de presença diária do passageiro. |
| `POST` | `/rota/encerrar` | Encerra formalmente o ciclo de viagem ativo. |

## 4. Pré-requisitos, Instalação e Configuração

### 4.1 Requisitos de Ambiente
* Java Development Kit (JDK) 17 ou superior.
* Apache Maven 3.8+.
* Banco de Dados PostgreSQL 14+.
* Conta Google Firebase configurada com Firebase Admin SDK.

### 4.2 Variáveis de Ambiente (`application.yml` / `.env`)
```env
SERVER_PORT=8080
SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/bevans_db
SPRING_DATASOURCE_USERNAME=postgres
SPRING_DATASOURCE_PASSWORD=sua_senha
FIREBASE_CONFIG_PATH=classpath:firebase-service-account.json
MAPBOX_SECRET_TOKEN=sk.xxxxxxxxxxxx
```

