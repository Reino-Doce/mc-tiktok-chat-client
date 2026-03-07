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

## Build multi-versao

O build e parametrizado por propriedades Gradle:

- `minecraft_version`
- `forge_version`
- `mapping_channel`
- `mapping_version`
- `java_version`
- `mod_version`

### Exemplo explicito para 1.20.1

```bash
./gradlew build \
  -Pminecraft_version=1.20.1 \
  -Pforge_version=47.4.16 \
  -Pmapping_channel=official \
  -Pmapping_version=1.20.1 \
  -Pjava_version=17
```

### Adaptacao para outros alvos

```bash
./gradlew build \
  -Pminecraft_version=<mc> \
  -Pforge_version=<forge> \
  -Pmapping_channel=official \
  -Pmapping_version=<mc> \
  -Pjava_version=<java>
```

### Validacao de combinacao

O build falha com mensagem clara quando:

- propriedades obrigatorias estao ausentes;
- `official` mappings nao bate com `mapping_version == minecraft_version`;
- combinacao nao esta na matriz validada.

Para alvo nao validado:

```bash
./gradlew build -Pallow_unverified_target=true ...
```

## Compatibilidade entre versoes (estrategia)

- Codebase unica para regras/estado/comandos.
- Camada `platform` isola APIs volateis de Forge/Minecraft.
- Implementacao atual: `platform/mc1201`.
- Para nova versao, adiciona-se novo adaptador sem duplicar dominio.

Limite real: "qualquer versao" 100% automatica nao existe, porque API Forge/Minecraft e baseline de Java mudam entre linhas.

## Dependencia TikTokLiveJava

Para evitar instabilidade de JitPack no build, o projeto provisiona automaticamente:

- `Client-<version>-all.jar` da release oficial do repositrio TikTokLiveJava no GitHub.
- O artefato final do mod inclui essa dependencia embutida no `.jar` final.

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
- Sem mensagens no jogo:
  - valide se o chat HUD esta habilitado.
  - rode `/reinodoce status` para ver estado e ultimo erro.
- Username invalido:
  - use `@username` com letras, numeros, `.` ou `_`.
- Live offline:
  - ajuste reconnect com `/reinodoce settings reconnect <seconds>`.

## Nota importante

Este mod nao tenta se passar por jogador real do servidor. Todas as mensagens de LIVE aparecem com prefixo visual (`[LIVE]`) para manter identidade de origem externa.
