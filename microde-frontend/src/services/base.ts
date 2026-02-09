import { extend } from 'umi-request';
import { message } from 'antd';

const request = extend({
  prefix: '/api',
  timeout: 10000,
  headers: {
    'Content-Type': 'application/json',
  },
  credentials: 'include', // CRITICAL for session cookies
});

// Response interceptor handling BaseResponse format
request.interceptors.response.use(
  async (response) => {
    const res = await response.clone().json();

    // Handle business errors from BaseResponse
    if (res.code !== 0) {
      switch (res.code) {
        case 40100: // NOT_LOGIN
          message.error('Please login first');
          // Redirect to login
          window.location.href = '/login';
          break;
        case 40101: // NO_AUTH
          message.error('No permission');
          break;
        case 40000: // PARAMS_ERROR
          message.error(res.message || 'Parameter error');
          break;
        case 40001: // NULL_ERROR
          message.error('Data is empty');
          break;
        case 50000: // SYSTEM_ERROR
          message.error('System error, please try again later');
          break;
        default:
          message.error(res.message || 'Request failed');
      }
    }

    return response;
  },
  (error) => {
    // Handle network errors
    message.error('Network error, please try again');
    return Promise.reject(error);
  }
);

export default request;
