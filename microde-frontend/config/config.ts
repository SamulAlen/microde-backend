import { defineConfig } from '@umijs/max';
import proxy from './proxy.config';
import routes from './routes';

export default defineConfig({
  antd: {},
  access: {},
  model: {},
  initialState: {},
  request: {},
  layout: {
    name: 'Microde',
    locale: false,
    siderWidth: 72,
    theme: {
      'sider-bg': '#001529',
    },
  },
  routes,
  proxy,
  fastRefresh: {},
  dva: {},
  outputPath: 'dist',
  base: '/',
  publicPath: '/',
  title: 'Microde',
  favicon: '/favicon.ico',
  logo: '/logo.svg',
});
