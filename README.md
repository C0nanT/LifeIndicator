# LifeIndicator

Mod para Hytale que mostra a vida dos animais e mobs quando você olha para eles.

## Funcionalidades

- Exibe uma barra de vida acima da cabeça da entidade quando o jogador olha para ela
- Atualiza automaticamente conforme a vida muda
- Remove o indicador quando o jogador olha para outro lugar
- Alcance de detecção: 30 blocos

## Como funciona

```
[|||||||||||||||]  100% vida
[||||||||||||   ]  80% vida
[|||||||        ]  50% vida
[|||            ]  20% vida
```

## Instalação

1. Compile o projeto:
   ```bash
   ./gradlew build
   ```

2. Copie o arquivo `build/libs/LifeIndicator-1.0.0.jar` para a pasta `mods` do servidor Hytale

3. Reinicie o servidor

## Requisitos

- Java 21
- Hytale Server

## Estrutura do Projeto

```
src/main/java/com/example/plugin/
├── LifeIndicator.java      # Classe principal do plugin
└── LifeIndicatorTask.java  # Tarefa que detecta entidades e mostra vida
```

## Configuração

| Parâmetro | Valor | Descrição |
|-----------|-------|-----------|
| MAX_DISTANCE | 30.0 | Distância máxima de detecção (blocos) |
| MIN_DOT_PRODUCT | 0.9 | Precisão do olhar (0.9 = ~25 graus) |
| Intervalo | 200ms | Frequência de atualização |

## API Utilizada

- `Nameplate` - Componente para mostrar texto acima de entidades
- `EntityStatMap` - Acesso aos stats das entidades (vida, etc)
- `HeadRotation` - Direção do olhar do jogador
- `TransformComponent` - Posição das entidades

## Licença

MIT
