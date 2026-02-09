import React, { useState, useEffect } from 'react';
import { Card, Form, Input, Button, message, Select, Upload, Tag, Space } from 'antd';
import { UserOutlined, MailOutlined, PhoneOutlined, TagOutlined, PlusOutlined } from '@ant-design/icons';
import { useModel } from '@umijs/max';
import type { User } from '@/types';
import { Gender } from '@/types';
import { UploadOutlined } from '@ant-design/icons';
import { userServices } from '@/services/user';

const { TextArea } = Input;
const { Option } = Select;

const ProfilePage: React.FC = () => {
  const { initialState } = useModel('@@initialState');
  const { currentUser, setInitialState } = initialState || {};
  const [form] = Form.useForm();
  const [loading, setLoading] = useState(false);
  const [tags, setTags] = useState<string[]>([]);
  const [inputVisible, setInputVisible] = useState(false);
  const [inputValue, setInputValue] = useState('');
  const [tagLoading, setTagLoading] = useState(false);

  useEffect(() => {
    if (currentUser) {
      form.setFieldsValue({
        username: currentUser.username,
        userAccount: currentUser.userAccount,
        avatarUrl: currentUser.avatarUrl,
        gender: currentUser.gender,
        phone: currentUser.phone,
        email: currentUser.email,
        planetCode: currentUser.planetCode,
      });

      // Parse tags from JSON string
      if (currentUser.tags) {
        try {
          const parsedTags = JSON.parse(currentUser.tags);
          setTags(Array.isArray(parsedTags) ? parsedTags : []);
        } catch {
          setTags([]);
        }
      }
    }
  }, [currentUser, form]);

  const handleUpdate = async (values: Partial<User>) => {
    setLoading(true);
    try {
      const res = await userServices.updateUser({
        id: currentUser?.id,
        ...values,
      });

      if (res.code === 0) {
        message.success('更新成功！');
        // Refresh current user data
        const updatedUser = await userServices.getCurrentUser();
        if (updatedUser.code === 0 && updatedUser.data) {
          setInitialState({ currentUser: updatedUser.data });
        }
      }
    } catch (error) {
      message.error('更新失败');
    } finally {
      setLoading(false);
    }
  };

  // Tag editing functions
  const handleClose = (removedTag: string) => {
    const newTags = tags.filter(tag => tag !== removedTag);
    setTags(newTags);
  };

  const showInput = () => {
    setInputVisible(true);
  };

  const handleInputChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    setInputValue(e.target.value);
  };

  const handleInputConfirm = async () => {
    const trimmedValue = inputValue.trim();
    if (trimmedValue && !tags.includes(trimmedValue)) {
      if (tags.length >= 10) {
        message.warning('最多只能添加10个标签');
        setInputVisible(false);
        setInputValue('');
        return;
      }
      const newTags = [...tags, trimmedValue];
      setTags(newTags);
      await saveTags(newTags);
    }
    setInputVisible(false);
    setInputValue('');
  };

  const saveTags = async (newTags: string[]) => {
    setTagLoading(true);
    try {
      const res = await userServices.updateUserTags(newTags);
      if (res.code === 0) {
        message.success('标签更新成功');
        // Refresh current user data
        const updatedUser = await userServices.getCurrentUser();
        if (updatedUser.code === 0 && updatedUser.data) {
          setInitialState({ currentUser: updatedUser.data });
        }
      } else {
        message.error('标签更新失败');
      }
    } catch (error) {
      message.error('标签更新失败');
    } finally {
      setTagLoading(false);
    }
  };

  return (
    <Space direction="vertical" style={{ width: '100%' }} size="large">
      <Card title="我的标签">
        <Space direction="vertical" style={{ width: '100%' }}>
          <div style={{ marginBottom: 16, color: '#8c8c8c' }}>
            添加标签可以帮助其他开发者更好地了解你，提高推荐匹配度。最多可以添加10个标签。
          </div>
          <div>
            {tags.map((tag, index) => {
              const isLong = tag.length > 20;
              const tagElem = (
                <Tag
                  key={tag}
                  closable={!tagLoading}
                  onClose={(e) => {
                    e.preventDefault();
                    handleClose(tag);
                  }}
                  style={{ marginBottom: 8, fontSize: 14 }}
                >
                  {isLong ? `${tag.slice(0, 20)}...` : tag}
                </Tag>
              );
              return isLong ? (
                <span key={tag} style={{ display: 'inline-block' }}>
                  {tagElem}
                </span>
              ) : (
                tagElem
              );
            })}
            {inputVisible && (
              <Input
                type="text"
                size="small"
                style={{ width: 100, marginBottom: 8 }}
                value={inputValue}
                onChange={handleInputChange}
                onBlur={handleInputConfirm}
                onPressEnter={handleInputConfirm}
                disabled={tagLoading}
              />
            )}
            {!inputVisible && tags.length < 10 && (
              <Tag
                onClick={showInput}
                style={{ background: '#fff', borderStyle: 'dashed', marginBottom: 8 }}
              >
                <PlusOutlined /> 添加标签
              </Tag>
            )}
          </div>
          {tagLoading && <div style={{ color: '#1890ff' }}>保存中...</div>}
        </Space>
      </Card>

      <Card title="个人资料">
        <Form
          form={form}
          layout="vertical"
          onFinish={handleUpdate}
        >
          <Form.Item
            label="用户名"
            name="username"
            rules={[{ required: true, message: '请输入用户名' }]}
          >
            <Input prefix={<UserOutlined />} placeholder="用户名" />
          </Form.Item>

        <Form.Item
          label="账号"
          name="userAccount"
        >
          <Input disabled prefix={<UserOutlined />} />
        </Form.Item>

        <Form.Item
          label="头像链接"
          name="avatarUrl"
        >
          <Input placeholder="https://example.com/avatar.jpg" />
        </Form.Item>

        <Form.Item
          label="性别"
          name="gender"
        >
          <Select placeholder="请选择性别">
            <Option value={Gender.MALE}>男</Option>
            <Option value={Gender.FEMALE}>女</Option>
            <Option value={Gender.SECRET}>保密</Option>
          </Select>
        </Form.Item>

        <Form.Item
          label="电话"
          name="phone"
          rules={[
            { pattern: /^1[3-9]\d{9}$/, message: '请输入有效的手机号码' }
          ]}
        >
          <Input prefix={<PhoneOutlined />} placeholder="手机号码" />
        </Form.Item>

        <Form.Item
          label="邮箱"
          name="email"
          rules={[{ type: 'email', message: '请输入有效的邮箱地址' }]}
        >
          <Input prefix={<MailOutlined />} placeholder="邮箱" />
        </Form.Item>

        <Form.Item
          label="星球编号"
          name="planetCode"
        >
          <Input prefix={<TagOutlined />} placeholder="星球编号" />
        </Form.Item>

        <Form.Item>
          <Button type="primary" htmlType="submit" loading={loading}>
            更新资料
          </Button>
        </Form.Item>
      </Form>
    </Card>
    </Space>
  );
};

export default ProfilePage;
