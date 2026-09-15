# Frontend

React 19 + TypeScript + Vite. Este documento existe porque não havia nada escrito sobre o
frontend — o `frontend/README.md` era o template do Vite.

Última verificação contra o código: branch `docs`.

---

## 1. Antes de tudo: os dados são mockados

**Não existe camada HTTP.** Nenhum `fetch`, nenhum `axios` (não está instalado), nenhum
`import.meta.env.VITE_*`, nenhum arquivo `api.ts`/`client.ts`. Todas as telas leem de
`src/mocks/` e mutam estado local com `useState`.

O login não valida nada:

```tsx
// src/App.tsx
function handleLogin() {
  localStorage.setItem("token", "mock-token-123");
  setIsAuthenticated(true);
}
```

A string `"mock-token-123"` é usada apenas como flag booleana de "autenticado". O
`LoginForm` é estático — três elementos sem `value`, sem `onChange`, sem `onSubmit`; as
credenciais digitadas são ignoradas.

O que isso significa na prática:

- o frontend **roda sem o backend** (`npm run dev` e pronto);
- as telas demonstram fluxo e layout, não comportamento real;
- nada é persistido: recarregar a página zera tudo;
- as regras de negócio do frontend são **reimplementações** das do backend
  (`src/services/ordemServicoCalculos.ts`, `pagamentoCalculos.ts`) e podem divergir.

Há 11 comentários `// TODO` marcando onde a API entraria, com endpoints já nomeados —
em `App.tsx`, `Header`, `pages/Usuarios`, `pages/Oficinas`, `pages/Unidades`,
`pages/Pagamentos`.

Plano de integração na §9.

---

## 2. Estrutura

Organização **por tipo**. Dentro de `components/` e `pages/`, o padrão é pasta por
componente com `index.tsx` + `<nome>.style.css` co-localizados, e um `*Fields.ts` quando
há formulário.

```
frontend/src
├── App.tsx            roteamento + todo o estado global (265 linhas)
├── main.tsx           entry: StrictMode + createRoot
├── index.css          reset + .page, .form/.form-content, .modal/.modal-content
├── App.css            ⚠️ vazio e não importado
├── assets/
├── mocks/             12 arquivos MOCK_* — a fonte de dados atual
├── types/             interfaces por domínio (subpasta por agregado)
├── services/          formatters.ts · ordemServicoCalculos.ts · pagamentoCalculos.ts
│                      (funções puras; nenhuma faz HTTP)
├── pages/             11 páginas
└── components/        componentes reutilizáveis e compostos
```

Não existem: `src/api/`, `src/hooks/`, `src/contexts/`, `src/store/`, `src/utils/`.
Nenhum arquivo de teste.

Imports são todos relativos (`../../`) — não há path alias configurado.

---

## 3. Autenticação e papéis

### Papéis

`src/types/usuario/role.ts`:

```ts
export type Role = "ADMIN" | "GERENTE" | "MECANICO";

export const ROLE_LABELS: Record<Role, string> = {
  ADMIN: "Administrador",
  GERENTE: "Gerente",
  MECANICO: "Mecânico",
};
```

**Alinhado ao backend.** Essa divergência já existiu — o frontend usava `GERENTE` enquanto
o backend usava `ADMINISTRATIVO`, e integrar naquele estado causaria loop de login. Hoje
os três valores são idênticos aos do enum Java. Não há nenhuma ocorrência de
`ADMINISTRATIVO` no código.

### Estado de autenticação

Não há Context nem gerenciador de estado. Tudo vive em `useState` dentro de `App.tsx` e
desce por props:

```
App (isAuthenticated, usuarioLogado, pagamentos, registros)
 └─ MainLayout
     ├─ Header  (menu do avatar, EditUsuarioModal)
     ├─ Sidebar (menu condicionado por role)
     └─ Routes → páginas
```

O usuário logado é `MOCK_USUARIO_LOGADO` (`role: "GERENTE"`, `oficinaId: 1`). Não há
decodificação de JWT em nenhum lugar.

