# Malinha

O projeto Android está nesta raiz. O guia de estudo pode ser baixado em [Guia do código e funcionamento](docs/Malinha-Guia-do-Codigo-e-Funcionamento.pdf).

Para compilar em outro computador, configure suas próprias chaves locais e adicione o arquivo `app/google-services.json` do seu projeto Firebase. Os arquivos de exemplo de configuração acompanham o código.

O Malinha é um planejador de viagens para Android, desenvolvido como trabalho acadêmico em Java e XML. Permite organizar atividades por dia, salvar roteiros no aparelho, descobrir destinos, consultar informações de viagem e anotar reservas de voos.

A interface está em português do Brasil. O planejamento manual e a consulta dos roteiros salvos funcionam offline. Consultas de mapas, estabelecimentos, câmbio, feriados, fotografias e sugestões do Gemini dependem dos serviços externos correspondentes.


## Identidade visual

| Elemento | Valor |
| --- | --- |
| Coral | `#FF6B4A` |
| Azul-petróleo | `#1F4E5F` |
| Amarelo-areia | `#FFD97A` |
| Creme | `#FFF8EE` |

Cores, textos, tema, ícones e formas ficam em `app/src/main/res/`. O app usa cantos arredondados e tipografia arredondada do sistema. O mascote e as ilustrações iniciais foram preparados em XML com base nas referências visuais. Fotografias são carregadas pelos serviços de imagens quando disponíveis.

## Funcionalidades

### Início e abertura

A Splash Screen leva ao início, que oferece atalhos para Planejar viagem, Meus roteiros, Hotéis, Câmbio, Voos e Explorar destinos. A navegação inferior dá acesso ao início, aos roteiros e à exploração. A home também apresenta viagens salvas próximas.

### Planejar viagem e editar roteiros

O planejamento segue os passos Destino, Datas, Viajantes e estilo, e Roteiro.

- Destino com autocomplete do Google Places quando configurado.
- Datas pelo calendário e atalhos de fim de semana, próximo feriado brasileiro e daqui a 30 dias.
- Duração em noites, quantidade de viajantes, ritmo, orçamento e interesses.
- Um dia por página no editor, com navegação por deslize horizontal e abas com data.
- Abertura no dia atual quando a viagem está em andamento.
- Preservação do dia selecionado e das posições de rolagem durante a recriação da tela.
- Adição, edição, exclusão com confirmação e reordenação por botões Subir e Descer.
- Nome, horário inicial e final, descrição, categoria e custo estimado em cada atividade.
- Botão Salvar roteiro para persistir as alterações em Meus roteiros.

A ordem das atividades é manual. Os horários de uma atividade pertencem ao mesmo dia, e a sequência pode ser ajustada independentemente deles. Encurtar uma viagem pela edição do planejamento é validado para evitar deixar atividades fora do período.

### Meus roteiros

Lista viagens salvas, permite abrir, editar e excluir. Os roteiros permanecem disponíveis após fechar o aplicativo. O armazenamento usa SQLite com uma linha por viagem e listas de interesses e atividades representadas em JSON. Salvar novamente atualiza a mesma viagem.

### Explorar destinos

Catálogo offline de 24 destinos, busca local por cidade ou país, categorias, filtro de região e carrosséis de cartões com fotos. Os cartões permitem favoritar e abrir detalhes, atrações e acesso ao planejamento com o destino preenchido.

Há oito modelos editáveis com quatro atividades por dia.

| Destino | Duração |
| --- | --- |
| Rio de Janeiro | 5 dias |
| Salvador | 5 dias |
| Santiago | 5 dias |
| Buenos Aires | 5 dias |
| Lisboa | 5 dias |
| Nova York | 5 dias |
| Paris | 6 dias |
| Tóquio | 7 dias |

Os horários dos modelos são sugestões. É necessário conferir abertura, ingressos e deslocamentos. O usuário escolhe a data de início e salva uma cópia editável no aparelho.

### Sugestões com IA

