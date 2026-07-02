# 🔍 Search Jobs — Backend
 
API REST da plataforma **Search Jobs**, responsável pela coleta automatizada de vagas de TI, categorização por stack e senioridade, autenticação de usuários e geração de relatórios.
 
🔗 **API publicada:** [https://search-jobs-back.onrender.com](https://search-jobs-back.onrender.com)  
🔗 **Repositório do frontend:** [https://github.com/ourique787/search-jobs-front](https://github.com/ourique787/search-jobs-front)
 
---
 
## 📋 Sobre o projeto
 
O backend do Search Jobs é o núcleo da plataforma. Ele orquestra a coleta diária de vagas nos portais **InfoJobs**, **empregos.com.br**, **trampos.co** e **Gupy** via web scraping com Selenium WebDriver, realiza o enriquecimento de stacks e senioridade, armazena os dados no PostgreSQL (hospedado no Neon) e expõe uma API REST consumida pelo frontend.
 
---
 
## ✨ Funcionalidades
 
- **Autenticação e cadastro** de usuários com Spring Security + JWT
- **Hash de senhas** com BCrypt
- **Recuperação de senha** via e-mail com SendGrid
- **Web scraping** automatizado e agendado (diariamente ao meio-dia)
- **Pipeline de coleta:**
  1. Coleta de vagas nos portais integrados
  2. Enriquecimento de stacks e senioridade
  3. Enriquecimento de descrição
  4. Remoção de vagas inativas
- **Categorização** de vagas por stack e senioridade
- **Deduplicação** e remoção automática de vagas expiradas
- **Recomendação** de vagas com base nas preferências do usuário
- **Relatórios** de candidaturas e stacks mais demandadas
- **Upload de imagens** de perfil via Cloudinary
- **Integração contínua** com GitHub Actions
---
 
## 🛠️ Tecnologias
 
| Tecnologia | Uso |
|---|---|
| [Java 17+](https://docs.oracle.com/en/java) | Linguagem principal |
| [Spring Boot](https://spring.io/projects/spring-boot) | Framework backend |
| [Spring Security](https://spring.io/projects/spring-security) | Autenticação e autorização |
| [Selenium WebDriver](https://www.selenium.dev) | Web scraping |
| [PostgreSQL](https://www.postgresql.org) | Banco de dados relacional |
| [Neon](https://neon.tech) | Hospedagem do banco de dados |
| [Cloudinary](https://cloudinary.com) | Armazenamento de imagens |
| [SendGrid](https://sendgrid.com) | Envio de e-mails |
| [Gradle](https://gradle.org) | Gerenciamento de dependências e build |
| [GitHub Actions](https://docs.github.com/actions) | Integração contínua (CI/CD) |
| [Render](https://render.com) | Deploy e hospedagem |
 
---
 
## 🚀 Como rodar localmente
 
### Pré-requisitos
 
- Java 17+
- Gradle
- PostgreSQL local ou conta no Neon
- Conta no Cloudinary (para upload de imagens)
- Conta no SendGrid (para envio de e-mails)
- ChromeDriver compatível com o Chrome instalado (para o Selenium)
### Instalação
 
```bash
# Clone o repositório
git clone https://github.com/ourique787/search-jobs-back.git
cd search-jobs-back
```
 
### Variáveis de ambiente
 
Crie um arquivo `application.properties` ou configure as variáveis de ambiente:
 
```properties
# Banco de dados
spring.datasource.url=jdbc:postgresql://localhost:5432/searchjobs
spring.datasource.username=seu_usuario
spring.datasource.password=sua_senha
 
# JWT
jwt.secret=sua_chave_secreta
jwt.expiration=86400000
 
# Cloudinary
cloudinary.cloud-name=seu_cloud_name
cloudinary.api-key=sua_api_key
cloudinary.api-secret=seu_api_secret
 
# SendGrid
sendgrid.api-key=sua_api_key
sendgrid.from-email=seu_email
 
# Frontend URL (para links de recuperação de senha)
app.frontend-url=http://localhost:5173
```
 
### Rodando o projeto
 
```bash
./gradlew bootRun
```
 
A API estará disponível em [http://localhost:8080](http://localhost:8080).
 
---
 
## 🔄 Pipeline de coleta de vagas
 
A coleta é executada automaticamente todos os dias ao meio-dia, seguindo este fluxo:
 
```
1. Coleta simultânea nos portais (thread por portal)
       ↓
2. Enriquecimento de stacks e senioridade
       ↓
3. Enriquecimento de descrição
       ↓
4. Validação e remoção de vagas inativas
```
 
Os portais integrados são: **InfoJobs**, **empregos.com.br**, **trampos.co** e **Gupy**.
 
> Como os portais podem alterar sua estrutura HTML com frequência, os coletores são monitorados via logs de erro para facilitar a manutenção.
 
---
 
## 📁 Estrutura de pacotes
 
```
src/main/java/
├── controller/       # Endpoints da API REST
├── service/          # Regras de negócio
├── repository/       # Acesso ao banco de dados (JPA)
├── model/            # Entidades do banco
├── dto/              # Objetos de transferência de dados
├── security/         # Configuração Spring Security + JWT
├── scraper/          # Coletores por portal (Selenium)
└── config/           # Configurações gerais
```
 
> Ajuste a estrutura acima conforme a organização real do seu projeto.
 
---
 
## 🌐 Deploy
 
O backend está hospedado no **Render** (plano gratuito) e o banco de dados no **Neon**.
 
🔗 [https://search-jobs-back.onrender.com](https://search-jobs-back.onrender.com)
 
> **Atenção:** o serviço pode entrar em modo hibernação após períodos de inatividade. Na primeira requisição, aguarde alguns segundos para o serviço retomar.
 
---
 
## 🔗 Repositórios relacionados
 
- [search-jobs-front](https://github.com/ourique787/search-jobs-front) — Interface web (React + TypeScript)
---
 
## 👨‍💻 Autor
 
**Lucas Ourique Trajano**  
Trabalho de Conclusão de Curso — Análise e Desenvolvimento de Sistemas  
ULBRA Torres/RS — 2026
