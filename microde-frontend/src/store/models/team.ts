import type { Effect, Reducer } from 'umi';
import type { Team, TeamQuery, TeamAddRequest } from '@/types';
import { teamServices } from '@/services/team';

export interface TeamModelState {
  teams: Team[];
  currentTeam?: Team;
  pagination: {
    current: number;
    pageSize: number;
    total: number;
  };
}

export interface TeamModelType {
  namespace: 'team';
  state: TeamModelState;
  effects: {
    fetchTeams: Effect;
    fetchTeamsByPage: Effect;
    fetchTeamDetail: Effect;
    createTeam: Effect;
    updateTeam: Effect;
    deleteTeam: Effect;
  };
  reducers: {
    saveTeams: Reducer<TeamModelState>;
    saveCurrentTeam: Reducer<TeamModelState>;
    updatePagination: Reducer<TeamModelState>;
  };
}

const TeamModel: TeamModelType = {
  namespace: 'team',

  state: {
    teams: [],
    currentTeam: undefined,
    pagination: {
      current: 1,
      pageSize: 10,
      total: 0,
    },
  },

  effects: {
    *fetchTeams(_, { call, put }) {
      try {
        const res = yield call(teamServices.listTeams, {});
        if (res.code === 0 && res.data) {
          yield put({
            type: 'saveTeams',
            payload: res.data,
          });
        }
      } catch (error) {
        console.error('Failed to fetch teams:', error);
      }
    },

    *fetchTeamsByPage({ payload }, { call, put }) {
      try {
        const res = yield call(teamServices.listTeamsByPage, payload);
        if (res.code === 0 && res.data) {
          yield put({
            type: 'saveTeams',
            payload: res.data.records || [],
          });
          yield put({
            type: 'updatePagination',
            payload: {
              current: res.data.current || 1,
              pageSize: res.data.size || 10,
              total: res.data.total || 0,
            },
          });
        }
      } catch (error) {
        console.error('Failed to fetch teams by page:', error);
      }
    },

    *fetchTeamDetail({ payload }, { call, put }) {
      try {
        const res = yield call(teamServices.getTeamById, payload);
        if (res.code === 0 && res.data) {
          yield put({
            type: 'saveCurrentTeam',
            payload: res.data,
          });
        }
      } catch (error) {
        console.error('Failed to fetch team detail:', error);
      }
    },

    *createTeam({ payload }, { call }) {
      try {
        const res = yield call(teamServices.createTeam, payload);
        return res;
      } catch (error) {
        console.error('Failed to create team:', error);
        throw error;
      }
    },

    *updateTeam({ payload }, { call }) {
      try {
        const res = yield call(teamServices.updateTeam, payload);
        return res;
      } catch (error) {
        console.error('Failed to update team:', error);
        throw error;
      }
    },

    *deleteTeam({ payload }, { call }) {
      try {
        const res = yield call(teamServices.deleteTeam, payload);
        return res;
      } catch (error) {
        console.error('Failed to delete team:', error);
        throw error;
      }
    },
  },

  reducers: {
    saveTeams(state, { payload }) {
      return {
        ...state,
        teams: payload,
      };
    },

    saveCurrentTeam(state, { payload }) {
      return {
        ...state,
        currentTeam: payload,
      };
    },

    updatePagination(state, { payload }) {
      return {
        ...state,
        pagination: payload,
      };
    },
  },
};

export default TeamModel;
