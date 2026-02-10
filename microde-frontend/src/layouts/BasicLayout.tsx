import React, { useMemo, useEffect } from 'react';
import { Outlet, useNavigate, useLocation } from '@umijs/max';
import { Layout, Menu, Avatar, Dropdown, Badge, Image, Spin } from 'antd';
import type { MenuProps } from 'antd';
import {
  SmileOutlined,
  UserOutlined,
  TeamOutlined,
  ApartmentOutlined,
  SearchOutlined,
  TagsOutlined,
  BarsOutlined,
  PlusCircleOutlined,
  SecurityScanOutlined,
  LogoutOutlined,
  MenuFoldOutlined,
  MenuUnfoldOutlined,
  LikeOutlined,
  StarOutlined,
} from '@ant-design/icons';
import { useModel } from '@umijs/max';
import { userServices } from '@/services/user';

const { Header, Sider, Content } = Layout;

/**
 * BasicLayout - 主应用布局组件
 * 对应路由配置中的 layout: '../layouts/BasicLayout'
 */
export default function BasicLayout() {
  const navigate = useNavigate();
  const location = useLocation();
  const { initialState, setInitialState, loading } = useModel('@@initialState');
  const [collapsed, setCollapsed] = React.useState(false);

  // 认证检查 - 未登录用户重定向到登录页
  useEffect(() => {
    // 如果正在加载用户信息，等待
    if (loading) return;

    // 检查登录状态
    const isLoggedIn = !!initialState?.currentUser;

    if (!isLoggedIn) {
      // 排除登录和注册页面本身
      const publicRoutes = ['/login', '/register'];
      if (!publicRoutes.includes(location.pathname)) {
        // 未登录访问受保护页面，重定向到登录页
        navigate('/login', { replace: true });
      }
    }
  }, [initialState, loading, location, navigate]);

  // 当前用户信息
  const currentUser = initialState?.currentUser;

  // 退出登录
  const handleLogout = async () => {
    try {
      await userServices.logout();
      setInitialState({ currentUser: undefined });
      navigate('/login');
    } catch (error) {
      console.error('退出登录失败:', error);
    }
  };

  // 用户下拉菜单
  const userMenuItems: MenuProps['items'] = [
    {
      key: 'profile',
      icon: <UserOutlined />,
      label: '个人资料',
      onClick: () => navigate('/profile'),
    },
    {
      type: 'divider',
    },
    {
      key: 'logout',
      icon: <LogoutOutlined />,
      label: '退出登录',
      onClick: handleLogout,
      danger: true,
    },
  ];

  // 侧边栏菜单项（根据 config/routes.ts 配置）
  const menuItems: MenuProps['items'] = useMemo(() => {
    const items: MenuProps['items'] = [
      {
        key: '/welcome',
        icon: <SmileOutlined />,
        label: '欢迎',
      },
      {
        key: '/profile',
        icon: <UserOutlined />,
        label: '个人资料',
      },
      {
        key: '/user',
        icon: <TeamOutlined />,
        label: '伙伴匹配',
        children: [
          {
            key: '/user/smart-recommend',
            icon: <StarOutlined />,
            label: '智能推荐',
          },
          {
            key: '/user/tags',
            icon: <TagsOutlined />,
            label: '标签搜索',
          },
        ],
      },
      {
        key: '/team',
        icon: <ApartmentOutlined />,
        label: '队伍',
        children: [
          {
            key: '/team/list',
            icon: <BarsOutlined />,
            label: '队伍列表',
          },
          {
            key: '/team/create',
            icon: <PlusCircleOutlined />,
            label: '创建队伍',
          },
        ],
      },
    ];

    // 管理员菜单（根据权限动态显示）
    if (currentUser?.userRole === 1) {
      items.push({
        key: '/admin',
        icon: <SecurityScanOutlined />,
        label: '管理员',
        children: [
          {
            key: '/admin/users',
            icon: <UserOutlined />,
            label: '用户管理',
          },
          {
            key: '/admin/teams',
            icon: <TeamOutlined />,
            label: '队伍管理',
          },
        ],
      });
    }

    return items;
  }, [currentUser]);

  // 获取当前选中的菜单 key
  const selectedKeys = useMemo(() => {
    // 精确匹配
    if (menuItems?.some((item) => item?.key === location.pathname)) {
      return [location.pathname];
    }
    // 父级菜单匹配
    const parentKey = menuItems?.find((item) =>
      item?.children?.some((child) => child?.key === location.pathname)
    );
    if (parentKey) {
      return [location.pathname];
    }
    return [];
  }, [location.pathname, menuItems]);

  // 获取展开的菜单 key
  const defaultOpenKeys = useMemo(() => {
    const pathSegments = location.pathname.split('/').filter(Boolean);
    if (pathSegments.length >= 2) {
      return [`/${pathSegments[0]}`];
    }
    return [];
  }, [location.pathname]);

  // 如果正在加载初始状态且不在登录/注册页，显示加载页面
  // 这样可以避免在加载期间渲染页面内容
  if (loading && location.pathname !== '/login' && location.pathname !== '/register') {
    return (
      <div style={{ display: 'flex', justifyContent: 'center', alignItems: 'center', height: '100vh' }}>
        <Spin size="large" tip="加载中..." />
      </div>
    );
  }

  return (
    <Layout style={{ minHeight: '100vh' }}>
      <Sider
        width={200}
        theme="dark"
        style={{
          overflow: 'auto',
          height: '100vh',
          position: 'fixed',
          left: 0,
          top: 0,
          bottom: 0,
          backgroundColor: '#001529',
        }}
        collapsed={collapsed}
        collapsedWidth={collapsed ? 0 : 200}
        trigger={null}
      >
        <div
          style={{
            height: 64,
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
            color: '#fff',
            fontSize: collapsed ? 14 : 18,
            fontWeight: 'bold',
            borderBottom: '1px solid rgba(255, 255, 255, 0.1)',
            padding: '0 16px',
            whiteSpace: 'nowrap',
            overflow: 'hidden',
            textOverflow: 'ellipsis',
          }}
        >
          {collapsed ? 'UC' : '用户中心'}
        </div>
        <Menu
          theme="dark"
          mode="inline"
          selectedKeys={selectedKeys}
          defaultOpenKeys={defaultOpenKeys}
          items={menuItems}
          onClick={({ key }) => navigate(key)}
          style={{ borderRight: 0 }}
        />
      </Sider>

      <Layout style={{ marginLeft: collapsed ? 0 : 200, transition: 'margin-left 0.2s' }}>
        <Header
          style={{
            padding: '0 24px',
            background: '#fff',
            display: 'flex',
            justifyContent: 'space-between',
            alignItems: 'center',
            borderBottom: '1px solid #f0f0f0',
          }}
        >
          <div style={{ display: 'flex', alignItems: 'center', gap: 16 }}>
            {React.createElement(collapsed ? MenuUnfoldOutlined : MenuFoldOutlined, {
              style: { fontSize: 18, cursor: 'pointer' },
              onClick: () => setCollapsed(!collapsed),
            })}
            <span style={{ fontSize: 16, fontWeight: 500, whiteSpace: 'nowrap' }}>用户中心</span>
          </div>

          <div style={{ display: 'flex', alignItems: 'center', gap: 16 }}>
            {currentUser ? (
              <Dropdown menu={{ items: userMenuItems }} placement="bottomRight">
                <div style={{ cursor: 'pointer', display: 'flex', alignItems: 'center', gap: 8 }}>
                  <Badge dot>
                    <Avatar
                      size={32}
                      src={currentUser.avatarUrl}
                      icon={<UserOutlined />}
                      style={{ backgroundColor: '#1890ff' }}
                    />
                  </Badge>
                  <span style={{ fontSize: 14 }}>{currentUser.username || '用户'}</span>
                </div>
              </Dropdown>
            ) : (
              <span onClick={() => navigate('/login')} style={{ cursor: 'pointer' }}>
                请登录
              </span>
            )}
          </div>
        </Header>

        <Content
          style={{
            margin: '24px',
            padding: '24px',
            background: '#fff',
            borderRadius: '8px',
            minHeight: 'calc(100vh - 64px - 48px)',
          }}
        >
          <Outlet />
        </Content>
      </Layout>
    </Layout>
  );
}
