import React, { useState, useEffect } from 'react';
import { Card, Descriptions, Badge, Button, Tag, Modal, Input, message, Space, Avatar, List, Empty } from 'antd';
import { useParams, history, useModel } from '@umijs/max';
import { UserOutlined, TeamOutlined } from '@ant-design/icons';
import { teamServices } from '@/services/team';
import type { Team, User } from '@/types';

const TeamDetailPage: React.FC = () => {
  const { id } = useParams<{ id: string }>();
  const [loading, setLoading] = useState(false);
  const [team, setTeam] = useState<Team | null>(null);
  const [members, setMembers] = useState<User[]>([]);
  const [membersLoading, setMembersLoading] = useState(false);
  const [passwordModalVisible, setPasswordModalVisible] = useState(false);
  const [password, setPassword] = useState('');
  const [joining, setJoining] = useState(false);
  const [hasJoined, setHasJoined] = useState(false);

  const { initialState } = useModel('@@initialState');
  const currentUser = initialState?.currentUser;

  const fetchTeamDetail = async () => {
    setLoading(true);
    try {
      const res = await teamServices.getTeamById(Number(id));
      if (res.code === 0 && res.data) {
        setTeam(res.data);
        fetchTeamMembers();
      }
    } catch (error) {
      message.error('加载队伍详情失败');
    } finally {
      setLoading(false);
    }
  };

  const fetchTeamMembers = async () => {
    setMembersLoading(true);
    try {
      const res = await teamServices.getTeamMembers(Number(id));
      if (res.code === 0 && res.data) {
        setMembers(res.data);
        // 检查当前用户是否已加入队伍
        setHasJoined(res.data.some(member => member.id === currentUser?.id));
      }
    } catch (error) {
      message.error('加载队伍成员失败');
    } finally {
      setMembersLoading(false);
    }
  };

  useEffect(() => {
    fetchTeamDetail();
  }, [id]);

  const handleJoin = () => {
    if (team?.status === 2) {
      // 加密队伍 - 显示密码输入框
      setPasswordModalVisible(true);
    } else if (team?.status === 0) {
      // 公开队伍 - 直接加入
      joinTeam('');
    }
  };

  const joinTeam = async (password: string) => {
    setJoining(true);
    try {
      const res = await teamServices.joinTeam({
        teamId: Number(id),
        password,
      });
      if (res.code === 0) {
        message.success('成功加入队伍！');
        setPasswordModalVisible(false);
        setPassword('');
        fetchTeamMembers();
      } else {
        message.error(res.message || '加入队伍失败');
      }
    } catch (error) {
      message.error('加入队伍失败');
    } finally {
      setJoining(false);
    }
  };

  const handleJoinWithPassword = async () => {
    if (!password) {
      message.warning('请输入密码');
      return;
    }
    joinTeam(password);
  };

  const handleQuit = async () => {
    Modal.confirm({
      title: '退出队伍',
      content: '确定要退出这个队伍吗？',
      onOk: async () => {
        try {
          const res = await teamServices.quitTeam({ teamId: Number(id) });
          if (res.code === 0) {
            message.success('退出成功');
            fetchTeamMembers();
          } else {
            message.error(res.message || '退出失败');
          }
        } catch (error) {
          message.error('退出失败');
        }
      },
    });
  };

  const handleEdit = () => {
    history.push(`/team/edit/${id}`);
  };

  const handleDelete = async () => {
    Modal.confirm({
      title: '删除队伍',
      content: '确定要删除这个队伍吗？',
      onOk: async () => {
        try {
          const res = await teamServices.deleteTeam(Number(id));
          if (res.code === 0) {
            message.success('删除成功');
            history.push('/team/list');
          }
        } catch (error) {
          message.error('删除失败');
        }
      },
    });
  };

  const canEdit = team && currentUser && currentUser.id === team.userId;

  if (loading || !team) {
    return <Card loading />;
  }

  const getStatusBadge = (status: number) => {
    const statusMap = {
      0: { text: '公开', color: 'green' },
      1: { text: '私密', color: 'orange' },
      2: { text: '加密', color: 'red' },
    };
    const statusInfo = statusMap[status as keyof typeof statusMap] || { text: '未知', color: 'default' };
    return <Badge color={statusInfo.color} text={statusInfo.text} />;
  };

  const isTeamLeader = currentUser && currentUser.id === team.userId;

  // 从成员列表中找到创建者
  const creator = members.find(member => member.id === team.userId);

  return (
    <Card
      title={team.name}
      extra={getStatusBadge(team.status)}
    >
      <Descriptions column={2} bordered>
        <Descriptions.Item label="队伍名称" span={2}>
          {team.name}
        </Descriptions.Item>
        <Descriptions.Item label="描述" span={2}>
          {team.description || '暂无描述'}
        </Descriptions.Item>
        <Descriptions.Item label="最大人数">
          {team.maxNum}
        </Descriptions.Item>
        <Descriptions.Item label="当前人数">
          {members.length}/{team.maxNum}
        </Descriptions.Item>
        <Descriptions.Item label="状态">
          {getStatusBadge(team.status)}
        </Descriptions.Item>
        <Descriptions.Item label="过期时间">
          {team.expireTime ? new Date(team.expireTime).toLocaleString() : '永不过期'}
        </Descriptions.Item>
        <Descriptions.Item label="创建者">
          {team.userId === currentUser?.id ? '你' : (creator?.username || creator?.userAccount || '未知')}
        </Descriptions.Item>
        <Descriptions.Item label="创建时间" span={2}>
          {new Date(team.createTime).toLocaleString()}
        </Descriptions.Item>
      </Descriptions>

      <Card
        type="inner"
        title={<span><TeamOutlined /> 队伍成员</span>}
        style={{ marginTop: 24 }}
      >
        {membersLoading ? (
          <div style={{ textAlign: 'center', padding: 24 }}>加载中...</div>
        ) : members.length === 0 ? (
          <Empty description="暂无成员" />
        ) : (
          <List
            dataSource={members}
            renderItem={(member) => (
              <List.Item>
                <List.Item.Meta
                  avatar={
                    <Avatar
                      size={40}
                      src={member.avatarUrl}
                      icon={<UserOutlined />}
                      style={{ backgroundColor: '#1890ff' }}
                    />
                  }
                  title={
                    <span>
                      {member.username || member.userAccount}
                      {member.id === team.userId && (
                        <Tag color="blue" style={{ marginLeft: 8 }}>队长</Tag>
                      )}
                    </span>
                  }
                  description={
                    <span>
                      {member.email && `邮箱: ${member.email}`}
                      {member.phone && ` | 电话: ${member.phone}`}
                    </span>
                  }
                />
              </List.Item>
            )}
          />
        )}
      </Card>

      <Space style={{ marginTop: 24 }}>
        {!hasJoined && !isTeamLeader && team.status !== 1 && (
          <Button type="primary" onClick={handleJoin}>
            加入队伍
          </Button>
        )}
        {hasJoined && !isTeamLeader && (
          <Button onClick={handleQuit}>
            退出队伍
          </Button>
        )}
        {canEdit && (
          <>
            <Button onClick={handleEdit}>
              编辑队伍
            </Button>
            <Button danger onClick={handleDelete}>
              删除队伍
            </Button>
          </>
        )}
        <Button onClick={() => history.back()}>
          返回
        </Button>
      </Space>

      <Modal
        title="输入密码加入队伍"
        open={passwordModalVisible}
        onOk={handleJoinWithPassword}
        onCancel={() => {
          setPasswordModalVisible(false);
          setPassword('');
        }}
        confirmLoading={joining}
      >
        <Input.Password
          value={password}
          onChange={(e) => setPassword(e.target.value)}
          placeholder="请输入队伍密码"
        />
      </Modal>
    </Card>
  );
};

export default TeamDetailPage;
