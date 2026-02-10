import request from './base';
import type {
  User,
  UserLoginRequest,
  UserRegisterRequest,
  BaseResponse,
  PageResult,
  RecommendRequest,
  RecommendationResult,
} from '@/types';

/**
 * User API Services
 * Matches UserController endpoints (line 54-218)
 */
export const userServices = {
  /**
   * POST /user/register (line 54)
   * User registration
   */
  register: (data: UserRegisterRequest): Promise<BaseResponse<number>> => {
    return request('/user/register', {
      method: 'POST',
      data,
    });
  },

  /**
   * POST /user/login (line 78)
   * User login
   */
  login: (data: UserLoginRequest): Promise<BaseResponse<User>> => {
    return request('/user/login', {
      method: 'POST',
      data,
    });
  },

  /**
   * POST /user/logout (line 98)
   * User logout
   */
  logout: (): Promise<BaseResponse<number>> => {
    return request('/user/logout', {
      method: 'POST',
    });
  },

  /**
   * GET /user/current (line 113)
   * Get current logged-in user
   */
  getCurrentUser: (): Promise<BaseResponse<User>> => {
    return request('/user/current', {
      method: 'GET',
    });
  },

  /**
   * GET /user/search (line 125)
   * Search users by username (Admin only)
   */
  searchUsers: (username?: string): Promise<BaseResponse<User[]>> => {
    return request('/user/search', {
      method: 'GET',
      params: { username },
    });
  },

  /**
   * GET /user/search/tags (line 148)
   * Search users by tags with pagination
   */
  searchUsersByTags: (params: {
    pageSize: number;
    pageNum: number;
    tagNameList?: string[];
  }): Promise<BaseResponse<PageResult<User>>> => {
    return request('/user/search/tags', {
      method: 'GET',
      params,
    });
  },


  /**
   * POST /user/update (line 179)
   * Update user profile
   */
  updateUser: (data: Partial<User>): Promise<BaseResponse<number>> => {
    return request('/user/update', {
      method: 'POST',
      data,
    });
  },

  /**
   * POST /user/delete (line 191)
   * Delete user by id (Admin only)
   */
  deleteUser: (id: number): Promise<BaseResponse<boolean>> => {
    return request('/user/delete', {
      method: 'POST',
      data: id,
    });
  },

  /**
   * POST /user/recommend/smart
   * Smart recommend users based on tags, skills, activity
   */
  smartRecommend: (
    recommendRequest: RecommendRequest,
  ): Promise<BaseResponse<PageResult<RecommendationResult>>> => {
    return request('/user/recommend/smart', {
      method: 'POST',
      data: recommendRequest,
    });
  },

  /**
   * POST /user/recommend/feedback
   * Record feedback for recommendation results
   */
  recommendFeedback: (
    recommendedUserId: number,
    feedback: number,
  ): Promise<BaseResponse<boolean>> => {
    return request('/user/recommend/feedback', {
      method: 'POST',
      params: { recommendedUserId, feedback },
    });
  },

  /**
   * POST /precompute/refresh
   * Clear cache and refresh recommendations (换一批)
   */
  refreshRecommendations: (params?: {
    userId?: number;
    strategy?: string;
    preferredTags?: string[];
  }): Promise<BaseResponse<any>> => {
    return request('/precompute/refresh', {
      method: 'POST',
      params,
    });
  },

  /**
   * POST /user/tags/update
   * Update user tags
   */
  updateUserTags: (tags: string[]): Promise<BaseResponse<boolean>> => {
    return request('/user/tags/update', {
      method: 'POST',
      data: tags,
    });
  },

  /**
   * POST /user/ban
   * Ban or unban user (Admin only)
   */
  banUser: (id: number, status: number): Promise<BaseResponse<boolean>> => {
    return request('/user/ban', {
      method: 'POST',
      params: { id, status },
    });
  },
};
