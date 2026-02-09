import React, { useState } from 'react';
import { Card, Form, Input, InputNumber, Select, Button, DatePicker, message } from 'antd';
import { history, useModel } from '@umijs/max';
import { teamServices } from '@/services/team';
import type { TeamAddRequest, TeamStatus } from '@/types';

const { TextArea } = Input;
const { Option } = Select;

const CreateTeamPage: React.FC = () => {
  const [loading, setLoading] = useState(false);
  const [form] = Form.useForm();
  const [teamStatus, setTeamStatus] = useState<number>(0);
  const { initialState } = useModel('@@initialState');
  const currentUser = initialState?.currentUser;

  const onFinish = async (values: any) => {
    setLoading(true);
    try {

      const teamData: TeamAddRequest = {
        name: values.name,
        description: values.description,
        maxNum: values.maxNum,
        expireTime: values.expireTime ? values.expireTime.toISOString() : undefined,
        status: values.status,
        password: values.status === 2 ? values.password : undefined,
      };

      const res = await teamServices.createTeam(teamData);

      if (res.code === 0) {
        message.success('创建成功！');
        history.push('/team/list');
      }
    } catch (error) {
      message.error('创建失败');
    } finally {
      setLoading(false);
    }
  };

  return (
    <Card title="创建队伍">
      <Form
        form={form}
        layout="vertical"
        onFinish={onFinish}
        initialValues={{
          maxNum: 10,
          status: 0,
        }}
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
          <Select onChange={(value) => setTeamStatus(value)}>
            <Option value={0}>公开</Option>
            <Option value={1}>私密</Option>
            <Option value={2}>加密</Option>
          </Select>
        </Form.Item>

        {teamStatus === 2 && (
          <Form.Item
            label="密码"
            name="password"
            rules={[{ required: true, message: '请输入加密队伍的密码' }]}
          >
            <Input.Password placeholder="请输入队伍密码" />
          </Form.Item>
        )}

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
            创建队伍
          </Button>
          <Button style={{ marginLeft: 8 }} onClick={() => history.back()}>
            取消
          </Button>
        </Form.Item>
      </Form>
    </Card>
  );
};

export default CreateTeamPage;
