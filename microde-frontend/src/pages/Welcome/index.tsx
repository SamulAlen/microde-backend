import React from 'react';
import { Card, Typography } from 'antd';
import { useModel } from '@umijs/max';

const { Title, Paragraph } = Typography;

const WelcomePage: React.FC = () => {
  const { initialState } = useModel('@@initialState');
  const { currentUser } = initialState || {};

  return (
    <Card>
      <Title level={2}>欢迎来到用户中心</Title>
      <Paragraph>
        你好，{currentUser?.username || currentUser?.userAccount || '游客'}！
      </Paragraph>
      <Paragraph>
        这是一个基于 React + Ant Design Pro 构建的用户管理和团队协作平台。
      </Paragraph>
      <Paragraph>
        使用左侧菜单导航不同功能：
      </Paragraph>
      <ul>
        <li><strong>个人资料</strong> - 查看和编辑您的个人信息</li>
        <li><strong>用户</strong> - 搜索和发现其他用户</li>
        <li><strong>队伍</strong> - 创建和管理团队</li>
      </ul>
    </Card>
  );
};

export default WelcomePage;