Quando `isAuthenticated === false`, `App` retorna `<Login/>` **antes** do
`<BrowserRouter>` — a tela de login fica fora do router.

---

## 4. Rotas

`react-router-dom` 7, tudo declarado inline em `App.tsx`.

| Rota | Página | Papéis |
|---|---|---|
| `/dashboard` | `Dashboard` | MECANICO, GERENTE |
| `/clientes` | `Clientes` | MECANICO, GERENTE |
| `/veiculos` | `Veiculos` | MECANICO, GERENTE |
| `/ordens-servico` | `OrdensServico` | MECANICO, GERENTE |
| `/mecanicos` | `Mecanicos` | MECANICO, GERENTE |
| `/pecas` | `Pecas` | MECANICO, GERENTE |
| `/pagamentos` | `Pagamentos` | MECANICO, GERENTE |
| `/usuarios` | `Usuarios` | GERENTE |
| `/unidades` | `Unidades` | GERENTE |
| `/admin/oficinas` | `Oficinas` | ADMIN |
| `/admin/usuarios` | `Usuarios` | ADMIN |
| `/`, `/login`, `*` | redireciona para a home do papel | — |

### Guard

```tsx
function RequireRole({ allowed, usuarioLogado }: { allowed: Role[]; usuarioLogado: Usuario }) {
  if (!allowed.includes(usuarioLogado.role)) {
    return <Navigate to={homeRouteFor(usuarioLogado.role)} replace />;
  }
  return <Outlet />;
}

function homeRouteFor(role: Role): string {
  return role === "ADMIN" ? "/admin/oficinas" : "/dashboard";
}
```

Layout route via `<Outlet/>`, usado em 3 grupos. Papel sem acesso é **redirecionado para a
home**, não vê tela de 403.

### Divergências com a matriz do backend

O agrupamento do frontend é mais grosso que o `@PreAuthorize`:

| Caso | Frontend | Backend |
|---|---|---|
| `/pagamentos` | MECANICO tem acesso à tela | quase todo `/api/pagamentos/**` é só GERENTE |
| `/mecanicos` | MECANICO acessa o cadastro | `/api/mecanicos/**` não libera MECANICO |
| `/clientes` | MECANICO acessa | `/api/clientes/**` não libera MECANICO |

Ao ligar a API, essas três telas vão dar `403` para MECANICO. Precisam ser restritas a
`GERENTE_ROLES` — ou a permissão do backend precisa mudar, decisão de produto. Matriz
autoritativa em [permissions.md](./permissions.md).

---

## 5. Os três componentes genéricos

Quase toda tela é montada com eles. Entender os três é entender o frontend.

### `EntityForm<T>`

Modal de formulário orientado a schema.

```ts
interface EntityFormProps<T> {
  title: string;
  fields: FormField<T>[];
  initialValues?: Partial<T>;
  onSubmit: (data: T) => void;
  onClose: () => void;
}
```

**Schema declarado em arquivo separado** com o helper `defineFields<T>` (identity function,
só para inferência):

```ts
// pages/Clientes/clientFields.ts
export const clientFields = defineFields<ClienteFormData>([
  { name: "nome", label: "Nome", placeholder: "Nome completo", type: "text", required: true },
  { name: "documento", label: "CPF/CNPJ", placeholder: "000.000.000-00", type: "document" },
]);
```

Quando as opções são dinâmicas, usa-se factory: `createUsuarioFields(oficinaOptions,
roleOptions)`, `createRegistroPagamentoFields(saldoRestante)`.

**Contrato do campo:**

```ts
interface BaseField<T> {
  name: keyof T & string;
  label: string;
  placeholder: string;              // obrigatório
  required?: boolean;
  readOnly?: boolean;
  hidden?: (formData: Partial<T>) => boolean;
  validate?: (rawValue: unknown, formData: Partial<T>) => string | undefined;
}
```

