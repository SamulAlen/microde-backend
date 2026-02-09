import type { InitialState } from '@umijs/max';
import type { User } from '@/types';

export default (initialState: InitialState | undefined) => {
  const { currentUser } = initialState || {};
  return {
    canAdmin: currentUser && (currentUser as User).userRole === 1,
    canEditTeam: (team: any) =>
      currentUser && (currentUser as User).id === team.userId,
    isLoggedIn: !!currentUser,
  };
};
