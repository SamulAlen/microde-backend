import React, { useState, useEffect } from 'react';
import { Card, Form, Input, InputNumber, Select, Button, DatePicker, message } from 'antd';
import { useParams, history } from '@umijs/max';
import { teamServices } from '@/services/team';
import type { Team } from '@/types';
import dayjs from 'dayjs';

const { TextArea } = Input;
const { Option } = Select;

const EditTeamPage: React.FC = () => {
  const { id } = useParams<{ id: string }>();
  const [loading, setLoading] = useState(false);
  const [fetchLoading, setFetchLoading] = useState(false);
  const [form] = Form.useForm();
  const [team, setTeam] = useState<Team | null>(null);

  const fetchTeamDetail = async () => {
    setFetchLoading(true);
    try {
      const res = await teamServices.getTeamById(Number(id));
      if (res.code === 0 && res.data) {
        const teamData = res.data;
        setTeam(teamData);
        form.setFieldsValue({
          name: teamData.name,
          description: teamData.description,
          maxNum: teamData.maxNum,
          status: teamData.status,
          expireTime: teamData.expireTime ? dayjs(teamData.expireTime) : null,
        });
      }
    } catch (error) {
      message.error('Failed to load team details');
    } finally {
      setFetchLoading(false);
    }
  };

  useEffect(() => {
    fetchTeamDetail();
  }, [id]);

  const onFinish = async (values: any) => {
    setLoading(true);
    try {
      const teamData: Team = {
        id: Number(id),
        name: values.name,
        description: values.description,
        maxNum: values.maxNum,
        expireTime: values.expireTime ? values.expireTime.toISOString() : undefined,
        status: values.status,
        password: values.status === 2 ? values.password : undefined,
        userId: team?.userId || 0,
        createTime: team?.createTime || '',
        updateTime: new Date().toISOString(),
      };

      const res = await teamServices.updateTeam(teamData);

      if (res.code === 0) {
        message.success('更新成功！');
        history.push(`/team/detail/${id}`);
      }
    } catch (error) {
      message.error('更新失败');
    } finally {
      setLoading(false);
    }
  };

  if (fetchLoading || !team) {
    return <Card loading />;
  }

  return (
    <Card title="编辑队伍">
      <Form
        form={form}
        layout="vertical"
        onFinish={onFinish}
      >
        <Form.Item
          label="队伍名称"
          name="name"
          rules={[{ required: true, message: '请输入队伍名称' }]}
        >
          <Input placeholder="请输入队伍名称" />
        </Form.Item>

        <Form.Item
          label="描述"
          name="description"
        >
          <TextArea rows={4} placeholder="请输入队伍描述" />
        </Form.Item>

        <Form.Item
          label="最大人数"
          name="maxNum"
          rules={[
            { required: true, message: '请输入最大人数' },
            { type: 'number', min: 1, max: 100, message: '最大人数必须在1到100之间' }
          ]}
        >
          <InputNumber min={1} max={100} style={{ width: '100%' }} />
        </Form.Item>

        <Form.Item
          label="队伍状态"
          name="status"
          rules={[{ required: true, message: '请选择队伍状态' }]}
        >
          <Select>
            <Option value={0}>公开</Option>
            <Option value={1}>私密</Option>
            <Option value={2}>加密</Option>
          </Select>
        </Form.Item>

        <Form.Item
          noStyle
          shouldUpdate={(prevValues, currentValues) => prevValues.status !== currentValues.status}
        >
          {({ getFieldValue }) =>
            getFieldValue('status') === 2 ? (
              <Form.Item
                label="密码"
                name="password"
                rules={[{ required: true, message: '请输入加密队伍的密码' }]}
              >
                <Input.Password placeholder="请输入队伍密码" />
              </Form.Item>
            ) : null
          }
        </Form.Item>

        <Form.Item
          label="过期时间"
          name="expireTime"
        >
          <DatePicker
            showTime
            style={{ width: '100%' }}
            placeholder="请选择过期时间"
          />
        </Form.Item>

        <Form.Item>
          <Button type="primary" htmlType="submit" loading={loading}>
            更新队伍
          </Button>
          <Button style={{ marginLeft: 8 }} onClick={() => history.back()}>
            取消
          </Button>
        </Form.Item>
      </Form>
    </Card>
  );
};

export default EditTeamPage;