11 tipos: `text`, `number`, `email`, `phone`, `document`, `date`, `currency`, `plate`,
`select`, `textarea`, `password`. (`date` e `email` estão declarados mas nenhum schema os
usa; `hidden` também está implementado e não usado.)

#### rawValues × displayValues — o ponto central

O componente mantém **dois estados paralelos**:

```tsx
const [rawValues, setRawValues] = useState<Partial<T>>(initialValues ?? {});
const [displayValues, setDisplayValues] = useState(() => buildInitialDisplay(fields, initialValues));
```

| Estado | Conteúdo | Destino |
|---|---|---|
| `rawValues` | valor canônico: dígitos puros, centavos como inteiro, placa normalizada | vai em `onSubmit` → backend |
| `displayValues` | string mascarada | vai no `value` do input |

Máscaras em `src/services/formatters.ts`:

| type | raw | display |
|---|---|---|
| `phone` | `unformatPhone` (≤11 dígitos) | `(83) 98888-1111` |
| `document` | `unformatDocument` (≤14 dígitos) | CPF ou CNPJ conforme o tamanho |
| `currency` | `parseCurrencyToCents` → **inteiro em centavos** | `R$ 1.234,56` |
| `plate` | `normalizePlate` (7 alfanuméricos maiúsculos) | `ABC-1D23` |
| `number` | `Number(val)` | texto digitado |
| `select` | `Number(val)` se numérico | texto |
| demais | identidade | identidade |

> **`currency` guarda centavos.** O backend espera decimal (`1234.56`), o form produz
> `123456`. A divisão por 100 na borda HTTP é obrigatória. Ver
> [business-rules.md §1](./business-rules.md).

#### Validação

Síncrona, **só no submit** (`validators.ts`): campo `required` vazio gera
`"{label} é obrigatório"`; depois roda o `validate` custom. Se houver qualquer erro,
`onSubmit` não é chamado.

Limitação conhecida: **os erros não são limpos ao digitar** — `handleFieldChange` não
reseta `errors`. A mensagem fica na tela até o próximo submit.

### `EntityTable<T>`

```ts
interface EntityTableProps<T> {
  data: T[];
  columns: Column<T>[];
  actions?: EntityAction<T>[];
  getRowKey: (item: T) => string | number;
  searchTerm?: string;
  searchFields?: (keyof T)[];
  searchFn?: (item: T, term: string) => boolean;
  loading?: boolean;
  emptyMessage?: string;
}
```

`Column<T>` aceita `format(value, item)` ou `render(item)`; `render` tem precedência.
`key` aceita chave derivada (`"codigo"`), não só `keyof T`.

`EntityAction<T>` tem `label`, `icon` (lucide), `variant`
(`view|edit|delete|print|status|upOs|default`), `onClick` e `hidden(item)`.

Filtro é **client-side**: `searchFn` tem precedência sobre `searchFields`; sem nenhum dos
dois, não filtra. Ao ligar a API com paginação server-side, isso precisa mudar.

### `EntityViewModal`

Modal de leitura. **Não é genérico:**

```ts
interface EntityViewModalProps {
  title: string;
  subtitle?: string;
  fields: ViewField[];      // { icon, label, value }
  onClose: () => void;
}
```

Usado em apenas 3 páginas (Usuários, Unidades, Oficinas). Detalhe: `ViewField.value` é
tipado `ReactNode`, mas o componente faz `String(field.value)` — nós React passados ali
não renderizam.

---

## 6. Páginas

Todas seguem o mesmo esqueleto: `HeaderPageWithButton` + `StatCard` + `SearchBar` +
`EntityTable` + modais.

