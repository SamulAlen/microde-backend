import React, { useState, useEffect } from 'react';
import { Card, Table, Input, Button, Space, Modal, message, Tag, Select, Badge } from 'antd';
import { SearchOutlined, DeleteOutlined, EyeOutlined, TeamOutlined, LockOutlined, EyeInvisibleOutlined } from '@ant-design/icons';
import { teamServices } from '@/services/team';
import type { Team, PageResult } from '@/types';
import { history } from '@umijs/max';

const { Search } = Input;
const { Option } = Select;

const AdminTeamManagementPage: React.FC = () => {
  const [loading, setLoading] = useState(false);
  const [teams, setTeams] = useState<Team[]>([]);
  const [searchText, setSearchText] = useState('');
  const [statusFilter, setStatusFilter] = useState<number | undefined>(undefined);
  const [pagination, setPagination] = useState({
    current: 1,
    pageSize: 10,
    total: 0,
  });
  const [memberCounts, setMemberCounts] = useState<Record<number, number>>({});

  const fetchTeams = async (page: number = 1, pageSize: number = 10, name?: string, status?: number) => {
    setLoading(true);
    try {
      const res = await teamServices.listTeamsByPage({
        pageNum: page,
        pageSize: pageSize,
        name: name || undefined,
        status: status,
      });
      if (res.code === 0 && res.data) {
        const pageResult = res.data as PageResult<Team>;
        setTeams(pageResult.records || []);
        setPagination({
          current: pageResult.current || page,
          pageSize: pageResult.size || pageSize,
          total: pageResult.total || 0,
        });
        // 获取队伍成员数量
        fetchTeamMemberCounts(pageResult.records || []);
      }
    } catch (error) {
      message.error('获取队伍失败');
    } finally {
      setLoading(false);
    }
  };

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

  useEffect(() => {
    fetchTeams();
  }, []);

  const handleSearch = (value: string) => {
    setSearchText(value);
    fetchTeams(1, pagination.pageSize, value || undefined, statusFilter);
  };

  const handleStatusChange = (value: number | undefined) => {
    setStatusFilter(value);
    fetchTeams(1, pagination.pageSize, searchText || undefined, value);
  };

  const handleView = (team: Team) => {
    history.push(`/team/detail/${team.id}`);
  };

  const handleDelete = (team: Team) => {
    Modal.confirm({
      title: '删除队伍',
      content: `确定要删除队伍 "${team.name}" 吗？`,
      onOk: async () => {
        try {
          const res = await teamServices.deleteTeam(team.id);
          if (res.code === 0) {
            message.success('删除成功');
            fetchTeams(pagination.current, pagination.pageSize, searchText || undefined, statusFilter);
          }
        } catch (error) {
          message.error('删除失败');
        }
      },
    });
  };

  const handleTableChange = (page: number, pageSize: number) => {
    fetchTeams(page, pageSize, searchText || undefined, statusFilter);
  };

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

  const columns = [
    {
      title: 'ID',
      dataIndex: 'id',
      key: 'id',
      width: 80,
    },
    {
      title: '队伍名称',
      dataIndex: 'name',
      key: 'name',
      width: 150,
    },
    {
      title: '描述',
      dataIndex: 'description',
      key: 'description',
      width: 200,
      ellipsis: true,
      render: (description: string) => description || '-',
    },
    {
      title: '状态',
      dataIndex: 'status',
      key: 'status',
      width: 100,
      render: (status: number) => getStatusBadge(status),
    },
    {
      title: '队长ID',
      dataIndex: 'userId',
      key: 'userId',
      width: 100,
    },
    {
      title: '人数',
      key: 'memberCount',
      width: 100,
      render: (_: any, record: Team) => (
        <span>
          {memberCounts[record.id] || 0}/{record.maxNum}
        </span>
      ),
    },
    {
      title: '密码',
      dataIndex: 'password',
      key: 'password',
      width: 100,
      render: (password: string, record: Team) => {
        if (record.status !== 2) return '-';
        return password ? '***' : '未设置';
      },
    },
    {
      title: '过期时间',
      dataIndex: 'expireTime',
      key: 'expireTime',
      width: 180,
      render: (time: string) => time ? new Date(time).toLocaleString() : '永不过期',
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
      width: 180,
      fixed: 'right' as const,
      render: (_: any, record: Team) => (
        <Space>
          <Button
            type="link"
            icon={<EyeOutlined />}
            size="small"
            onClick={() => handleView(record)}
          >
            查看
          </Button>
          <Button
            danger
            icon={<DeleteOutlined />}
            size="small"
            onClick={() => handleDelete(record)}
          >
            删除
          </Button>
        </Space>
      ),
    },
  ];

  return (
    <Card title="队伍管理（管理员）">
      <Space direction="vertical" style={{ width: '100%' }} size="large">
        <Space style={{ width: '100%' }}>
          <Search
            placeholder="按队伍名称搜索"
            allowClear
            enterButton={<SearchOutlined />}
            onSearch={handleSearch}
            style={{ width: 300 }}
          />
          <Select
            placeholder="按状态筛选"
            allowClear
            style={{ width: 150 }}
            onChange={handleStatusChange}
          >
            <Option value={0}>公开</Option>
            <Option value={1}>私密</Option>
            <Option value={2}>加密</Option>
          </Select>
        </Space>

        <Table
          dataSource={teams}
          columns={columns}
          rowKey="id"
          loading={loading}
          pagination={{
            current: pagination.current,
            pageSize: pagination.pageSize,
            total: pagination.total,
            showSizeChanger: true,
            showQuickJumper: true,
            showTotal: (total) => `共 ${total} 个队伍`,
            onChange: handleTableChange,
          }}
          scroll={{ x: 1400 }}
        />
      </Space>
    </Card>
  );
};

export default AdminTeamManagementPage;
