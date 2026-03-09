# reinodoce-mc-tiktok-shared

Linha compartilhada Java 17 do cliente TikTok para Minecraft. Esta branch nao gera um mod Forge; ela empacota apenas o nucleo reutilizavel entre as branches de versao.

## O que fica aqui

- Integracao com TikTok LIVE
- Regras, estado e controle de sessao
- Parser de rich messages e emoji
- Schema e persistencia de configuracao
- Modelos compartilhados (`RichLiveMessage`, `CommandResult`, `ChatEventSink`)
- Testes puros de JVM

## O que nao fica aqui

- Entry point Forge
- Brigadier/command registration do Minecraft
- `Component`, `ResourceLocation`, `Minecraft`, `MinecraftForge`
- Inline media renderer, cache, coremod e assets do mod
- `mods.toml`, `pack.mcmeta` e demais recursos de empacotamento Forge

## Build

```powershell
.\gradlew.bat test
```

O build verifica automaticamente que a branch compartilhada nao contem imports de `net.minecraft` nem `net.minecraftforge`.

## Estrategia de branches

Git nao permite coexistir `shared` e `shared/java17` como branches, porque `shared` bloquearia o namespace `shared/*`. A convencao implementada localmente e:

- `shared/latest`: linha compartilhada mais nova
- `shared/java17`: linha compartilhada Java 17
- `1.20.1`: adapter Forge 1.20.1 em cima de `shared/java17`

O fluxo continua descendente: mudancas comuns entram primeiro na branch `shared/*` pertinente e depois sao mescladas nas branches de versao.