| Página | Rota | O que faz |
|---|---|---|
| `Login` | — | logo + form estático |
| `Dashboard` | `/dashboard` | 4 `StatCard` + `OrdersChart` (recharts) + `RecentOrders`. Dados inline hardcoded |
| `Clientes` | `/clientes` | CRUD local. Ação "ver OS do cliente" é `console.log` |
| `Veiculos` | `/veiculos` | CRUD local. Ação "ver OS" é `console.log` |
| `Mecanicos` | `/mecanicos` | CRUD local. `oficinaId: 1` hardcoded na criação |
| `Pecas` | `/pecas` | CRUD local. Ação "relacionar a OS" é `console.log` |
| `OrdensServico` | `/ordens-servico` | a maior tela: OS + peças + mão de obra + desconto + pagamento, com `ViewOrdemServicoModal` |
| `Pagamentos` | `/pagamentos` | 3 `StatCard`, histórico, registro de pagamento. Impressão é TODO |
| `Usuarios` | `/usuarios` e `/admin/usuarios` | **mesmo componente nas duas rotas**, comportamento por `isAdmin` |
| `Unidades` | `/unidades` | CRUD completo, filtrado pela `oficinaId` da prop |
| `Oficinas` | `/admin/oficinas` | CRUD completo — home do ADMIN |

### Sobre as telas que foram pedidas

| Pedido | Situação |
|---|---|
| Gestão de unidades | ✅ `pages/Unidades` |
| Gestão de usuários da oficina | ✅ `pages/Usuarios` com `isAdmin=false`: vê só a própria oficina e só cria GERENTE/MECANICO |
| Dashboard do admin | ✅ `pages/Oficinas` (home do ADMIN) + `/admin/usuarios` |
| Perfil do usuário | ⚠️ **não é página** — é o `EditUsuarioModal`, aberto pelo menu do avatar no `Header` |

Duas lacunas no perfil: o campo `novaSenha` existe no form e é **ignorado** pelo handler
(`App.tsx`), e a página `Usuarios` não tem ação de editar — só visualizar e remover.

O ADMIN, coerentemente com o princípio de separação, só tem acesso a `/admin/oficinas` e
`/admin/usuarios`. Nenhuma tela operacional.

---

## 7. Estilo

CSS **global puro**. Sem CSS Modules, sem Tailwind, sem CSS-in-JS. 56 arquivos `.css`:
`index.css` global + 54 co-localizados com nome `<camelCase>.style.css`, importados por
side-effect.

Cores são hex hardcoded por arquivo — **não há custom properties**. Mudar a paleta hoje
exige varredura em ~350 blocos de regras.

Problemas conhecidos:

| # | Problema |
|---|---|
| 1 | `.contact-info` duplicado **no mesmo arquivo** (`entityTable.style.css`), o segundo bloco sobrescrevendo o primeiro |
| 2 | `.page` definido identicamente em `index.css` e em `mecanicos.style.css` |
| 3 | 4 arquivos CSS vazios: `App.css`, `clientes.style.css`, `pecas.style.css`, `unidades.style.css` |
| 4 | `pages/Pecas` não importa CSS nenhum, mas usa `.contact-info` — funciona só porque o CSS é global |
| 5 | Classes usadas no JSX sem definição: `.role-badge`, `.role-admin/gerente/mecanico`, `.user-name`, `.unit-name` |
| 6 | Dois sistemas de overlay de modal coexistem (`.form/.form-content` e `.modal/.modal-content`), além de overlays próprios em 5 componentes |
| 7 | Três implementações de tabela: `EntityTable`, `ViewTable`, `PaymentHistoryTable` |

O item 5 é o mais visível: o badge de papel na tela de usuários não tem estilo.

---

## 8. Ferramentas e configuração

### Scripts

```bash
npm run dev            # vite
npm run build          # tsc -b && vite build
npm run lint           # eslint
npm run format         # prettier --write
npm run format:check
```

Não há script de `preview` nem de teste.

### Dependências

Runtime: `react`, `react-dom`, `lucide-react` (ícones), `recharts` (gráficos).

Nenhuma biblioteca de HTTP, de formulário (react-hook-form/zod/yup) ou de data-fetching
(react-query/swr).

### Pontos de atenção na config

