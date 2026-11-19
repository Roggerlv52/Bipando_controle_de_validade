# Bipando - Controle de Validade

## Descrição do Projeto

O **Bipando - Controle de Validade** é uma aplicação Android nativa moderna, desenvolvida para auxiliar no controlo eficiente de validade de produtos. Através da leitura de códigos de barras pela câmera do dispositivo ou busca por voz, o utilizador pode registar produtos, acompanhar automaticamente as datas de vencimento e receber alertas preventivos sobre produtos prestes a expirar ou já vencidos.

O objetivo principal é eliminar o desperdício de alimentos e produtos e otimizar a gestão de estoque, atendendo desde o uso pessoal e doméstico até pequenos negócios e equipes com estoque compartilhado.

---

## Funcionalidades Principais

* 📦 **Registo e Gestão de Produtos**: Adicione, edite e acompanhe produtos com informações completas: nome, código de barras, categoria, data de validade, quantidade, localização/estoque, anotações e foto.
* 🔍 **Consulta Automática na Open Food Facts**: Ao ler o código de barras de um produto, o app consulta automaticamente a API global da Open Food Facts para preencher o nome e imagem do produto.
* 📷 **Leitor de Código de Barras de Alta Precisão**: Integração com a câmera via CameraX e Google ML Kit para leitura instantânea de códigos de barras.
* 🎙️ **Pesquisa por Voz e Busca Inteligente**: Encontre produtos rapidamente digitando ou utilizando pesquisa por voz, além de filtros por categoria e status de validade.
* ⏳ **Gestão Visual de Validade**: Produtos organizados por proximidade do vencimento, com sinalização visual clara (indicadores amarelos para produtos a expirar e vermelhos para expirados).
* 🔔 **Notificações e Alertas Preventivos**: Agendamento inteligente de notificações via WorkManager para alertar sobre o vencimento de produtos antes que expirem.
* 👥 **Modo de Trabalho e Estoque Compartilhado (Grupos)**:
  * **Modo Individual**: Gestão pessoal e privada de estoque.
  * **Modo Grupo**: Crie grupos/equipes, convide membros por código ou e-mail e faça a gestão colaborativa do estoque em tempo real.
* 📂 **Categorização Personalizada**: Organize os seus produtos por categorias com ícones e seleções personalizadas.
* 🗑️ **Lixeira e Recuperação**: Produtos excluídos são movidos para a lixeira, permitindo restauração rápida ou exclusão permanente.
* ⭐ **Plano Premium (Google Play Billing)**:
  * Modalidades de assinatura: **Mensal**, **Semestral** e **Anual**, com período de teste gratuito para novos utilizadores.
  * Benefícios Premium: cadastro e scanner ilimitados (plano gratuito limitado a 100 produtos), categorias ilimitadas e notificações customizadas por produto.
* 👤 **Autenticação e Perfil de Utilizador**:
  * Autenticação segura via Firebase Auth (incluindo **Login com Google**).
  * Gestão de perfil com alteração de foto de avatar, atualização de dados, redefinição de senha e exclusão de conta.
* ⚙️ **Configurações e Personalização**:
  * Alternância entre modos de trabalho (Individual x Grupo).
  * Preferências de interface (seletores de data, efeitos sonoros/beep ao escanear).
  * Suporte a tema escuro (Dark Mode).
* 🚀 **Onboarding / Tour Interativo**: Apresentação guiada para introduzir as principais funcionalidades a novos utilizadores.

---

## Tecnologias Utilizadas

O projeto é construído seguindo as melhores práticas do ecossistema Android moderno:

* **Linguagem**: Kotlin (100% nativo)
* **Interface de Utilizador (UI)**: Jetpack Compose com Material 3 e Navigation Compose
* **Arquitetura**: MVVM (Model-View-ViewModel) com Clean Architecture (camadas `data`, `domain` e `ui`), Kotlin Coroutines e StateFlow
* **Base de Dados Local**: Room Persistence Library (SQLite) para suporte e cache offline
* **Backend e Nuvem**:
  * **Firebase Authentication**: Login por e-mail/senha e Google Sign-In
  * **Firebase Firestore**: Sincronização de dados em tempo real
  * **Firebase Storage**: Armazenamento seguro de imagens de produtos e avatares
  * **Firebase Analytics & App Check**: Telemetria e proteção de API
* **Integrações Externas**:
  * **Open Food Facts API**: Catálogo global para preenchimento automático de produtos
  * **Google Play Billing Library**: Gestão de assinaturas e compras in-app
* **Câmera e ML**: CameraX + Google ML Kit Barcode Scanning
* **Tarefas em Segundo Plano**: Android Jetpack WorkManager para agendamento de notificações e sincronização de imagens
* **Carregamento de Imagens**: Picasso / Coil

---

## Estrutura do Projeto

O código-fonte está organizado modularmente por camadas e funcionalidades:

```text
com.rogger.bp
├── data                # Entidades Room, DAOs, DTOs, Repositórios e Data Sources (Firebase, Room, Open Food Facts)
├── domain              # Casos de Uso (Use Cases), modelos de domínio e interfaces de repositório
├── ui                  # Camada de apresentação desenvolvida em Jetpack Compose
│   ├── add             # Cadastro de produtos e integração com câmera/Open Food Facts
│   ├── category        # Gestão de categorias personalizadas
│   ├── deleteitem      # Lixeira de produtos excluídos
│   ├── edit            # Edição de produtos existentes
│   ├── groups          # Gestão de grupos, convites, membros e estoque compartilhado
│   ├── home            # Lista principal de produtos agrupados por validade, buscas e filtros
│   ├── login           # Autenticação e Login com Google
│   ├── naviation       # Grafo de navegação (BipandoNavGraph) e rotas
│   ├── payment         # Paywall, planos e integração com Google Play Billing
│   ├── profile         # Perfil do utilizador, foto, configurações e tema
│   ├── scanner         # Leitor de código de barras em tempo real (CameraX + ML Kit)
│   ├── splash          # Tela de abertura e verificação de sessão/sincronização
│   └── tour            # Onboarding e tour interativo para novos utilizadores
└── util                # Utilitários para áudio/beep, pesquisa por voz, compartilhamento e permissões
```

---

## Como Contribuir

1. Faça um *fork* do projeto.
2. Crie uma *branch* para a sua funcionalidade (`git checkout -b feature/nova-funcionalidade`).
3. Faça as suas alterações e *commit* (`git commit -m 'Adiciona nova funcionalidade'`).
4. Faça *push* para a sua *branch* (`git push origin feature/nova-funcionalidade`).
5. Abra um *Pull Request*.

---

## Licença

Este projeto está licenciado sob a licença MIT. Veja o ficheiro `LICENSE` para mais detalhes.
