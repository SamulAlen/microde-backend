export default [
  {
    path: '/login',
    component: './Login',
    layout: false,
  },
  {
    path: '/register',
    component: './Register',
    layout: false,
  },
  {
    path: '/',
    component: '../layouts/BasicLayout',
    routes: [
      {
        path: '/',
        redirect: '/login',
      },
      {
        path: '/welcome',
        name: 'welcome',
        icon: 'smile',
        component: './Welcome',
        access: 'isLoggedIn',
      },
      {
        path: '/profile',
        name: 'profile',
        icon: 'user',
        component: './Profile',
        access: 'isLoggedIn',
      },
      {
        path: '/user',
        name: 'user',
        icon: 'team',
        access: 'isLoggedIn',
        routes: [
          {
            path: '/user/smart-recommend',
            name: 'smartRecommend',
            icon: 'star',
            component: './User/SmartRecommend',
          },
          {
            path: '/user/recommend',
            name: 'recommend',
            icon: 'like',
            component: './User/Recommend',
          },
          {
            path: '/user/tags',
            name: 'searchByTags',
            icon: 'tags',
            component: './User/SearchByTags',
          },
        ],
      },
      {
        path: '/team',
        name: 'team',
        icon: 'apartment',
        access: 'isLoggedIn',
        routes: [
          {
            path: '/team/list',
            name: 'list',
            icon: 'bars',
            component: './Team/List',
          },
          {
            path: '/team/create',
            name: 'create',
            icon: 'plus-circle',
            component: './Team/Create',
          },
          {
            path: '/team/detail/:id',
            name: 'detail',
            hideInMenu: true,
            component: './Team/Detail',
          },
          {
            path: '/team/edit/:id',
            name: 'edit',
            hideInMenu: true,
            component: './Team/Edit',
          },
        ],
      },
      {
        path: '/admin',
        name: 'admin',
        icon: 'security-scan',
        access: 'canAdmin',
        routes: [
          {
            path: '/admin/users',
            name: 'userManagement',
            component: './Admin/UserManagement',
          },
          {
            path: '/admin/teams',
            name: 'teamManagement',
            component: './Admin/TeamManagement',
          },
        ],
      },
      {
        component: './404',
      },
    ],
  },
];