| # | Item | Situação |
|---|---|---|
| C1 | **`strict` desligado no TypeScript** | `tsconfig.app.json` não declara `strict` (default do TS é `false`) e não estende base config. Sem `strictNullChecks`, sem `noImplicitAny` |
| C2 | **`react-router-dom` em `devDependencies`** | é dependência de runtime; no lugar errado |
| C3 | Sem framework de teste | zero cobertura |
| C4 | Sem path alias | todos os imports são `../../` |
| C5 | Prettier configurado mas não aplicado em tudo | vários arquivos com aspas simples e sem ponto-e-vírgula, contra o `.prettierrc` |
| C6 | `index.html` com `lang="en"` e `<title>frontend</title>` | app em português |

C1 é o mais sério: sem `strictNullChecks`, o compilador não avisa sobre `undefined` — e
`usuario.oficinaId` é legitimamente `null` para ADMIN. Ligar o `strict` vai revelar erros
reais, e é melhor fazer isso **antes** de escrever a camada HTTP, não depois.

`noUnusedLocals`, `noUnusedParameters` e `noFallthroughCasesInSwitch` estão ligados.

ESLint usa flat config com `tseslint.configs.recommended` — **não** a variante
type-checked.

---

## 9. Plano de integração com a API

Ordem sugerida, das dependências para as folhas:

**1. Ligar o `strict` do TypeScript.** Antes de adicionar código novo. Corrigir os erros
que aparecerem, em especial `oficinaId: number | null`.

**2. Criar a camada HTTP** (`src/api/client.ts`):
- `baseURL` de `import.meta.env.VITE_API_URL`;
- anexar `Authorization: Bearer <token>`;
- centralizar o tratamento de erro no envelope da API (`status`/`error`/`message`/
  `timestamp`, mais `fields` na validação) — ver [api.md §3](./api.md);
- `401` → limpar token e mandar para o login;
- **converter dinheiro**: centavos → decimal ao enviar, decimal → centavos ao receber.

**3. Login de verdade.** Dar estado ao `LoginForm` (hoje é estático e o `onSubmit` está no
`<div>` pai), chamar `POST /api/auth/login`, guardar `accessToken` e usar o bloco
`usuario` da resposta em vez de `MOCK_USUARIO_LOGADO`.

**4. Contexto de autenticação.** Substituir o `useState` em `App.tsx`: `usuarioLogado`
hoje desce por props para quase toda a árvore.

**5. Alinhar as rotas à matriz de permissões.** As três divergências da §4 — Pagamentos,
Mecânicos e Clientes liberados a MECANICO — viram `403` assim que a API entrar.

**6. Substituir um mock por vez**, começando pelos CRUDs simples (Clientes, Veículos) e
deixando `OrdensServico` para o fim, que é a tela mais acoplada.

**7. Remover as regras duplicadas.** `ordemServicoCalculos.ts` e `pagamentoCalculos.ts`
reimplementam regra que o backend já aplica. Depois da integração, o total e o status
devem vir da API — manter as duas implementações é garantir divergência.

**8. Paginação.** `EntityTable` filtra client-side; os endpoints de pessoa são paginados
com `page`/`size`/`sort`. Ver [api.md §4](./api.md).

**9. Estados de carregamento e erro.** `EntityTable` já tem `loading`; falta o caminho de
erro nas páginas.

---

## 10. Convenções ao contribuir

1. **Componente novo:** pasta com `index.tsx` + `<nome>.style.css`.
2. **Formulário novo:** schema em `<nome>Fields.ts` com `defineFields<T>`; não declare
   campos inline.
3. **Máscara nova:** par `format`/`unformat` em `services/formatters.ts` e um novo
   `FieldType` — nunca formate dentro da página.
4. **Tipo novo:** `src/types/<dominio>/`, espelhando o DTO do backend.
5. `npm run lint` e `npm run format` antes de commitar (o hook `pre-commit` roda
   `lint-staged` + `npm run build`).
6. Papel novo ou mudança de permissão: atualizar `types/usuario/role.ts`, o `RequireRole`
   em `App.tsx` e [permissions.md](./permissions.md).
