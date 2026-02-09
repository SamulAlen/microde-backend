import React, { useState } from 'react';
import { Card, Table, Input, Button, Space, Modal, message, Tag, Avatar } from 'antd';
import { UserOutlined, SearchOutlined, DeleteOutlined } from '@ant-design/icons';
import { userServices } from '@/services/user';
import type { User } from '@/types';

const { Search } = Input;

const AdminUserManagementPage: React.FC = () => {
  const [loading, setLoading] = useState(false);
  const [users, setUsers] = useState<User[]>([]);
  const [searchText, setSearchText] = useState('');

  const fetchUsers = async (username?: string) => {
    setLoading(true);
    try {
      const res = await userServices.searchUsers(username);
      if (res.code === 0 && res.data) {
        setUsers(res.data);
      }
    } catch (error) {
      message.error('获取用户失败');
    } finally {
      setLoading(false);
    }
  };

  React.useEffect(() => {
    fetchUsers();
  }, []);

  const handleSearch = (value: string) => {
    setSearchText(value);
    fetchUsers(value || undefined);
  };

  const handleDelete = (user: User) => {
    Modal.confirm({
      title: '删除用户',
      content: `确定要删除用户 "${user.username || user.userAccount}" 吗？`,
      onOk: async () => {
        try {
          const res = await userServices.deleteUser(user.id);
          if (res.code === 0) {
            message.success('删除成功');
            fetchUsers(searchText || undefined);
          }
        } catch (error) {
          message.error('删除失败');
        }
      },
    });
  };

  const columns = [
    {
      title: 'ID',
      dataIndex: 'id',
      key: 'id',
      width: 80,
    },
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
      title: '电话',
      dataIndex: 'phone',
      key: 'phone',
    },
    {
      title: '角色',
      dataIndex: 'userRole',
      key: 'userRole',
      render: (role: number) => (
        <Tag color={role === 1 ? 'red' : 'blue'}>
          {role === 1 ? '管理员' : '普通用户'}
        </Tag>
      ),
    },
    {
      title: '状态',
      dataIndex: 'userStatus',
      key: 'userStatus',
      render: (status: number) => (
        <Tag color={status === 0 ? 'green' : 'red'}>
          {status === 0 ? '正常' : '封禁'}
        </Tag>
      ),
    },
    {
      title: '标签',
      dataIndex: 'tags',
      key: 'tags',
      render: (tags: string) => {
        if (!tags) return '-';
        try {
          const tagList = JSON.parse(tags);
          return (
            <div style={{ display: 'flex', flexWrap: 'wrap', gap: 4 }}>
              {tagList.map((tag: string, index: number) => (
                <Tag key={index} color="blue" style={{ marginBottom: 0 }}>
                  {tag}
                </Tag>
              ))}
            </div>
          );
        } catch {
          return '-';
        }
      },
    },
    {
      title: '创建时间',
      dataIndex: 'createTime',
      key: 'createTime',
      width: 180,
      render: (time: string) => new Date(time).toLocaleString(),
    },
    {
      title: '操作',
      key: 'action',
      width: 120,
      fixed: 'right' as const,
      render: (_: any, record: User) => (
        <Button
          danger
          icon={<DeleteOutlined />}
          size="small"
          onClick={() => handleDelete(record)}
        >
          删除
        </Button>
      ),
    },
  ];

  return (
    <Card title="用户管理（管理员）">
      <Space direction="vertical" style={{ width: '100%' }} size="large">
        <Search
          placeholder="按用户名搜索"
          allowClear
          enterButton={<SearchOutlined />}
          size="large"
          onSearch={handleSearch}
          style={{ width: 400 }}
        />

        <Table
          dataSource={users}
          columns={columns}
          rowKey="id"
          loading={loading}
          pagination={{
            pageSize: 10,
            showSizeChanger: true,
            showQuickJumper: true,
            showTotal: (total) => `共 ${total} 个用户`,
          }}
          scroll={{ x: 1200 }}
        />
      </Space>
    </Card>
  );
};

export default AdminUserManagementPage;
