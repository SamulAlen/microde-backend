import React, { useState, useEffect, useCallback } from 'react';
import {
  Card,
  Row,
  Col,
  Pagination,
  Tag,
  Avatar,
  Empty,
  Spin,
  Select,
  Button,
  Space,
  Progress,
  message,
  Divider,
} from 'antd';
import {
  UserOutlined,
  LikeOutlined,
  CloseOutlined,
  StarFilled,
  ThunderboltFilled,
  TeamOutlined,
  SyncOutlined,
} from '@ant-design/icons';
import { userServices } from '@/services/user';
import type { RecommendationResult, PageResult, RecommendRequest } from '@/types';

const { Option } = Select;

interface RecommendationCardProps {
  result: RecommendationResult;
  onFeedback: (userId: number, type: number) => void;
}

const RecommendationCard: React.FC<RecommendationCardProps> = ({ result, onFeedback }) => {
  const getMatchTypeIcon = (matchType: string) => {
    switch (matchType) {
      case '相似匹配':
        return <StarFilled style={{ color: '#faad14' }} />;
      case '互补匹配':
        return <ThunderboltFilled style={{ color: '#52c41a' }} />;
      case '活跃用户':
        return <TeamOutlined style={{ color: '#1890ff' }} />;
      default:
        return <StarFilled style={{ color: '#1890ff' }} />;
    }
  };

  const getSimilarityColor = (similarity: number) => {
    if (similarity >= 0.8) return '#52c41a';
    if (similarity >= 0.6) return '#faad14';
    if (similarity >= 0.4) return '#1890ff';
    return '#8c8c8c';
  };

  return (
    <Col xs={24} sm={12} md={8} lg={6}>
      <Card
        hoverable
        style={{ marginBottom: 16, height: '100%', display: 'flex', flexDirection: 'column' }}
        actions={[
          <Button
            type="text"
            icon={<LikeOutlined />}
            onClick={() => onFeedback(result.userId, 1)}
            style={{ color: '#52c41a' }}
          >
            感兴趣
          </Button>,
          <Button
            type="text"
            icon={<CloseOutlined />}
            onClick={() => onFeedback(result.userId, -1)}
          >
            跳过
          </Button>,
        ]}
      >
        <div style={{ textAlign: 'center', marginBottom: 16 }}>
          <Avatar
            size={64}
            src={result.avatarUrl}
            icon={<UserOutlined />}
            style={{ backgroundColor: '#1890ff' }}
          />
        </div>
        <Card.Meta
          title={
            <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'center', gap: 8 }}>
              {result.username}
              {getMatchTypeIcon(result.matchType)}
            </div>
          }
          description={
            <div>
              <div style={{ marginBottom: 8 }}>
                <span style={{ marginRight: 8 }}>匹配度:</span>
                <Progress
                  percent={Math.round(result.similarity * 100)}
                  size="small"
                  strokeColor={getSimilarityColor(result.similarity)}
                  showInfo={false}
                  style={{ display: 'inline-block', width: 80 }}
                />
                <span style={{ marginLeft: 4, color: getSimilarityColor(result.similarity) }}>
                  {Math.round(result.similarity * 100)}%
                </span>
              </div>
              {result.reasons && result.reasons.length > 0 && (
                <div style={{ marginBottom: 8, color: '#8c8c8c', fontSize: 12 }}>
                  {result.reasons.map((reason, index) => (
                    <div key={index}>• {reason}</div>
                  ))}
                </div>
              )}
              {result.tags && result.tags.length > 0 && (
                <div style={{ marginTop: 8 }}>
                  {result.tags.map((tag, index) => (
                    <Tag key={index} color="blue" style={{ marginBottom: 4 }}>
                      {tag}
                    </Tag>
                  ))}
                </div>
              )}
              {result.profile && (
                <div style={{ marginTop: 8, color: '#8c8c8c', fontSize: 12 }}>
                  {result.profile}
                </div>
              )}
            </div>
          }
        />
      </Card>
    </Col>
  );
};

