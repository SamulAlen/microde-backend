/**
 * Team Entity
 * Matches Team.java (line 15-74)
 */
export interface Team {
  id: number;
  name: string;
  description?: string;
  maxNum: number;
  expireTime?: string;
  userId: number;
  status: number;
  password?: string;
  createTime: string;
  updateTime: string;
  isDelete?: number;
}

/**
 * Team Add Request
 * Matches TeamAddRequest.java (line 16-55)
 */
export interface TeamAddRequest {
  name: string;
  description?: string;
  maxNum: number;
  expireTime?: string;
  status: number;
  password?: string;
}

/**
 * Team Query Request
 * Matches TeamQuery.java (line 9-35)
 */
export interface TeamQuery {
  name?: string;
  description?: string;
  maxNum?: number;
  userId?: number;
  status?: number;
  pageNum?: number;
  pageSize?: number;
}

/**
 * Team Status
 */
export enum TeamStatus {
  PUBLIC = 0,
  PRIVATE = 1,
  ENCRYPTED = 2,
}

/**
 * User Team Join Request
 * Matches UserTeamJoinRequest.java
 */
export interface UserTeamJoinRequest {
  teamId: number;
  password?: string;
}
