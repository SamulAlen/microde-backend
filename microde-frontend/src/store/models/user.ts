import type { Effect, Reducer } from 'umi';
import type { User } from '@/types';
import { userServices } from '@/services/user';

export interface UserModelState {
  currentUser?: User;
  isLoggedIn: boolean;
}

export interface UserModelType {
  namespace: 'user';
  state: UserModelState;
  effects: {
    fetchCurrent: Effect;
    login: Effect;
    logout: Effect;
    updateProfile: Effect;
  };
  reducers: {
    saveCurrentUser: Reducer<UserModelState>;
    clearCurrentUser: Reducer<UserModelState>;
  };
}

const UserModel: UserModelType = {
  namespace: 'user',

  state: {
    currentUser: undefined,
    isLoggedIn: false,
  },

  effects: {
    *fetchCurrent(_, { call, put }) {
      try {
        const res = yield call(userServices.getCurrentUser);
        if (res.code === 0 && res.data) {
          yield put({
            type: 'saveCurrentUser',
            payload: res.data,
          });
        }
      } catch (error) {
        console.error('Failed to fetch current user:', error);
      }
    },

    *login({ payload }, { call, put }) {
      try {
        const res = yield call(userServices.login, payload);
        if (res.code === 0 && res.data) {
          yield put({
            type: 'saveCurrentUser',
            payload: res.data,
          });
        }
        return res;
      } catch (error) {
        console.error('Login failed:', error);
        throw error;
      }
    },

    *logout(_, { call, put }) {
      try {
        yield call(userServices.logout);
        yield put({ type: 'clearCurrentUser' });
      } catch (error) {
        console.error('Logout failed:', error);
        // Still clear local state even if API call fails
        yield put({ type: 'clearCurrentUser' });
      }
    },

    *updateProfile({ payload }, { call, put }) {
      try {
        const res = yield call(userServices.updateUser, payload);
        if (res.code === 0) {
          // Refresh current user data
          yield put({ type: 'fetchCurrent' });
        }
        return res;
      } catch (error) {
        console.error('Update profile failed:', error);
        throw error;
      }
    },
  },

  reducers: {
    saveCurrentUser(state, { payload }) {
      return {
        ...state,
        currentUser: payload,
        isLoggedIn: true,
      };
    },

    clearCurrentUser() {
      return {
        currentUser: undefined,
        isLoggedIn: false,
      };
    },
  },
};

export default UserModel;
