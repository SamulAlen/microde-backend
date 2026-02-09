import { history } from '@umijs/max';

// @ts-ignore
export async function getInitialState(): Promise<{
  currentUser?: API.User;
  settings?: any;
}> {
  // Fetch current user info
  const fetchUserInfo = async () => {
    try {
      const { userServices } = await import('@/services/user');
      const res = await userServices.getCurrentUser();
      if (res.code === 0 && res.data) {
        return res.data;
      }
    } catch (error) {
      console.error('Failed to fetch current user:', error);
    }
    return undefined;
  };

  // If visiting login page, don't fetch user info
  if (history.location.pathname === '/login' || history.location.pathname === '/register') {
    return {
      settings: {},
    };
  }

  const currentUser = await fetchUserInfo();

  return {
    currentUser,
    settings: {},
  };
}
