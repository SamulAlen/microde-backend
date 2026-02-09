import React from 'react';
import { Outlet } from '@umijs/max';
import { Card } from 'antd';

/**
 * UserLayout - 登录/注册页面布局
 * 简洁的居中卡片式布局，用于登录和注册页面
 */
export default function UserLayout() {
  return (
    <div
      style={{
        minHeight: '100vh',
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'center',
        background: 'linear-gradient(135deg, #667eea 0%, #764ba2 100%)',
        padding: '24px',
      }}
    >
      <div
        style={{
          width: '100%',
          maxWidth: 400,
        }}
      >
        <Card
          bordered={false}
          style={{
            borderRadius: '12px',
            boxShadow: '0 20px 60px rgba(0, 0, 0, 0.15)',
          }}
        >
          <div
            style={{
              textAlign: 'center',
              marginBottom: '32px',
            }}
          >
            <h1
              style={{
                fontSize: '28px',
                fontWeight: 'bold',
                margin: 0,
                background: 'linear-gradient(135deg, #667eea 0%, #764ba2 100%)',
                WebkitBackgroundClip: 'text',
                WebkitTextFillColor: 'transparent',
                backgroundClip: 'text',
              }}
            >
              Microde
            </h1>
            <p style={{ color: '#666', marginTop: '8px', marginBottom: 0 }}>
              欢迎使用 Microde 系统
            </p>
          </div>
          <Outlet />
        </Card>

        <div
          style={{
            textAlign: 'center',
            marginTop: '24px',
            color: 'rgba(255, 255, 255, 0.8)',
            fontSize: '14px',
          }}
        >
          <p style={{ margin: 0 }}>© 2024 Microde. Created by Samul</p>
        </div>
      </div>
    </div>
  );
}
