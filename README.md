# Bipando Controle de Validade

## Descrição do Projeto

O **Bipando Controle de Validade** é uma aplicação Android desenvolvida para auxiliar no controlo de validade de produtos. Através da leitura de códigos de barras, o utilizador pode registar produtos, gerir as suas datas de validade e receber notificações sobre produtos que estão prestes a expirar ou que já expiraram. O objetivo principal é evitar o desperdício e otimizar a gestão de stock, seja para uso pessoal ou em pequenos negócios.

## Funcionalidades

*   **Registo de Produtos**: Adicione novos produtos à base de dados da aplicação, incluindo nome, código de barras, categoria, data de validade, anotações e imagem.
*   **Leitura de Código de Barras**: Utilize a câmara do dispositivo para ler códigos de barras e preencher automaticamente os dados do produto.
*   **Gestão de Validade**: Visualize os produtos organizados por proximidade da data de validade, com indicadores visuais (círculos coloridos) para alertar sobre produtos a expirar (amarelo) ou expirados (vermelho).
*   **Notificações**: Receba alertas sobre produtos que estão a aproximar-se da data de validade ou que já expiraram.
*   **Edição de Produtos**: Edite os detalhes de produtos existentes, incluindo a data de validade, nome, anotações e imagem.
*   **Categorização**: Organize os produtos por categorias personalizadas.
*   **Lixeira**: Produtos eliminados são movidos para uma "lixeira", permitindo a recuperação ou eliminação permanente.
*   **Login de Utilizador**: Sistema de autenticação para proteger os dados do utilizador.

## Tecnologias Utilizadas

O projeto é uma aplicação Android nativa e utiliza as seguintes tecnologias:

*   **Linguagem**: Java
*   **Interface de Utilizador (UI)**: XML para layouts, RecyclerView para listas dinâmicas.
*   **Base de Dados**: Room Persistence Library (SQLite) para armazenamento local de dados.
*   **Arquitetura**: MVVM (Model-View-ViewModel) com LiveData para observação de dados.
*   **Navegação**: Android Jetpack Navigation Component.
*   **Leitura de Código de Barras**: Biblioteca de scanner de código de barras (implementação interna ou externa).
*   **Carregamento de Imagens**: Picasso para carregamento e exibição eficiente de imagens.
*   **Notificações**: WorkManager para agendamento de tarefas em segundo plano e notificações.
*   **Autenticação**: Firebase Authentication (presumido, dado o `default_web_client_id` no `strings.xml`).

## Estrutura do Projeto

O projeto segue uma estrutura modular, com pacotes bem definidos para cada camada da aplicação:

*   `data`: Contém os modelos (entities), DAOs (Data Access Objects) e repositórios para interação com a base de dados Room.
*   `notification`: Lógica para agendamento e exibição de notificações.
*   `ui`: Contém os fragmentos, adaptadores, ViewModels e outras classes relacionadas à interface do utilizador e lógica de apresentação.
    *   `ui.home`: Fragmento principal que exibe a lista de produtos.
    *   `ui.add`: Fragmento para adicionar novos produtos.
    *   `ui.edit`: Fragmento para editar produtos existentes.
    *   `ui.scanner`: Lógica para a leitura de códigos de barras.
    *   `ui.category`: Gestão de categorias.
    *   `ui.deleteitem`: Gestão de itens na lixeira.
*   `ui.base`: Classes utilitárias e de base para a UI.
*   `ui.commun`: Classes utilitárias comuns, como `SharedPreferencesManager`.

## Como Contribuir

1.  Faça um *fork* do projeto.
2.  Crie uma *branch* para a sua funcionalidade (`git checkout -b feature/nova-funcionalidade`).
3.  Faça as suas alterações e *commit* (`git commit -m 'Adiciona nova funcionalidade'`).
4.  Faça *push* para a *branch* (`git push origin feature/nova-funcionalidade`).
5.  Abra um *Pull Request*.

## Licença

Este projeto está licenciado sob a licença MIT. Veja o arquivo `LICENSE` para mais detalhes.
