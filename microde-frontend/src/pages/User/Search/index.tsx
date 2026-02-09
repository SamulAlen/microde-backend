import React, { useState } from 'react';
import { Card, Input, Row, Col, Table, Tag, Avatar, message } from 'antd';
import { UserOutlined, SearchOutlined } from '@ant-design/icons';
import { useAccess } from '@umijs/max';
import { userServices } from '@/services/user';
import type { User } from '@/types';

const { Search } = Input;

const UserSearchPage: React.FC = () => {
  const [loading, setLoading] = useState(false);
  const [users, setUsers] = useState<User[]>([]);
  const access = useAccess();

  const handleSearch = async (username: string) => {
    if (!access.canAdmin) {
      message.error('需要管理员权限');
      return;
    }

    setLoading(true);
    try {
      const res = await userServices.searchUsers(username);

      if (res.code === 0 && res.data) {
        setUsers(res.data);
      }
    } catch (error) {
      console.error('Failed to search users:', error);
      message.error('搜索失败');
    } finally {
      setLoading(false);
    }
  };

  const columns = [
    {
      title: '头像',
      dataIndex: 'avatarUrl',
      key: 'avatar',
      width: 80,
      render: (avatarUrl: string) => (
        <Avatar
          size={40}
          src={avatarUrl}
          icon={<UserOutlined />}
          style={{ backgroundColor: '#1890ff' }}
        />
      ),
    },
    {
      title: '用户名',
      dataIndex: 'username',
      key: 'username',
      render: (username: string, record: User) => username || record.userAccount,
    },
    {
      title: '账号',
      dataIndex: 'userAccount',
      key: 'userAccount',
    },
    {
      title: '星球编号',
      dataIndex: 'planetCode',
      key: 'planetCode',
    },
    {
      title: '邮箱',
      dataIndex: 'email',
      key: 'email',
    },
    {
      title: '角色',
      dataIndex: 'userRole',
      key: 'userRole',
      render: (role: number) => (role === 1 ? '管理员' : '普通用户'),
    },
    {
      title: '标签',
      dataIndex: 'tags',
      key: 'tags',
      render: (tags: string) => {
        if (!tags) return '-';
        const tagList = JSON.parse(tags);
        return tagList.map((tag: string, index: number) => (
          <Tag key={index} color="blue">
            {tag}
          </Tag>
        ));
      },
    },
  ];

  return (
    <Card title="用户搜索（仅管理员）">
      <Row gutter={[16, 16]}>
        <Col span={24}>
          <Search
            placeholder="按用户名搜索"
            enterButton={<SearchOutlined />}
            size="large"
            onSearch={handleSearch}
            loading={loading}
          />
        </Col>
        <Col span={24}>
          <Table
            dataSource={users}
            columns={columns}
            rowKey="id"
            loading={loading}
            pagination={{ pageSize: 10 }}
          />
        </Col>
      </Row>
    </Card>
  );
};

export default UserSearchPage;
