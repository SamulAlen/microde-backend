import { defineConfig } from '@umijs/max';
import proxy from './config/proxy.config';
import routes from './config/routes';

export default defineConfig({
  routes,
  proxy,
  fastRefresh: true,
  title: 'Microde',

  // 使用 @umijs/max 插件
  antd: {},
  access: {},
  model: {},
  initialState: {},
  request: {},
  dva: {},
});
