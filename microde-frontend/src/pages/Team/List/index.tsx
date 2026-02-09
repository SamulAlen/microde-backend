import React, { useState, useEffect } from 'react';
import { Card, Row, Col, Pagination, Tag, Badge, Button, Input, Select, Space, message, Radio } from 'antd';
import { PlusOutlined, TeamOutlined, LockOutlined, EyeInvisibleOutlined, AppstoreOutlined, UserOutlined } from '@ant-design/icons';
import { history, useModel } from '@umijs/max';
import { teamServices } from '@/services/team';
import type { Team, PageResult, TeamStatus } from '@/types';

const { Search } = Input;
const { Option } = Select;

type TeamViewType = 'public' | 'my';

const TeamCard: React.FC<{ team: Team; memberCount: number; onJoin: (team: Team) => void; onEdit: (teamId: number) => void; onDelete: (teamId: number) => void; canEdit: boolean }> = ({
  team,
  memberCount,
  onJoin,
  onEdit,
  onDelete,
  canEdit,
}) => {
  const getStatusBadge = (status: number) => {
    const statusMap = {
      0: { text: '公开', color: 'green' },
      1: { text: '私密', color: 'orange' },
      2: { text: '加密', color: 'red' },
    };
    const statusInfo = statusMap[status as keyof typeof statusMap] || { text: '未知', color: 'default' };
    return <Badge color={statusInfo.color} text={statusInfo.text} />;
  };

  const getStatusIcon = (status: number) => {
    switch (status) {
      case 1:
        return <LockOutlined />;
      case 2:
        return <EyeInvisibleOutlined />;
      default:
        return <TeamOutlined />;
    }
  };

  return (
    <Col xs={24} sm={12} md={8} lg={6}>
      <Card
        hoverable
        style={{ marginBottom: 16, height: '100%' }}
        actions={[
          <Button type="primary" size="small" onClick={() => history.push(`/team/detail/${team.id}`)}>
            查看
          </Button>,
          canEdit && (
            <Button size="small" onClick={() => onEdit(team.id)}>
              编辑
            </Button>
          ),
          canEdit && (
            <Button danger size="small" onClick={() => onDelete(team.id)}>
              删除
            </Button>
          ),
        ].filter(Boolean)}
      >
        <Card.Meta
          avatar={getStatusIcon(team.status)}
          title={team.name}
          description={
            <div>
              <div>{team.description || '暂无描述'}</div>
              <div style={{ marginTop: 8 }}>
                {getStatusBadge(team.status)}
              </div>
              <div style={{ marginTop: 8 }}>
                <Tag>当前人数: {memberCount}/{team.maxNum}</Tag>
                {team.expireTime && (
                  <Tag>过期时间: {new Date(team.expireTime).toLocaleDateString()}</Tag>
                )}
              </div>
            </div>
          }
        />
      </Card>
    </Col>
  );
};

