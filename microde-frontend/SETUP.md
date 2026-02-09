# Microde Frontend Project

## Project Summary

A complete React + Ant Design Pro frontend application for the Microde backend system. All backend APIs have been integrated and the application is ready to run.

## Project Location

```
d:\Workspace\IDEAworkspace\microde-backend\microde-frontend\
```

## Features Implemented

### Authentication Module
- ✅ User Registration (`/register`)
- ✅ User Login (`/login`)
- ✅ User Logout
- ✅ Session management with Redis cookies

### User Module
- ✅ Profile page (`/profile`) - View and edit user information
- ✅ User Search (`/user/search`) - Admin only user search
- ✅ Search by Tags (`/user/tags`) - Tag-based user discovery
- ✅ Recommended Users (`/user/recommend`) - Paginated user recommendations

### Team Module
- ✅ Team List (`/team/list`) - View and filter teams
- ✅ Create Team (`/team/create`) - Create new teams
- ✅ Team Detail (`/team/detail/:id`) - View team information
- ✅ Edit Team (`/team/edit/:id`) - Update team information
- ✅ Delete Team functionality

### Admin Module
- ✅ User Management (`/admin/users`) - Admin user management interface

## Tech Stack

- **React 18** - UI library
- **TypeScript** - Type safety
- **Ant Design 5** - UI components
- **UMI 4** - Framework and routing
- **DVA** - State management
- **dayjs** - Date handling

## Quick Start

### Prerequisites
1. Backend server running on `http://localhost:8080`
2. MySQL database configured
3. Redis running for session storage

### Installation
```bash
cd microde-frontend
npm install
```

### Development
```bash
npm start
```
The application will open at `http://localhost:3000`

### Build for Production
```bash
npm run build
```

## API Integration

All backend APIs are integrated:

### User APIs (`/api/user/*`)
- `POST /register` - User registration
- `POST /login` - User login
- `POST /logout` - User logout
- `GET /current` - Get current user
- `GET /search` - Search users (admin)
- `GET /search/tags` - Search by tags
- `GET /recommend` - Recommended users
- `POST /update` - Update user profile
- `POST /delete` - Delete user (admin)

### Team APIs (`/api/team/*`)
- `POST /add` - Create team
- `POST /delete` - Delete team
- `POST /update` - Update team
- `GET /get` - Get team by ID
- `GET /list` - List teams
- `GET /list/page` - List teams with pagination

## Project Structure

```
microde-frontend/
├── config/                    # Configuration files
│   ├── config.ts             # Main configuration
│   ├── proxy.config.ts       # API proxy settings
│   ├── routes.ts             # Route definitions
│   └── defaultSettings.ts    # Default theme settings
├── public/                    # Static assets
│   ├── favicon.ico
│   └── logo.svg
├── src/
│   ├── components/           # Reusable components (empty for now)
│   ├── pages/                # Page components
│   │   ├── 404.tsx
│   │   ├── Login/
│   │   ├── Register/
│   │   ├── Profile/
│   │   ├── User/
│   │   ├── Team/
│   │   ├── Admin/
│   │   └── Welcome/
│   ├── services/             # API services
│   │   ├── base.ts           # Base request configuration
│   │   ├── user.ts           # User API services
│   │   └── team.ts           # Team API services
│   ├── store/                # State management
│   │   ├── models/
│   │   │   ├── user.ts       # User state model
│   │   │   └── team.ts       # Team state model
│   │   └── index.ts
│   ├── types/                # TypeScript definitions
│   │   ├── api.ts            # Base response types
│   │   ├── user.ts           # User types
│   │   ├── team.ts           # Team types
│   │   └── index.ts
│   ├── access.ts             # Access control
│   ├── app.tsx               # App initialization
│   ├── global.tsx            # Global configuration
│   └── global.less           # Global styles
├── .env                       # Environment variables
├── .eslintrc.js               # ESLint configuration
├── .prettierrc.js             # Prettier configuration
├── .umirc.ts                  # UMI configuration
├── package.json               # Dependencies
├── tsconfig.json              # TypeScript configuration
└── README.md                  # Project documentation
```

## Authentication Flow

1. **Login**: User submits credentials → POST `/api/user/login` → Backend creates session → Cookie stored automatically
2. **Authenticated Requests**: Frontend sends requests with cookies → Backend validates session from Redis → Returns data
3. **Logout**: Frontend calls POST `/api/user/logout` → Backend invalidates session → Frontend clears local state

## Configuration

- **API Proxy**: Configured in `config/proxy.config.ts` to forward `/api` requests to `http://localhost:8080`
- **CORS**: Backend configured to allow requests from `http://localhost:3000`
- **Session**: Uses `credentials: 'include'` for cookie transmission

## Development Notes

1. The project uses UMI 4.x with Ant Design Pro components
2. All TypeScript types are defined to match backend models exactly
3. State management uses DVA models for user and team data
4. Access control implemented for admin-only routes
5. Error handling integrated with Ant Design message notifications

## Next Steps

1. Start the backend server
2. Run `npm start` in the frontend directory
3. Open browser to `http://localhost:3000`
4. Register a new user and test all features

## Backend Reference Files

When making changes, reference these backend files:
- `UserController.java` (line 40-218) - User endpoints
- `TeamController.java` (line 34-115) - Team endpoints
- `BaseResponse.java` (line 14-55) - Response format
- `User.java` (line 17-99) - User entity
- `Team.java` (line 15-74) - Team entity
- `ErrorCode.java` (line 11-51) - Error codes
