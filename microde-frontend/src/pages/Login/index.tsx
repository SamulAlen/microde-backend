import React, { useState } from 'react';
import { Form, Input, Button, Card, message } from 'antd';
import { UserOutlined, LockOutlined } from '@ant-design/icons';
import { history, useModel } from '@umijs/max';
import { userServices } from '@/services/user';
import type { UserLoginRequest } from '@/types';

import styles from './index.less';

const LoginPage: React.FC = () => {
  const [loading, setLoading] = useState(false);
  const { setInitialState } = useModel('@@initialState');

  const onFinish = async (values: UserLoginRequest) => {
    setLoading(true);
    try {
      const res = await userServices.login(values);
      if (res.code === 0) {
        message.success('登录成功！');
        // 获取当前用户信息并更新全局状态
        const userRes = await userServices.getCurrentUser();
        if (userRes.code === 0 && userRes.data) {
          setInitialState({ currentUser: userRes.data });
        }
        // 使用 requestAnimationFrame 确保状态更新后再导航
        requestAnimationFrame(() => {
          history.push('/welcome');
        });
      } else {
        message.error(res.message || '登录失败');
      }
    } catch (error) {
      message.error('登录失败，请重试');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className={styles.container}>
      <div className={styles.content}>
        <div className={styles.top}>
          <div className={styles.header}>
            <span className={styles.title}>用户中心</span>
          </div>
          <div className={styles.desc}>用户管理与团队协作平台</div>
        </div>
        <Card className={styles.loginCard}>
          <Form
            name="login"
            initialValues={{ remember: true }}
            onFinish={onFinish}
            autoComplete="off"
            size="large"
          >
            <Form.Item
              name="userAccount"
              rules={[{ required: true, message: '请输入账号' }]}
            >
              <Input
                prefix={<UserOutlined className={styles.prefixIcon} />}
                placeholder="账号"
              />
            </Form.Item>

            <Form.Item
              name="userPassword"
              rules={[{ required: true, message: '请输入密码' }]}
            >
              <Input.Password
                prefix={<LockOutlined className={styles.prefixIcon} />}
                placeholder="密码"
              />
            </Form.Item>

            <Form.Item>
              <Button type="primary" htmlType="submit" loading={loading} block>
                登录
              </Button>
            </Form.Item>

            <div className={styles.footer}>
              <span>还没有账号？</span>
              <a onClick={() => history.push('/register')}>立即注册</a>
            </div>
          </Form>
        </Card>
      </div>
    </div>
  );
};

export default LoginPage;