const TeamListPage: React.FC = () => {
  const [loading, setLoading] = useState(false);
  const [teams, setTeams] = useState<Team[]>([]);
  const [myTeams, setMyTeams] = useState<Team[]>([]);
  const [memberCounts, setMemberCounts] = useState<Record<number, number>>({});
  const [myTeamMemberCounts, setMyTeamMemberCounts] = useState<Record<number, number>>({});
  const [viewType, setViewType] = useState<TeamViewType>('public');
  const [pagination, setPagination] = useState({
    current: 1,
    pageSize: 12,
    total: 0,
  });
  const [filters, setFilters] = useState({
    name: '',
    status: undefined as number | undefined,
  });

  const { initialState } = useModel('@@initialState');
  const currentUser = initialState?.currentUser as { id: number; userRole?: number } | undefined;

  // 检查是否为管理员
  const isAdmin = currentUser?.userRole === 1;

  const fetchTeamMemberCounts = async (teamList: Team[]) => {
    const counts: Record<number, number> = {};
    await Promise.all(
      teamList.map(async (team) => {
        try {
          const res = await teamServices.getTeamMembers(team.id);
          if (res.code === 0 && res.data) {
            counts[team.id] = res.data.length;
          } else {
            counts[team.id] = 0;
          }
        } catch (error) {
          counts[team.id] = 0;
        }
      })
    );
    setMemberCounts(counts);
  };

  const fetchMyTeamMemberCounts = async (teamList: Team[]) => {
    const counts: Record<number, number> = {};
    await Promise.all(
      teamList.map(async (team) => {
        try {
          const res = await teamServices.getTeamMembers(team.id);
          if (res.code === 0 && res.data) {
            counts[team.id] = res.data.length;
          } else {
            counts[team.id] = 0;
          }
        } catch (error) {
          counts[team.id] = 0;
        }
      })
    );
    setMyTeamMemberCounts(counts);
  };

  // 获取我的队伍（创建的 + 加入的）
  const fetchMyTeams = async () => {
    setLoading(true);
    try {
      // 获取所有队伍（不分页，因为需要在客户端过滤）
      const res = await teamServices.listTeams({
        pageNum: 1,
        pageSize: 1000, // 获取足够多的数据
      });

      if (res.code === 0 && res.data) {
        let teams = res.data || [];

        // 过滤：只保留自己创建的或已加入的队伍
        teams = teams.filter(team => {
          // 自己创建的队伍
          if (team.userId === currentUser?.id) {
            return true;
          }
          // 非管理员用户过滤掉私密队伍
          if (!isAdmin && team.status === 1) {
            return false;
          }
          return true; // 其他队伍需要检查是否已加入（通过成员数量判断）
        });

        // 获取每个队伍的成员信息，判断是否已加入
        const teamsWithMembership = await Promise.all(
          teams.map(async (team) => {
            try {
              const memberRes = await teamServices.getTeamMembers(team.id);
              if (memberRes.code === 0 && memberRes.data) {
                const hasJoined = memberRes.data.some(member => member.id === currentUser?.id);
                return { ...team, hasJoined, members: memberRes.data };
              }
            } catch (error) {
              // 忽略错误
            }
            return { ...team, hasJoined: false, members: [] };
          })
        );

        // 只保留已加入的队伍
        const myTeams = teamsWithMembership.filter(team => team.userId === currentUser?.id || team.hasJoined);

        setMyTeams(myTeams);
        fetchMyTeamMemberCounts(myTeams);
      }
    } catch (error) {
      console.error('Failed to fetch my teams:', error);
      message.error('加载我的队伍失败');
    } finally {
      setLoading(false);
    }
  };

  const fetchTeams = async (page: number, pageSize: number) => {
    setLoading(true);
    try {
      const res = await teamServices.listTeamsByPage({
        pageNum: page,
        pageSize: pageSize,
        name: filters.name || undefined,
        status: filters.status,
      });

      if (res.code === 0 && res.data) {
        const pageResult = res.data as PageResult<Team>;
        let teams = pageResult.records || [];

        // 非管理员用户过滤掉私密队伍（status = 1）
        if (!isAdmin) {
          teams = teams.filter(team => team.status !== 1);
        }

        // 对于公共队伍视图，过滤掉自己创建的和已加入的队伍
        if (viewType === 'public') {
          // 获取已加入的队伍信息
          const teamsWithMembership = await Promise.all(
            teams.map(async (team) => {
              try {
                const memberRes = await teamServices.getTeamMembers(team.id);
                if (memberRes.code === 0 && memberRes.data) {
                  const hasJoined = memberRes.data.some(member => member.id === currentUser?.id);
                  return { ...team, hasJoined };
                }
              } catch (error) {
                // 忽略错误
              }
              return { ...team, hasJoined: false };
            })
          );

          // 过滤掉自己创建的和已加入的
          teams = teamsWithMembership.filter(
            team => team.userId !== currentUser?.id && !team.hasJoined
          );
        }

        setTeams(teams);
        setPagination({
          current: pageResult.current || page,
          pageSize: pageResult.size || pageSize,
          total: pageResult.total || 0,
        });

        // 获取队伍成员数量
        fetchTeamMemberCounts(teams);
      }
    } catch (error) {
      console.error('Failed to fetch teams:', error);
      message.error('加载队伍失败');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    if (viewType === 'public') {
      fetchTeams(1, pagination.pageSize);
    } else {
      fetchMyTeams();
    }
  }, [filters, viewType]);

  const handlePageChange = (page: number, pageSize: number) => {
    fetchTeams(page, pageSize);
  };

  const handleCreateTeam = () => {
    history.push('/team/create');
  };

  const handleEdit = (teamId: number) => {
    history.push(`/team/edit/${teamId}`);
  };

  const handleDelete = async (teamId: number) => {
    try {
      const res = await teamServices.deleteTeam(teamId);
      if (res.code === 0) {
        message.success('删除成功');
        if (viewType === 'public') {
          fetchTeams(pagination.current, pagination.pageSize);
        } else {
          fetchMyTeams();
        }
      }
    } catch (error) {
      message.error('删除失败');
    }
  };

  const canEditTeam = (team: Team) => {
    return currentUser && currentUser.id === team.userId;
  };

  const handleViewTypeChange = (e: any) => {
    setViewType(e.target.value);
  };

  // 根据视图类型选择要显示的队伍
  const displayTeams = viewType === 'public' ? teams : myTeams;
  const displayMemberCounts = viewType === 'public' ? memberCounts : myTeamMemberCounts;

  return (
    <Card
      title={viewType === 'public' ? '公共队伍' : '我的队伍'}
      extra={
        <Button type="primary" icon={<PlusOutlined />} onClick={handleCreateTeam}>
          创建队伍
        </Button>
      }
    >
      <Space direction="vertical" style={{ width: '100%', marginBottom: 16 }}>
        <Radio.Group value={viewType} onChange={handleViewTypeChange} buttonStyle="solid">
          <Radio.Button value="public"><AppstoreOutlined /> 公共队伍</Radio.Button>
          <Radio.Button value="my"><UserOutlined /> 我的队伍</Radio.Button>
        </Radio.Group>

        {viewType === 'public' && (
          <>
            <Search
              placeholder="按队伍名称搜索"
              allowClear
              enterButton
              onSearch={(value) => setFilters({ ...filters, name: value })}
              style={{ width: 300 }}
            />
            <Select
              placeholder="按状态筛选"
              allowClear
              style={{ width: 200 }}
              onChange={(value) => setFilters({ ...filters, status: value })}
            >
              <Option value={0}>公开</Option>
              {isAdmin && <Option value={1}>私密</Option>}
              <Option value={2}>加密</Option>
            </Select>
          </>
        )}
      </Space>

      {displayTeams.length === 0 && !loading ? (
        <div style={{ textAlign: 'center', padding: 40 }}>
          {viewType === 'public' ? '暂无公共队伍' : '暂无我的队伍'}
        </div>
      ) : (
        <>
          <Row gutter={[16, 16]}>
            {displayTeams.map((team) => (
              <TeamCard
                key={team.id}
                team={team}
                memberCount={displayMemberCounts[team.id] || 0}
                onJoin={() => {}}
                onEdit={handleEdit}
                onDelete={handleDelete}
                canEdit={canEditTeam(team)}
              />
            ))}
          </Row>

          {viewType === 'public' && (
            <div style={{ marginTop: 24, textAlign: 'center' }}>
              <Pagination
                current={pagination.current}
                pageSize={pagination.pageSize}
                total={pagination.total}
                onChange={handlePageChange}
                showSizeChanger
                showQuickJumper
                showTotal={(total) => `共 ${total} 个队伍`}
              />
            </div>
          )}
        </>
      )}
    </Card>
  );
};

export default TeamListPage;
