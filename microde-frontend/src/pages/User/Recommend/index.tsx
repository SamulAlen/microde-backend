import React, { useState, useEffect, useCallback } from 'react';
import { Card, Row, Col, Pagination, Tag, Avatar, Empty, Spin } from 'antd';
import { UserOutlined } from '@ant-design/icons';
import { userServices } from '@/services/user';
import type { User, PageResult } from '@/types';

const UserCard: React.FC<{ user: User }> = ({ user }) => {
  const tags = user.tags ? JSON.parse(user.tags) : [];

  return (
    <Col xs={24} sm={12} md={8} lg={6}>
      <Card
        hoverable
        style={{ marginBottom: 16 }}
      >
        <div style={{ textAlign: 'center', marginBottom: 16 }}>
          <Avatar
            size={64}
            src={user.avatarUrl}
            icon={<UserOutlined />}
            style={{ backgroundColor: '#1890ff' }}
          />
        </div>
        <Card.Meta
          title={user.username || user.userAccount}
          description={
            <div>
              <div>星球: {user.planetCode || '无'}</div>
              {tags.length > 0 && (
                <div style={{ marginTop: 8 }}>
                  {tags.map((tag: string, index: number) => (
                    <Tag key={index} color="blue" style={{ marginBottom: 4 }}>
                      {tag}
                    </Tag>
                  ))}
                </div>
              )}
            </div>
          }
        />
      </Card>
    </Col>
  );
};

const RecommendPage: React.FC = () => {
  const [loading, setLoading] = useState(false);
  const [users, setUsers] = useState<User[]>([]);
  const [pagination, setPagination] = useState({
    current: 1,
    pageSize: 12,
    total: 0,
  });

  const fetchUsers = async (page: number, pageSize: number) => {
    setLoading(true);
    try {
      const res = await userServices.recommendUsers({
        pageNum: page,
        pageSize: pageSize,
      });

      if (res.code === 0 && res.data) {
        const pageResult = res.data as PageResult<User>;
        setUsers(pageResult.records || []);
        setPagination({
          current: pageResult.current || page,
          pageSize: pageResult.size || pageSize,
          total: pageResult.total || 0,
        });
      }
    } catch (error) {
      console.error('Failed to fetch recommended users:', error);
    } finally {
      setLoading(false);
    }
  };

  // 初始加载
  useEffect(() => {
    fetchUsers(1, pagination.pageSize);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []); // 只在组件挂载时执行一次

  const handlePageChange = (page: number, pageSize: number) => {
    setPagination(prev => ({ ...prev, current: page, pageSize: pageSize }));
    fetchUsers(page, pageSize);
  };

  return (
    <Card title="推荐用户">
      <Spin spinning={loading}>
        {users.length === 0 && !loading ? (
          <Empty description="未找到用户" />
        ) : (
          <>
            <Row gutter={[16, 16]}>
              {users.map((user) => (
                <UserCard key={user.id} user={user} />
              ))}
            </Row>
            <div style={{ marginTop: 24, textAlign: 'center' }}>
              <Pagination
                current={pagination.current}
                pageSize={pagination.pageSize}
                total={pagination.total}
                onChange={handlePageChange}
                showSizeChanger
                showQuickJumper
                showTotal={(total) => `共 ${total} 个用户`}
              />
            </div>
          </>
        )}
      </Spin>
    </Card>
  );
};

export default RecommendPage;
