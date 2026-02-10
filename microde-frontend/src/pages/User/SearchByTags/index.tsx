import React, { useState, useRef, useCallback } from 'react';
import { Card, Row, Col, Pagination, Tag, Avatar, Empty, Spin, message, Button, Input } from 'antd';
import { UserOutlined, PlusOutlined } from '@ant-design/icons';
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
                    <Tag key={index} color="geekblue" style={{ marginBottom: 4 }}>
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

const SearchByTagsPage: React.FC = () => {
  const [loading, setLoading] = useState(false);
  const [users, setUsers] = useState<User[]>([]);
  const [selectedTags, setSelectedTags] = useState<string[]>([]);
  const [inputValue, setInputValue] = useState('');
  const [hasSearched, setHasSearched] = useState(false);
  const inputRef = useRef<any>(null);
  // 使用 ref 保存最新的标签列表，避免闭包陷阱
  const selectedTagsRef = useRef<string[]>([]);
  const [pagination, setPagination] = useState({
    current: 1,
    pageSize: 12,
    total: 0,
  });

  // 更新 ref 当 selectedTags 变化时
  selectedTagsRef.current = selectedTags;

  const fetchUsers = async (page: number, pageSize: number, tagNameList?: string[]) => {
    setLoading(true);
    try {
      console.log('fetchUsers called with:', { page, pageSize, tagNameList });
      const res = await userServices.searchUsersByTags({
        pageNum: page,
        pageSize: pageSize,
        tagNameList,
      });

      console.log('API response:', res);

      if (res.code === 0 && res.data) {
        const pageResult = res.data as PageResult<User>;
        console.log('Page result:', pageResult);
        setUsers(pageResult.records || []);
        setPagination({
          current: pageResult.current || page,
          pageSize: pageResult.size || pageSize,
          total: pageResult.total || 0,
        });
      } else {
        console.error('API error:', res);
        message.error(res.message || '搜索失败');
      }
    } catch (error) {
      console.error('Failed to search users by tags:', error);
      message.error('搜索失败');
    } finally {
      setLoading(false);
    }
  };

  const handleInputConfirm = () => {
    const trimmedValue = inputValue.trim();
    if (trimmedValue && !selectedTags.includes(trimmedValue)) {
      setSelectedTags([...selectedTags, trimmedValue]);
    }
    setInputValue('');
    // 移除 focus() 调用，避免干扰用户操作分页
    // inputRef.current?.focus();
  };

  const handleTagClose = (removedTag: string) => {
    const newTags = selectedTags.filter(tag => tag !== removedTag);
    setSelectedTags(newTags);
    // 如果还有标签，自动重新搜索；否则清空结果
    if (newTags.length > 0) {
      fetchUsers(1, pagination.pageSize, newTags);
    } else {
      setUsers([]);
      setHasSearched(false);
      setPagination({
        current: 1,
        pageSize: 12,
        total: 0,
      });
    }
  };

  const handleSearch = () => {
    if (selectedTags.length === 0) {
      message.warning('请至少输入一个标签');
      return;
    }
    setHasSearched(true);
    fetchUsers(1, pagination.pageSize, selectedTags);
  };

  const handleReset = () => {
    setSelectedTags([]);
    setUsers([]);
    setHasSearched(false);
    setInputValue('');
    setPagination({
      current: 1,
      pageSize: 12,
      total: 0,
    });
  };

  // 使用 useCallback 并且从 ref 读取最新的 selectedTags
  const handlePageChange = useCallback((page: number, pageSize: number) => {
    console.log('handlePageChange called with:', { page, pageSize });
    const currentTags = selectedTagsRef.current;
    console.log('currentTags from ref:', currentTags);
    fetchUsers(page, pageSize, currentTags.length > 0 ? currentTags : undefined);
  }, []);

  return (
    <Card title="按标签搜索用户">
      <div style={{ marginBottom: 16 }}>
        <div style={{ marginBottom: 8 }}>
          {selectedTags.map((tag) => (
            <Tag
              key={tag}
              closable
              onClose={(e) => {
                e.preventDefault();
                handleTagClose(tag);
              }}
              style={{ marginBottom: 4 }}
            >
              {tag}
            </Tag>
          ))}
          <Input
            ref={inputRef}
            type="text"
            size="small"
            style={{ width: 150 }}
            value={inputValue}
            onChange={(e) => setInputValue(e.target.value)}
            onPressEnter={(e) => {
              e.preventDefault();
              handleInputConfirm();
            }}
            placeholder="输入标签后按回车"
            prefix={<PlusOutlined />}
          />
        </div>
        <div>
          <Button onClick={handleSearch} style={{ marginRight: 8 }}>搜索</Button>
          <Button onClick={handleReset}>重置</Button>
        </div>
      </div>

      <Spin spinning={loading}>
        {!hasSearched ? (
          <Empty description="请输入标签后点击搜索按钮" />
        ) : users.length === 0 && !loading ? (
          <Empty description="未找到用户。请尝试使用其他标签进行搜索。" />
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

export default SearchByTagsPage;
