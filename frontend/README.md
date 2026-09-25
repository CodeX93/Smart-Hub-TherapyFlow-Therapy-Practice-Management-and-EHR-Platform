# Trappy Flow Frontend


A modern healthcare management platform built with React, TypeScript, and Vite. This application provides a comprehensive portal for clients, therapists, and administrators to manage appointments, billing, documents, and clinical forms.

## 🚀 Features

### User (Client) Features
- **Appointments**: View and schedule therapy sessions
- **Invoices**: Access billing history and payment status
- **Documents**: Securely upload and manage documents (insurance cards, forms, etc.)
- **Clinical Forms**: View and complete clinical assessment forms
- **Privacy Settings**: Manage consent settings and data protection preferences
- **Notifications**: Real-time notification system with dropdown interface

### Therapist Features
- **Dashboard**: Overview of clients and appointments
- **Clients**: Manage client information
- **Scheduling**: Schedule and manage appointments
- **Billings**: Handle billing and invoicing
- **Tasks**: Manage therapy-related tasks

### Admin Features
- **Dashboard**: System overview and analytics
- **Settings**: Platform configuration and management

## 🛠️ Tech Stack

- **React 19** - UI library
- **TypeScript** - Type safety
- **Vite** - Build tool and dev server
- **React Router DOM** - Client-side routing
- **Tailwind CSS 4** - Utility-first CSS framework
- **React Hook Form** - Form state management
- **Zod** - Schema validation
- **Radix UI** - Accessible component primitives
- **Lucide React** - Icon library
- **Class Variance Authority** - Component variant management

## 📦 Installation

1. Clone the repository:
```bash
git clone <repository-url>
cd trappy-flow-frontend
```

2. Install dependencies:
```bash
npm install
```

3. Start the development server:
```bash
npm run dev
```

The application will be available at `http://localhost:5173` (or the port specified by Vite).

## 🏗️ Project Structure

```
src/
├── components/          # Reusable UI components
│   ├── ui/             # Base UI components (Button, Input, Badge, etc.)
│   ├── shared/         # Shared components (Pagination, NotificationDropdown, etc.)
│   ├── form/           # Form-related components
│   ├── sidebar/        # Sidebar navigation
│   ├── topbar/         # Top navigation bar
│   └── ...
├── layouts/            # Layout components (UserLayout, TherapistLayout, etc.)
├── pages/              # Page components organized by feature
│   ├── auth/           # Authentication pages
│   ├── user/           # User/client pages
│   ├── therapist/      # Therapist pages
│   └── admin/          # Admin pages
├── routes/             # Route configuration
├── types/              # TypeScript type definitions
├── schemas/            # Zod validation schemas
├── utils/              # Utility functions
└── lib/                # Library configurations
```

## 🎨 Styling

The project uses **Tailwind CSS 4** with custom CSS variables defined in `src/index.css`. Key design tokens:

- **Primary Colors**: `--text-primary-500` (#517889), `--bg-primary-dark`, etc.
- **Neutral Colors**: `--text-neutral-600`, `--text-neutral-400`, etc.
- **Typography**: Manrope font family
- **Spacing & Layout**: Tailwind utility classes

## 🔐 Authentication

The application supports role-based authentication with three user roles:
- **User/Client**: Access to appointments, invoices, documents, and clinical forms
- **Therapist**: Access to client management, scheduling, and billing
- **Admin**: System administration and settings

Authentication state is managed via `localStorage` with automatic route protection.

## 📝 Available Scripts

- `npm run dev` - Start development server
- `npm run build` - Build for production
- `npm run preview` - Preview production build
- `npm run lint` - Run ESLint

## 🧩 Key Components

### Reusable UI Components
- `Button` - Customizable button with variants
- `Input` - Form input component
- `Badge` - Status and label badges
- `Switch` - Toggle switch component
- `DropdownMenu` - Dropdown menu component
- `Avatar` - User avatar display

### Feature Components
- `NotificationDropdown` - Notification system with date grouping
- `Pagination` - Pagination controls
- `ClinicalFormCard` - Clinical form display card
- `InvoiceTable` - Invoice data table
- `DocumentTable` - Document management table

## 🔄 State Management

- **Local State**: React `useState` for component-level state
- **Form State**: React Hook Form for form management
- **Routing**: React Router for navigation and route state
- **Persistence**: `localStorage` for user session

## 🎯 Development Guidelines

### Code Style
- Use TypeScript for all components
- Follow React functional component patterns
- Use Tailwind CSS for styling (avoid inline styles)
- Extract static content to `.static.ts` files
- Define types in dedicated type files

### Component Organization
- Keep components small and focused
- Extract reusable logic into custom hooks
- Use composition over inheritance
- Follow the existing folder structure

### Styling Best Practices
- Use CSS variables for colors (defined in `index.css`)
- Prefer Tailwind utility classes
- Use `cn()` utility for conditional class merging
- Maintain consistent spacing and typography

## 🚧 Environment Setup

Create a `.env` file in the root directory for environment-specific variables:

```env
VITE_API_URL=your_api_url_here
```

## 📄 License

[Add your license information here]

## 🤝 Contributing

[Add contribution guidelines here]

## 📞 Support

For questions or issues, please contact [your contact information].
