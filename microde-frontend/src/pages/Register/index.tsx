import React, { useState } from 'react';
import { Form, Input, Button, Card, message } from 'antd';
import { UserOutlined, LockOutlined, SafetyOutlined, GlobalOutlined } from '@ant-design/icons';
import { history } from '@umijs/max';
import { userServices } from '@/services/user';
import type { UserRegisterRequest } from '@/types';

import styles from './index.less';

const RegisterPage: React.FC = () => {
  const [loading, setLoading] = useState(false);
  const [form] = Form.useForm();

  const onFinish = async (values: UserRegisterRequest) => {
    setLoading(true);
    try {
      const res = await userServices.register(values);
      if (res.code === 0) {
        message.success('注册成功！请登录');
        history.push('/login');
      }
    } catch (error) {
      message.error('注册失败，请重试');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className={styles.container}>
      <div className={styles.content}>
        <div className={styles.top}>
          <div className={styles.header}>
            <span className={styles.title}>注册</span>
          </div>
          <div className={styles.desc}>创建账号开始使用</div>
        </div>
        <Card className={styles.registerCard}>
          <Form
            form={form}
            name="register"
            onFinish={onFinish}
            autoComplete="off"
            size="large"
          >
            <Form.Item
              name="userAccount"
              rules={[
                { required: true, message: '请输入账号' },
                { min: 4, message: '账号至少4个字符' },
              ]}
            >
              <Input
                prefix={<UserOutlined className={styles.prefixIcon} />}
                placeholder="账号"
              />
            </Form.Item>

            <Form.Item
              name="userPassword"
              rules={[
                { required: true, message: '请输入密码' },
                { min: 6, message: '密码至少6个字符' },
              ]}
            >
              <Input.Password
                prefix={<LockOutlined className={styles.prefixIcon} />}
                placeholder="密码"
              />
            </Form.Item>

            <Form.Item
              name="checkPassword"
              dependencies={['userPassword']}
              rules={[
                { required: true, message: '请确认密码' },
                ({ getFieldValue }) => ({
                  validator(_, value) {
                    if (!value || getFieldValue('userPassword') === value) {
                      return Promise.resolve();
                    }
                    return Promise.reject(new Error('两次输入的密码不一致'));
                  },
                }),
              ]}
            >
              <Input.Password
                prefix={<SafetyOutlined className={styles.prefixIcon} />}
                placeholder="确认密码"
              />
            </Form.Item>

            <Form.Item
              name="planetCode"
              rules={[{ required: true, message: '请输入星球编号' }]}
            >
              <Input
                prefix={<GlobalOutlined className={styles.prefixIcon} />}
                placeholder="星球编号"
              />
            </Form.Item>

            <Form.Item>
              <Button type="primary" htmlType="submit" loading={loading} block>
                注册
              </Button>
            </Form.Item>

            <div className={styles.footer}>
              <span>已有账号？</span>
              <a onClick={() => history.push('/login')}>立即登录</a>
            </div>
          </Form>
        </Card>
      </div>
    </div>
  );
};

export default RegisterPage;