const SmartRecommendPage: React.FC = () => {
  const [loading, setLoading] = useState(false);
  const [recommendations, setRecommendations] = useState<RecommendationResult[]>([]);
  const [pagination, setPagination] = useState({
    current: 1,
    pageSize: 12,
    total: 0,
  });
  const [filters, setFilters] = useState<Partial<RecommendRequest>>({
    strategy: 'all',
    preferredTags: [],
    minSimilarity: 30,
  });

  const fetchRecommendations = async (page: number = 1, pageSize: number = 12, currentFilters?: Partial<RecommendRequest>) => {
    setLoading(true);
    try {
      const res = await userServices.smartRecommend({
        ...(currentFilters || filters),
        pageNum: page,
        pageSize,
      });

      if (res.code === 0 && res.data) {
        const pageResult = res.data as PageResult<RecommendationResult>;
        setRecommendations(pageResult.records || []);
        setPagination({
          current: pageResult.current || page,
          pageSize: pageResult.size || pageSize,
          total: pageResult.total || 0,
        });
      }
    } catch (error) {
      console.error('Failed to fetch recommendations:', error);
      message.error('获取推荐失败');
    } finally {
      setLoading(false);
    }
  };

  // 初始加载
  useEffect(() => {
    fetchRecommendations(1, pagination.pageSize);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []); // 只在组件挂载时执行一次

  const handlePageChange = (page: number, pageSize: number) => {
    setPagination(prev => ({ ...prev, current: page, pageSize: pageSize }));
    fetchRecommendations(page, pageSize);
  };

  const handleFeedback = async (userId: number, type: number) => {
    try {
      await userServices.recommendFeedback(userId, type);
      message.success(type === 1 ? '已记录感兴趣' : '已跳过');
      // 刷新推荐 - 使用当前页码和筛选条件
      fetchRecommendations(pagination.current, pagination.pageSize);
    } catch (error) {
      console.error('Failed to record feedback:', error);
      message.error('操作失败');
    }
  };

  const handleStrategyChange = (strategy: string) => {
    const newFilters = { ...filters, strategy };
    setFilters(newFilters);
    setPagination(prev => ({ ...prev, current: 1 }));
    fetchRecommendations(1, pagination.pageSize, newFilters);
  };

  const handleMinSimilarityChange = (value: number) => {
    const newFilters = { ...filters, minSimilarity: value };
    setFilters(newFilters);
    setPagination(prev => ({ ...prev, current: 1 }));
    fetchRecommendations(1, pagination.pageSize, newFilters);
  };

  const handleRefresh = async () => {
    setLoading(true);
    try {
      await userServices.refreshRecommendations({
        userId: filters.userId,
        strategy: filters.strategy || 'all',
        preferredTags: filters.preferredTags,
      });
      message.success('已换一批推荐');
      // 重新获取推荐结果
      fetchRecommendations(1, pagination.pageSize);
    } catch (error) {
      console.error('换一批失败:', error);
      message.error('换一批失败');
    } finally {
      setLoading(false);
    }
  };

  return (
    <Card
      title="智能伙伴推荐"
      extra={
        <Button
          type="primary"
          icon={<SyncOutlined />}
          onClick={handleRefresh}
          loading={loading}
        >
          换一批
        </Button>
      }
    >
      <Space direction="vertical" style={{ width: '100%', marginBottom: 16 }} size="large">
        <Card size="small" title="推荐筛选">
          <Space wrap>
            <div>
              <span style={{ marginRight: 8 }}>推荐策略:</span>
              <Select
                value={filters.strategy}
                onChange={handleStrategyChange}
                style={{ width: 150 }}
              >
                <Option value="all">综合推荐</Option>
                <Option value="skill">技能相似</Option>
                <Option value="complement">技能互补</Option>
                <Option value="activity">活跃用户</Option>
              </Select>
            </div>
            <div>
              <span style={{ marginRight: 8 }}>最小匹配度:</span>
              <Select
                value={filters.minSimilarity}
                onChange={handleMinSimilarityChange}
                style={{ width: 120 }}
              >
                <Option value={0}>不限</Option>
                <Option value={30}>30%+</Option>
                <Option value={50}>50%+</Option>
                <Option value={70}>70%+</Option>
              </Select>
            </div>
          </Space>
        </Card>

        <Divider />

        <Spin spinning={loading}>
          {recommendations.length === 0 && !loading ? (
            <Empty description="未找到合适的推荐" />
          ) : (
            <>
              <Row gutter={[16, 16]}>
                {recommendations.map((result) => (
                  <RecommendationCard
                    key={result.userId}
                    result={result}
                    onFeedback={handleFeedback}
                  />
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
                  showTotal={(total) => `共 ${total} 个推荐`}
                />
              </div>
            </>
          )}
        </Spin>
      </Space>
    </Card>
  );
};

export default SmartRecommendPage;
