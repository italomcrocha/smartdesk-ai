# SmartDesk AI

Triagem inteligente de chamados com IA. MVP para hackathon que transforma descrições de problemas em categoria, prioridade, resumo e ação recomendada. Ajuda a encaminhar solicitações com contexto e urgência, sem cadastro, banco de dados ou histórico.

## Tecnologias e arquitetura

Java 17+, Spring Boot 3.5, Maven, Spring Web com RestClient, Bean Validation e HTML/CSS/JavaScript puro. Para executar, prefira JDK 21 ou 25. A interface não usa imagens, fontes ou bibliotecas externas.

**Frontend → Spring Boot → Google Gemini → Spring Boot → Frontend**

- `controller`: endpoint e erros HTTP.
- `service`: regra de negócio e normalização da descrição.
- `client`: comunicação HTTP com Gemini, parsing e validação da saída.
- `dto`: contratos de entrada e saída.
- `config`: CORS.
- `frontend/`: interface; Maven inclui esses arquivos no backend automaticamente, sem duplicação manual.

## 1. Configure a chave

Crie uma chave no [Google AI Studio](https://aistudio.google.com/apikey). Configure a variável no terminal em que iniciará o backend.

PowerShell:

```powershell
$env:GEMINI_API_KEY = 'SUA_CHAVE_AQUI'
# Opcional: substitua por outro modelo disponível na sua conta.
$env:GEMINI_MODEL = 'gemini-3.5-flash-lite'
```

Linux/macOS:

```bash
export GEMINI_API_KEY='SUA_CHAVE_AQUI'
export GEMINI_MODEL='gemini-3.5-flash-lite'
```

A chave existe somente no ambiente do backend e é enviada ao Google no header `x-goog-api-key`. Não a coloque no frontend, no Git nem em screenshots. Arquivos `.env` são ignorados, mas **não são carregados automaticamente** pelo Spring. Sem a chave, a página inicia normalmente e a análise retorna HTTP 503 com orientação amigável.

O modelo é configurável em `application.properties` através de `GEMINI_MODEL`. O padrão é o modelo estável Flash-Lite da [lista oficial de modelos](https://ai.google.dev/gemini-api/docs/models), adequado para classificação de texto. Disponibilidade e cotas dependem da conta Google. A integração usa a [API oficial generateContent](https://ai.google.dev/api/generate-content) com [saída estruturada JSON](https://ai.google.dev/gemini-api/docs/generate-content/structured-output).

## 2. Execute o backend e o frontend juntos

Com Java e Maven no PATH, na pasta `EVENTO HACKHATON`:

```powershell
cd backend
mvn spring-boot:run
```

Abra **http://localhost:8081**. O Spring serve a página e a API no mesmo endereço. Não é necessário um segundo servidor.

Para gerar um executável:

```powershell
mvn clean package
java -jar target/smartdesk-ai-1.0.0.jar
```

Se a máquina tiver vários Javas, aponte `JAVA_HOME` para um JDK compatível e acrescente seu diretório `bin` ao PATH. Maven exige um JDK, não apenas um JRE. A primeira compilação precisa de internet para baixar dependências. `PORT` permite mudar a porta do servidor.

### Maven local desta entrega

Caso `.tools/apache-maven-3.9.11` esteja presente, pode usar o Maven baixado para verificação, sem instalação global. A partir de `EVENTO HACKHATON`, após configurar `JAVA_HOME` e a chave:

```powershell
& '.\.tools\apache-maven-3.9.11\bin\mvn.cmd' "-Dmaven.repo.local=$PWD\.tools\repository" -f backend/pom.xml spring-boot:run
```

`.tools` é uma pasta local ignorada pelo Git; não é requisito para quem clonar o projeto e já tiver Maven.

## 3. Frontend separado (opcional)

Mantenha o backend na porta 8081. Em outro terminal, na pasta `EVENTO HACKHATON`:

```powershell
python -m http.server 5500 --directory frontend
```

Abra **http://localhost:5500**. Também funciona com Live Server na porta 5500. O JavaScript usa `http://localhost:8081` para frontends nas portas 5500 e 3000; servido pelo Spring, usa URL relativa. CORS aceita `localhost` e `127.0.0.1` nessas duas portas. Uma porta personalizada para o frontend exige ajustar a configuração do CORS e `apiBase` em `frontend/script.js`.

## Exemplo de utilização

1. Clique no exemplo “Computador não liga” ou escreva um chamado.
2. Clique em **Analisar chamado**.
3. O navegador envia o JSON ao Spring. O Spring envia as instruções e a descrição ao Gemini com um schema de saída.
4. O backend extrai o texto da resposta, remove blocos markdown quando necessário, converte para DTO e valida os campos.
5. A página exibe a análise com destaque visual para a prioridade. Revise a sugestão antes de encaminhar o chamado.

```http
POST /api/chamados/analisar
Content-Type: application/json

{"descricao":"Meu computador não liga e tenho uma apresentação em 30 minutos."}
```

Exemplo ilustrativo de resposta; a análise real pode variar:

```json
{
  "categoria": "TI",
  "prioridade": "CRITICA",
  "resumo": "Computador não liga e há uma apresentação em 30 minutos.",
  "acaoRecomendada": "Acionar o suporte de TI e providenciar um equipamento alternativo."
}
```

Categorias: TI, RH, Financeiro, Infraestrutura, Atendimento e Outro. Prioridades: CRITICA, ALTA, MEDIA e BAIXA; a página apresenta os acentos para leitura.

## Erros e limites

O contrato de erro é `{"erro":"Mensagem amigável"}`.

| HTTP | Situação |
| --- | --- |
| 400 | Descrição vazia, JSON de entrada inválido ou mais de 10.000 caracteres |
| 429 | Cota do Gemini excedida |
| 502 | Resposta inválida, bloqueada ou incompleta; erro de configuração/autenticação do provedor |
| 503 | Chave ausente ou Gemini indisponível |
| 504 | Falha de comunicação ou tempo limite na conexão com a IA |

O navegador permite uma análise por vez e aguarda até 65 segundos; o backend usa conexão de 10 segundos e leitura de 50 segundos. Não há armazenamento de chamados nem modo de demonstração que invente resultados. O texto é enviado ao Google quando você clica em analisar.

## Testes

```powershell
cd backend
mvn test
```

Os testes usam um servidor HTTP local simulando o Gemini, sem chave real ou consumo de cota. Exercitam o endpoint em um Spring real, JSON e markdown, enums, respostas truncadas, erros de API, validação de entrada, CORS e entrega do frontend sem segredos. Uma chamada ao Gemini real exige chave válida e cota disponível e deve ser conferida antes da apresentação.

## Organização das pastas

Abra EVENTO HACKHATON como pasta do projeto no VS Code ou Codex.

```text
EVENTO HACKHATON/
├── backend/          # API Java / Spring Boot e testes
├── frontend/         # HTML, CSS e JavaScript
├── .tools/           # Maven e dependências locais, ignorados pelo Git
├── iniciar.ps1       # Inicialização pelo PowerShell
└── README.md
```

O endereço padrão é http://localhost:8081. Feche a aba antiga na porta 8080.

Para iniciar pelo PowerShell, na raiz do projeto: `./iniciar.ps1`. Se a chave não estiver no ambiente, o iniciador solicita a chave sem exibi-la e sem gravá-la em arquivo.