O Firebase AI Logic envia pedidos ao Gemini para obter sugestões estruturadas. A interface mostra cartões com nome, categoria, descrição, duração e horário sugeridos. É possível selecionar atrações, consultar detalhes, pedir novas sugestões e adicionar as escolhidas ao dia do roteiro.

As sugestões são convertidas em atividades das classes existentes. O fluxo valida duplicações no dia e procura espaço nos horários. As fotografias usam a integração Wikimedia.

No código atual, o botão de sugestões aparece apenas no build debug. A classe `FirebaseAIService.java` fica diretamente no pacote `com.example.malinha`. O modelo configurado é `gemini-3.1-flash-lite`. Disponibilidade, acesso e cotas precisam ser conferidos no projeto Firebase utilizado.

### Câmbio

O conversor usa AwesomeAPI, apresenta o resultado e a data da cotação e faz o cálculo com `BigDecimal`. Conversões entre duas moedas estrangeiras usam os pares com BRL para calcular a relação. Moedas iguais usam taxa 1 localmente. Consultas HTTP rodam fora da thread principal e tratam erros de conexão e respostas inválidas.

### Hotéis, atrações, restaurantes e mapas

Hotéis permite pesquisar estabelecimentos pelo Places e abrir a pesquisa no Booking.com. A consulta de atrações e restaurantes mostra os dados recebidos do Google, como endereço, nota, avaliações e horários quando disponíveis.

O mapa interno usa Maps SDK for Android e resolve buscas por nome pelo Geocoder do Android. O usuário pode selecionar o endereço retornado. O acesso para traçar rota abre o Google Maps externo. A busca e a exibição do mapa dependem de rede, configuração e serviços do aparelho.

### Voos e reservas anotadas

A tela Voos oferece origem e destino com autocomplete offline por cidade, aeroporto ou código IATA. A base local reúne 6.071 registros filtrados do OpenFlights. A pesquisa ignora acentos e diferenças entre maiúsculas e minúsculas, com prioridade para aeroportos brasileiros e hubs.

- Inversão entre origem e destino.
- Opção Somente ida.
- Seleção de 1 a 9 adultos e classe.
- Última origem usada e histórico local das últimas cinco pesquisas.
- Abertura de Google Voos, Skyscanner, Kayak, Decolar e Momondo.
- Selecionar uma sugestão permite enviar o código IATA ao comparador.
- Na Decolar, a configuração atual abre a página de busca para preenchimento manual.
- A aplicação dos filtros é controlada pelo site externo.

Em Meus voos é possível anotar companhia, número do voo, origem, destino, partida, chegada opcional, localizador e observações. As anotações podem ser editadas e excluídas. O localizador aparece oculto até o usuário solicitar sua exibição. O arquivo privado de reservas fica em `getNoBackupFilesDir()` e é atualizado com `AtomicFile`.

O vínculo dessas anotações ao roteiro e à contagem regressiva da home, o checklist por voo e as notificações de embarque pertencem ao escopo solicitado para evolução e ainda estão pendentes.

### Países e feriados

Em Explorar → Países e feriados, o usuário escolhe país e ano. Dados de país e feriados são consultas independentes, de forma que a falha de uma fonte permite apresentar os resultados da outra.

Fins de semana prolongados são calculados localmente a partir dos feriados nacionais do tipo Public e do descanso aos sábados e domingos. O cálculo considera o ano consultado e representa uma sugestão de calendário.

## APIs, SDKs e dados usados

| Integração | Uso no Malinha | Configuração |
| --- | --- | --- |
| Google Maps SDK for Android | Mapa interno e marcadores | Chave em `maps.properties` |
| Google Places SDK for Android (New) | Autocomplete de cidades e consulta de locais, restaurantes e hotéis | Chave em `places.properties` |
| AwesomeAPI | Cotações de moedas | A implementação atual consulta o endpoint público |
| MediaWiki PageImages da Wikipédia em português | Localizar a imagem associada ao artigo do destino ou atração | Sem chave no app |
| Wikimedia Commons / MediaWiki | Buscar fotos, miniaturas e metadados de autoria e licença | Sem chave no app |
| Firebase AI Logic / Gemini Developer API | Sugestões de atrações em JSON estruturado | Projeto Firebase e acesso ao modelo |
| Firebase App Check | Autorização das chamadas de IA na variante de desenvolvimento | Provedor debug e token cadastrado no Console |
| Nager.Date / Nager.Holidays | Feriados por país e ano | Endpoint v4 utilizado pelo serviço atual |
| REST Countries | Idiomas, moedas, fusos e códigos telefônicos | Endpoint v5 com token Bearer |
| OpenFlights | Catálogo de aeroportos embarcado no APK | Asset TSV sob ODbL 1.0 |

