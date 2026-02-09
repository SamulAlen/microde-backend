# Microde Frontend

A React-based user management and team collaboration platform built with Ant Design Pro.

## Features

- **User Management**
  - User registration and login
  - Profile management
  - User search and recommendations
  - Tag-based user discovery

- **Team Management**
  - Create and manage teams
  - Public, private, and encrypted teams
  - Team search and filtering
  - Member management

- **Admin Features**
  - User management
  - Delete users
  - Search by username

## Tech Stack

- React 18
- TypeScript
- Ant Design Pro (UMI 4)
- Ant Design 5
- DVA (State Management)

## Getting Started

### Prerequisites

- Node.js 16+
- Backend server running on http://localhost:8080

### Installation

```bash
npm install
```

### Development

```bash
npm start
```

The application will be available at http://localhost:3000

### Build

```bash
npm run build
```

## Configuration

- API proxy configured for `/api` routes
- Backend URL: `http://localhost:8080/api`
- Session-based authentication with Redis

## Project Structure

```
microde-frontend/
├── config/           # Configuration files
├── public/           # Static assets
├── src/
│   ├── components/   # Reusable components
│   ├── pages/        # Page components
│   ├── services/     # API services
│   ├── store/        # State management
│   ├── types/        # TypeScript types
│   ├── access.ts     # Access control
│   ├── app.tsx       # App initialization
│   └── global.tsx    # Global configuration
└── package.json
```

## API Integration

All API endpoints match the backend controller methods:

- **User APIs**: `/api/user/*`
  - register, login, logout, current
  - search, recommend, update, delete

- **Team APIs**: `/api/team/*`
  - add, delete, update, get
  - list, list/page

## License

ISC
