

📚 EmaBooks — Aplicativo Mobile da Biblioteca da Unifor

Disciplina: Desenvolvimento Mobile — 2025.2
Professor: Narrar
Curso: Universidade de Fortaleza (Unifor)

⸻

📝 Descrição do Projeto

O EmaBooks é o aplicativo oficial desenvolvido como trabalho final da disciplina Desenvolvimento Mobile.
Seu propósito é oferecer aos alunos, professores e colaboradores da Unifor uma experiência prática, simples e acessível para:
	•	Consulta ao acervo físico e digital
	•	Gerenciamento de empréstimos
	•	Visualização de histórico
	•	Reserva de livros
	•	Favoritos
	•	Acesso a informações da conta
	•	Configurações de acessibilidade e preferências de uso

O app foi desenvolvido em Android Studio (Kotlin) e utiliza Firebase Firestore como backend. Todas as telas e requisitos seguiram boas práticas de UI/UX, acessibilidade, usabilidade e arquitetura limpa.

⸻

🎯 Objetivos do Projeto
	•	Criar um aplicativo funcional com a lógica completa de navegação, listagem, autenticação e manipulação de dados.
	•	Trabalhar conceitos de:
	•	Consumo de banco de dados no Firebase
	•	Autenticação e persistência (Firestore)
	•	Padrões de layout XML responsivos
	•	Boas práticas em Kotlin
	•	Arquitetura de telas mobile
	•	UX acessível (contraste, fonte, hierarquia visual)

⸻

🧩 Funcionalidades Implementadas

🔐 1. Autenticação
	•	Login com validação de email e senha.
	•	Cadastro de usuário com:
	•	Nome completo
	•	Email
	•	Matrícula
	•	Senha e confirmação
	•	Recuperação de senha com envio automático via Firebase.
	•	Tratamento de erros e mensagens claras ao usuário.

⸻

📚 2. Consulta de Acervo
	•	Campo de busca único com validações:
	•	Trim automático
	•	Mínimo de 2 caracteres
	•	Limite de 100 caracteres
	•	Case-insensitive
	•	Accent-insensitive
	•	Listagem com card contendo:
	•	Capa
	•	Título (destaque)
	•	Autor
	•	Ano
	•	Status: Disponível / Indisponível
	•	Estado de carregamento enquanto busca
	•	Mensagem quando não há resultados
	•	Abertura da tela de detalhes do livro

⸻

⭐ 3. Sistema de Favoritos
	•	Usuário pode favoritar/unfavoritar livros tocando na estrela
	•	A lógica salva uma relação userId + livroId no Firestore
	•	O botão muda cor de fundo ao favoritar
	•	A aba Minha Conta exibe todos os livros favoritados
	•	Renderização dinâmica mesmo após fechar o app

⸻

📖 4. Detalhes do Livro
	•	Exibe todas as informações do livro
	•	Botão de favoritos integrado
	•	Exibição do status de disponibilidade
	•	Se estiver disponível → botão Reservar
	•	Se indisponível → mensagem informativa

⸻

📅 5. Reservar Livro (Admin + Usuário)
	•	Usuário comum pode:
	•	Ver disponibilidade
	•	Solicitar reserva
	•	Ver histórico de reservas
	•	Administrador pode:
	•	Realizar reservas
	•	Cancelar
	•	Registrar retirada e devolução
	•	Ver fila de espera

⸻

👤 6. Minha Conta

Exibe:
	•	Nome
	•	Email
	•	Matrícula
	•	Membro desde
	•	Quantidade de empréstimos
	•	Quantidade de favoritos
	•	Lista renderizada dos livros favoritados

Sessão Configurações com:
	•	Tamanho da fonte
	•	Modo de contraste
	•	Notificações
	•	Logout

Todos os dados são buscados dinamicamente do Firestore.

⸻

⚙️ 7. Configurações / Preferências do Usuário

Gravadas no Firestore por usuário:
	•	Tamanho da fonte selecionado
	•	Tema de contraste
	•	Preferência de notificações
	•	Todas refletem imediatamente na UI
	•	Persistência entre sessões

⸻

🚦 8. Barra de Navegação Inferior (Bottom Navigation)

Inclui:
	•	Home
	•	Empréstimos
	•	Conta
	•	Sobre

Sempre visível após login.

⸻

🛠️ 9. Painel Administrativo (Somente Admins)
	•	Cadastro de livro
	•	Edição de livro
	•	Atualização de capa
	•	Gestão de reservas
	•	Visualização de empréstimos
	•	Indicadores básicos (quantidade de livros, emprestados, etc.)

⸻

🏛️ Arquitetura do Projeto

O app segue boas práticas de organização:

/app
  /java/com.example.emabooks
      /auth              → Login, cadastro, reset senha
      /home              → Tela inicial e busca
      /models            → Data classes (Livro, Usuario, Emprestimo)
      /services          → Integrações com Firestore
      /admin             → Telas de administração
      /favoritos         → Lógica + telas
      /emprestimos       → Reservas e histórico
      /utils             → Funções auxiliares
      BaseActivity.kt    → BottomNavigation + helpers
  /res
      /layout            → Telas em XML
      /drawable          → Ícones
      /values            → Cores, strings, estilos


⸻

🔥 Tecnologias Utilizadas

Tecnologia	Uso
Android Studio (Kotlin)	Desenvolvimento mobile
Firebase Firestore	Banco de dados
Material Design 3	Componentes modernos
RecyclerView	Listagens
Glide	Exibição de imagens
XML	Construção da interface


⸻

🏗️ Como Rodar o Projeto Localmente

1. Clone o repositório

git clone https://github.com/seuusuario/emabooks.git

2. Abra no Android Studio

3. Configure o Firebase:
	•	Crie um projeto no Firebase
	•	Adicione o app Android
	•	Baixe o arquivo google-services.json
	•	Cole em:

/app/google-services.json

4. Instale dependências automaticamente

5. Execute no Emulador ou Dispositivo Físico

⸻

📌 Status Atual

✔️ Funcionalidades principais implementadas
✔️ Firestore totalmente integrado
✔️ Lógica de favoritos funcionando
✔️ Reservas conectadas
✔️ Admin funcional
✔️ Teste final de usabilidade em andamento

⸻

👨‍🏫 Professor

Narark — Unifor (2025.2)

⸻

👨‍💻 Desenvolvido por

Lucas Braide - 2526367
Handerson 
Maria Alice
Aluno da disciplina Desenvolvimento Mobile (2025.2)
Universidade de Fortaleza — Unifor