### Google Maps e Places

O Maps mostra mapas e marcadores. O Places fornece os dados dos locais e o autocomplete. São configurações distintas, mesmo quando pertencem ao mesmo projeto Google Cloud. O projeto usa Maps SDK 20.1.0 e Places SDK 5.2.0.

Arquivos principais são `activities/MapActivity.java`, `services/PlacesClientProvider.java`, `services/CitySearchRepository.java`, `services/ExplorePlacesService.java` e `services/HotelPlacesService.java`. A inicialização do Places usa a API New e o idioma português do Brasil.

Habilite os produtos usados no Google Cloud e confira faturamento, restrições da chave, pacote Android e certificado de assinatura. Alterar as propriedades exige gerar um novo APK. [Maps SDK](https://developers.google.com/maps/documentation/android-sdk/overview) · [Places SDK](https://developers.google.com/maps/documentation/places/android-sdk/overview)

### AwesomeAPI

`CurrencyService.java` consulta `https://economia.awesomeapi.com.br/json/last/{pares}`, lê o valor `bid` e valida moeda e timestamp. Para pares estrangeiros, calcula a taxa a partir das cotações em BRL. A versão anterior do conversor usava Frankfurter, conforme os documentos históricos das etapas 5 e 6. [Documentação AwesomeAPI](https://docs.awesomeapi.com.br/api-de-moedas)

### Fotografias e créditos

`WikimediaImageService.java` tenta primeiro PageImages na Wikipédia em português e depois a pesquisa no Commons. O serviço resolve nomes equivalentes, usa contexto do destino, valida metadados e devolve autoria, licença e origem junto com a miniatura.

Há execução assíncrona, reaproveitamento de buscas em andamento e cache de resultados. `CommonsPhotoView.java` carrega as imagens com Glide, associa os créditos à foto correta durante a reciclagem dos cartões e mantém a imagem padrão quando a consulta falha. A atribuição deve acompanhar a licença da fotografia. [PageImages](https://www.mediawiki.org/wiki/Extension:PageImages) · [Wikimedia Commons](https://commons.wikimedia.org/)

### Firebase AI Logic e App Check

O projeto usa Firebase BoM 35.1.0, `firebase-ai`, plugin Google Services 4.5.0 e `firebase-appcheck-debug` em `debugImplementation`.

`DebugMalinhaApplication.java` inicializa o provedor de depuração antes das chamadas ao Gemini e solicita um token do App Check. Em um aparelho ou emulador de teste, o token aparece no Logcat sob a tag `DebugAppCheckProvider`. Registre o token daquele dispositivo no Firebase Console, em App Check, no app `com.example.malinha`. O token é privado e deve ser tratado fora do repositório.

Para distribuição, é necessário revisar a habilitação da funcionalidade no release e configurar um provedor de produção do App Check. A inicialização debug está em `app/src/debug/`. [Firebase AI Logic](https://firebase.google.com/docs/ai-logic) · [App Check](https://firebase.google.com/docs/ai-logic/app-check)

### Nager.Date e REST Countries

`NagerDateService.java` consulta `https://nagerholidays.com/api/v4/Holidays/{CountryCode}/{Year}`. `HolidayWeekends.java` calcula localmente os fins de semana prolongados. [Nager.Holidays](https://nagerholidays.com/api)

`RestCountriesService.java` consulta `https://api.restcountries.com/countries/v5/codes.alpha_2/{code}` e seleciona os campos necessários. O código verifica se a resposta corresponde ao país e rejeita amostras de demonstração que não representam a consulta. [REST Countries](https://restcountries.com/docs/countries)

### OpenFlights e comparadores externos

O catálogo `app/src/main/assets/airports.tsv` deriva de `airports.dat`, mantendo registros com código IATA válido. As colunas são IATA, nome do aeroporto, cidade, país, latitude, longitude e fuso. A origem e a licença estão em `openflights-license.txt` e nos créditos da tela Voos. [Fonte OpenFlights](https://github.com/jpatokal/openflights/blob/master/data/airports.dat) · [Licença ODbL](https://opendatacommons.org/licenses/odbl/1-0/)

Booking.com e os cinco comparadores de voos são acessados por URLs e navegador. `TravelSearchService.java` cuida dos acessos externos gerais. `FlightSearchLinks.java` centraliza os destinos, caminhos, datas e classes dos links de voos. Os testes verificam a construção das URLs, enquanto o preenchimento dos filtros precisa ser conferido no site escolhido.

### Travelpayouts e proxy legado

O material anterior descreve uma integração opcional de preços indicativos com Travelpayouts, Aviasales e um proxy Node.js. `FlightPriceService.java`, `FlightPricesActivity.java` e a configuração de proxy permanecem no projeto, mas a tela Voos atual usa os comparadores externos.

O proxy histórico usa `/aviasales/v3/prices_for_dates`, devolve tarifas indicativas em BRL e mantém o token Travelpayouts no servidor. Para reproduzir essa demonstração, use Node.js 22 ou superior, configure `TRAVELPAYOUTS_TOKEN` e `PORT` no arquivo privado `flight-proxy/.env`, execute `node --env-file=.env server.mjs` e configure `FLIGHTS_PROXY_URL`. No ambiente local, o acesso usa localhost e `adb reverse tcp:8787 tcp:8787`.

O endpoint não personaliza os preços por quantidade de passageiros e classe. Datas efetivas, contexto de cache e links de confirmação precisam acompanhar os resultados. A documentação histórica registra dados de buscas recentes e não comprova disponibilidade no momento da compra. A hospedagem de produção do proxy permanece uma configuração separada.

## Tecnologias e organização

O app usa Activities em Java, layouts XML, componentes Android Widget e Material, RecyclerView, ViewPager2, ArrayList, SQLite, SharedPreferences, JSON e HttpURLConnection. Glide é a biblioteca de carregamento de imagens. O Gradle usa Kotlin DSL para a configuração do build.

```text
app/src/main/
├── java/com/example/malinha/
│   ├── activities/     Telas, navegação e eventos
│   ├── adapters/       Listas e páginas dos roteiros
│   ├── models/         Viagem, Atividade, Destino e entidades
│   ├── database/       Persistência SQLite
│   ├── services/       Integrações e links externos
│   ├── utils/          Validação, calendários, imagens e catálogo
│   └── FirebaseAIService.java
├── assets/             Aeroportos e atribuição OpenFlights
└── res/                Layouts, cores, textos, temas e drawables
app/src/debug/          Inicialização debug e configurações de demonstração
app/src/test/           Testes unitários
app/src/androidTest/    Testes instrumentados
```

Os dados das viagens são salvos no SQLite. Favoritos e pesquisas recentes usam preferências locais. As reservas anotadas usam um arquivo privado separado e ainda não integram a entidade Viagem.

## Abrir e configurar

1. Abra a pasta do projeto no Android Studio.
2. Use o SDK solicitado pelo Gradle. A configuração atual usa Android mínimo 24, compileSdk e targetSdk 37.
3. Configure o JDK indicado em `gradle/gradle-daemon-jvm.properties`, atualmente versão 25. A compatibilidade do código-fonte Java é 11.
4. Configure as integrações que deseja testar usando os exemplos abaixo.
5. Sincronize o Gradle e gere a variante debug para demonstração local com IA.

| Arquivo local | Conteúdo esperado |
| --- | --- |
| `maps.properties` | `MAPS_API_KEY=SUA_CHAVE` |
| `places.properties` | `PLACES_API_KEY=SUA_CHAVE` |
| `app/google-services.json` | Configuração do app `com.example.malinha` no Firebase |
| `app/src/debug/res/values/country_api_local.xml` | Recurso `rest_countries_api_key` com o token de demonstração REST Countries |
| `flights.properties` | `FLIGHTS_PROXY_URL`, apenas para a integração legada |
| `flight-proxy/.env` | Token Travelpayouts e porta do proxy legado |

Os arquivos `*.properties.example`, `country_api_local.xml.example` e `flight-proxy/.env.example` são referências de preenchimento, quando presentes na cópia utilizada.

O Gradle também possui um fallback local para a chave Places no arquivo `Desktop/key.txt`. A configuração explícita em `places.properties` torna a montagem reproduzível em outro computador.

Para Maps e Places, restrinja a chave por aplicação Android, pacote `com.example.malinha`, SHA-1 do certificado de assinatura e APIs habilitadas. Um APK instalado no celular deve corresponder ao certificado autorizado. O relatório de assinatura pode ser obtido com `gradlew.bat signingReport`.

O recurso de token REST Countries no build debug entra no APK. Para distribuição com token privado, o serviço precisa usar um backend apropriado. Chaves, tokens App Check, arquivos de configuração pessoais e reservas não devem ser publicados na documentação.

## Compilar, testar e instalar

Execute os comandos na raiz da cópia do projeto que será compilada.

```powershell
.\gradlew.bat assembleDebug testDebugUnitTest --no-configuration-cache
.\gradlew.bat assembleDebugAndroidTest --no-configuration-cache
.\gradlew.bat connectedDebugAndroidTest --no-configuration-cache
```

O último comando exige emulador ou aparelho de teste conectado. Alguns testes de Maps, câmbio e Gemini dependem de rede, credenciais e argumentos de habilitação específicos. Use dados de teste ao executar testes instrumentados.

O APK debug fica em `app/build/outputs/apk/debug/app-debug.apk`. Transfira para o celular e autorize a instalação pela fonte usada quando solicitado. Atualizações assinadas com o mesmo certificado podem ser instaladas sobre a versão existente. A desinstalação remove dados locais do aplicativo.

Para testar Gemini no celular, instale a variante debug, ative a depuração USB, selecione o aparelho no Logcat e cadastre seu token App Check. O token do emulador pode ser diferente do token do celular.

## Verificações registradas

A consolidação deste README é documental. Nenhum build, teste ou chamada de IA foi executado para esta edição.

Na última implementação do pager de dias, em 09/11/2026, houve registro de compilação debug bem-sucedida, nove testes unitários aprovados e aprovação do teste instrumentado de deslize, seleção por abas e recriação da tela. A suíte instrumentada completa apresentou incompatibilidade entre o Espresso configurado e a API 37. O teste do pager foi adaptado para injeção de toque nativa e passou. O teste existente de navegação do Explorar continuou com essa limitação.

O relatório de 02/11/2026 registra seis testes unitários, onze instrumentados e Lint com zero erros e 59 avisos naquela versão. Esses números descrevem a entrega antiga. A geração de um novo APK deve ser acompanhada de verificação da versão correspondente.

## Requisitos acadêmicos

| Requisito | Onde aparece |
| --- | --- |
| Duas ou mais Activities | Splash, início, planejamento, editor e demais funcionalidades |
| Componentes android.widget | TextView, EditText, Button, CheckBox, Spinner, ImageView e Toast |
| Drawable | Mascote, ícones, cartões e fundos XML |
| Array e ArrayList | Viagens, atividades, catálogo e interesses |
| try/catch | Rede, JSON, banco, arquivos e abertura de links |
| if/else e switch | Validações, seleção de páginas, menus e serviços |
| Menu de opções | `BaseActivity.onCreateOptionsMenu()` e `onOptionsItemSelected()` |
| Toast | Validações, erros e confirmação de salvamento |
| Acesso à internet | Câmbio, Google, Wikimedia, países, feriados e IA |
| Persistência local | `TripDatabase.java` e armazenamento das reservas |

