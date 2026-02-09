/**
 * Base API Response Format
 * Matches BaseResponse.java (line 14-55)
 */
export interface BaseResponse<T = any> {
  code: number;
  data: T;
  message: string;
  description: string;
}

/**
 * Error Codes
 * Matches ErrorCode.java (line 11-51)
 */
export enum ErrorCode {
  SUCCESS = 0,
  PARAMS_ERROR = 40000,
  NULL_ERROR = 40001,
  NOT_LOGIN = 40100,
  NO_AUTH = 40101,
  SYSTEM_ERROR = 50000,
}

/**
 * Pagination Request Parameters
 */
export interface PageRequest {
  pageSize?: number;
  pageNum?: number;
}

/**
 * Pagination Result
 * Matches MyBatis Plus Page structure
 */
export interface PageResult<T> {
  records: T[];
  total: number;
  size: number;
  current: number;
  pages: number;
}

/**
 * Smart Recommendation Request
 * Matches RecommendRequest.java
 */
export interface RecommendRequest extends PageRequest {
  userId?: number;
  strategy?: 'all' | 'skill' | 'complement' | 'activity';
  preferredTags?: string[];
  minSimilarity?: number;
}

/**
 * Recommendation Result
 * Matches RecommendationResult.java
 */
export interface RecommendationResult {
  userId: number;
  username: string;
  avatarUrl?: string;
  tags: string[];
  profile?: string;
  similarity: number;
  reasons: string[];
  matchType: string;
}
