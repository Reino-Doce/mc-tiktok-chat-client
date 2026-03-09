# reinodoce-mc-tiktok

Mod **client-side only** para Minecraft Java (Forge) que espelha eventos do chat de uma LIVE do TikTok dentro do chat do Minecraft.

## Visao geral

- Projeto: Forge mod cliente, sem dependencia de servidor.
- Funciona em singleplayer e multiplayer.
- Nao exige plugin/mod/datapack no servidor.
- Prefixo padrao no chat: `[LIVE]`.
- Comandos client-side:
  - `/reinodoce connect @username`
  - `/reinodoce disconnect`
  - `/reinodoce status`
  - `/reinodoce settings reconnect <seconds>`
  - `/reinodoce rule follower <true|false>`
  - `/reinodoce rule min-member-level <level>`
  - `/reinodoce syntetic gift <value>`
  - `/reinodoce syntetic gift-combo <ignore|single|bulk>`
  - `/reinodoce syntetic follow <true|false>`
  - `/reinodoce syntetic join <true|false>`
  - `/reinodoce syntetic member-level <true|false>`
  - `/reinodoce reload`

## Stack

- Minecraft alvo principal: `1.20.1`
- Forge alvo principal: `47.4.16`
- ForgeGradle: `net.minecraftforge.gradle` `[6.0,6.2)`
- Gradle wrapper: `8.8`
- Java alvo para 1.20.1: `17`
- TikTokLiveJava: `1.11.11-Release` (jar provisionado de release GitHub)

## Instalacao (uso do mod)

1. Gere o jar do mod com `./gradlew build` (ou `gradlew.bat build` no Windows).
2. Copie o jar gerado em `build/libs` para a pasta `mods` do cliente Forge.
3. Inicie Minecraft com perfil Forge correspondente.
4. No jogo, use `/reinodoce connect @username`.

## Exemplo rapido

1. `/reinodoce settings reconnect 5`
2. `/reinodoce rule follower true`
3. `/reinodoce rule min-member-level 1`
4. `/reinodoce syntetic gift-combo "bulk"`
5. `/reinodoce connect @seuusuario`
6. `/reinodoce status`

## Prism Launcher (Side client vs both)

Para mods locais `.jar`, o Prism Launcher normalmente mostra `Side = both` quando nao existe metadado Packwiz no `.index`.

Este projeto gera um bundle especifico para Prism com:

- `mods/reinodoce-mc-tiktok-<versao>.jar`
- `mods/.index/reinodoce-mc-tiktok.pw.toml` com `side = "client"`

Comando para gerar o bundle Prism:

```bash
./gradlew clean build prismBundle verifyPrismMetadata
```

No Windows:

```powershell
.\gradlew.bat clean build prismBundle verifyPrismMetadata
```

Saida:

- `build/prism-bundle/mods`

Uso no Prism:

1. Feche a instancia no Prism.
2. Copie todo o conteudo de `build/prism-bundle/mods` para a pasta `mods` da instancia.
3. Abra a instancia no Prism e confira a coluna Side do mod (`client`).

Observacao: usar apenas o `.jar` sem o `.pw.toml` continua funcionando no Forge, mas o Prism pode exibir `both` na UI.

## Persistencia de configuracao

Configuracao local do cliente em:

- `config/reinodoce-mc-tiktok-client.json`

Campos persistidos:

- `lastUsername`
- `reconnectSeconds`
- `ruleFollowerOnly`
- `ruleMinMemberLevel`
- `synteticGiftMinValue`
- `synteticGiftComboMode`
- `synteticFollowEnabled`
- `synteticJoinEnabled`
- `synteticMemberLevelEnabled`
- `chatPrefix`

## Build da branch

Esta branch e fixa em:

- Minecraft `1.20.1`
- Forge `47.4.16`
- Java `17`

Build local:

```powershell
.\gradlew.bat build
```

O artefato desta branch existe para o adapter Forge 1.20.1. O nucleo compartilhado vive nas branches `shared/latest` e `shared/java17`.

## Estrategia entre branches

- `shared/latest`: linha compartilhada mais nova
- `shared/java17`: linha compartilhada Java 17
- `1.20.1`: adapter Forge 1.20.1 em cima de `shared/java17`

Fluxo de manutencao:

- mudancas comuns entram primeiro na branch `shared/*` pertinente;
- depois sao mescladas para a branch de versao;
- nao ha merge lateral entre branches de versao.

Observacao: Git nao permite coexistir `shared` e `shared/java17` como branches. Por isso a linha compartilhada mais nova foi implementada como `shared/latest`.

## Dependencia TikTokLiveJava

Para evitar instabilidade de JitPack no build, o projeto provisiona automaticamente:

- `Client-<version>-all.jar` da release oficial do repositrio TikTokLiveJava no GitHub.
- O artefato final do mod inclui essa dependencia embutida no `.jar` final.
- Para compatibilidade com Forge + Connector/Fabric API, libs fornecidas pelo host (`com.google.gson` e `org.slf4j`) sao excluidas do empacotamento final do mod.

Propriedade opcional para usar caminho customizado:

- `-Ptiktoklive_jar_path=<caminho-do-jar>`

## Threading e robustez

- Integracao TikTok roda fora da game thread.
- Chat do Minecraft e sempre enviado no contexto seguro do cliente.
- Reconnect com scheduler dedicado.
- Telemetria local de reconnect (contador de tentativas no `/reinodoce status`).
- Token de ciclo de vida evita callback antigo afetar sessao atual.
- Protecao para connect/disconnect repetidos e troca de username ativa.
- Avisos de erro/reconnect no chat com throttling para reduzir flood.

## Limitacoes

- Integracao TikTok usa API nao oficial.
- Mudancas no TikTok podem quebrar eventos/conexao sem aviso.
- Member level pode variar por regiao/payload da live.

## Troubleshooting

- Erro de Java no Gradle:
  - use JDK 17 para build 1.20.1 (`JAVA_HOME` apontando para Java 17).
- Falha ao testar nova versao no launcher:
  - remova o jar antigo do mod na pasta `mods` antes de copiar o novo artefato.
- Sem mensagens no jogo:
  - valide se o chat HUD esta habilitado.
  - rode `/reinodoce status` para ver estado e ultimo erro.
- Username invalido:
  - use `@username` com letras, numeros, `.` ou `_`.
- Live offline:
  - ajuste reconnect com `/reinodoce settings reconnect <seconds>`.

## Nota importante

Este mod nao tenta se passar por jogador real do servidor. Todas as mensagens de LIVE aparecem com prefixo visual (`[LIVE]`) para manter identidade de origem externa.
