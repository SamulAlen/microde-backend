/**
 * User Entity
 * Matches User.java (line 17-99)
 */
export interface User {
  id: number;
  username?: string;
  userAccount: string;
  avatarUrl?: string;
  gender?: number;
  userPassword?: string;
  phone?: string;
  email?: string;
  tags?: string;
  userStatus: number;
  createTime: string;
  updateTime: string;
  isDelete?: number;
  userRole: number;
  planetCode?: string;
}

/**
 * User Login Request
 * Matches UserLoginRequest.java (line 16-30)
 */
export interface UserLoginRequest {
  userAccount: string;
  userPassword: string;
}

/**
 * User Register Request
 * Matches UserRegisterRequest.java (line 14-37)
 */
export interface UserRegisterRequest {
  userAccount: string;
  userPassword: string;
  checkPassword: string;
  planetCode: string;
}

/**
 * User Roles
 */
export enum UserRole {
  USER = 0,
  ADMIN = 1,
}

/**
 * User Status
 */
export enum UserStatus {
  NORMAL = 0,
  BANNED = 1,
}

/**
 * Gender
 */
export enum Gender {
  MALE = 0,
  FEMALE = 1,
  SECRET = 2,
}
