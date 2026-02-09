import request from './base';
import type {
  Team,
  TeamAddRequest,
  TeamQuery,
  UserTeamJoinRequest,
  BaseResponse,
  PageResult,
  User,
} from '@/types';

/**
 * Team API Services
 * Matches TeamController endpoints (line 42-113)
 */
export const teamServices = {
  /**
   * POST /team/add (line 42)
   * Create a new team
   */
  createTeam: (data: TeamAddRequest): Promise<BaseResponse<number>> => {
    return request('/team/add', {
      method: 'POST',
      data,
    });
  },

  /**
   * POST /team/delete (line 54)
   * Delete team by id
   */
  deleteTeam: (id: number): Promise<BaseResponse<boolean>> => {
    return request('/team/delete', {
      method: 'POST',
      params: { id },
    });
  },

  /**
   * POST /team/update (line 66)
   * Update team information
   */
  updateTeam: (data: Team): Promise<BaseResponse<boolean>> => {
    return request('/team/update', {
      method: 'POST',
      data,
    });
  },

  /**
   * GET /team/get (line 78)
   * Get team by id
   */
  getTeamById: (id: number): Promise<BaseResponse<Team>> => {
    return request('/team/get', {
      method: 'GET',
      params: { id },
    });
  },

  /**
   * GET /team/list (line 90)
   * List teams with filters
   */
  listTeams: (query: Partial<TeamQuery>): Promise<BaseResponse<Team[]>> => {
    return request('/team/list', {
      method: 'GET',
      params: query,
    });
  },

  /**
   * GET /team/list/page (line 102)
   * List teams with pagination
   */
  listTeamsByPage: (query: TeamQuery): Promise<BaseResponse<PageResult<Team>>> => {
    return request('/team/list/page', {
      method: 'GET',
      params: query,
    });
  },

  /**
   * POST /userTeam/join
   * Join a team
   */
  joinTeam: (data: UserTeamJoinRequest): Promise<BaseResponse<boolean>> => {
    return request('/userTeam/join', {
      method: 'POST',
      data,
    });
  },

  /**
   * POST /userTeam/quit
   * Quit a team
   */
  quitTeam: (data: { teamId: number }): Promise<BaseResponse<boolean>> => {
    return request('/userTeam/quit', {
      method: 'POST',
      data,
    });
  },

  /**
   * GET /userTeam/listMembers/{teamId}
   * Get team members
   */
  getTeamMembers: (teamId: number): Promise<BaseResponse<User[]>> => {
    return request(`/userTeam/listMembers/${teamId}`, {
      method: 'GET',
    });
  },
};
