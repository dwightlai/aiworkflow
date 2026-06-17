import { ReloadOutlined, SaveOutlined } from '@ant-design/icons';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { Alert, Button, Card, Form, Input, Space, Switch, Typography, message } from 'antd';
import type React from 'react';
import { useEffect } from 'react';
import {
  getStorageSettings,
  resetStorageSettings,
  saveStorageSettings,
  type SaveStorageSettingsRequest
} from '../../api/storageSettings';

export function StorageSettingsPage() {
  const [form] = Form.useForm<SaveStorageSettingsRequest>();
  const queryClient = useQueryClient();

  const settingsQuery = useQuery({
    queryKey: ['system', 'storage-settings'],
    queryFn: getStorageSettings
  });

  useEffect(() => {
    if (settingsQuery.data) {
      form.setFieldsValue(settingsQuery.data);
    }
  }, [form, settingsQuery.data]);

  const saveMutation = useMutation({
    mutationFn: (values: SaveStorageSettingsRequest) => saveStorageSettings(values),
    onSuccess: async () => {
      message.success('存储路径已保存');
      await queryClient.invalidateQueries({ queryKey: ['system', 'storage-settings'] });
    },
    onError: (error: Error) => message.error(error.message || '保存失败')
  });

  const resetMutation = useMutation({
    mutationFn: resetStorageSettings,
    onSuccess: async (settings) => {
      form.setFieldsValue(settings);
      message.success('已恢复为 application.yml 默认配置');
      await queryClient.invalidateQueries({ queryKey: ['system', 'storage-settings'] });
    },
    onError: (error: Error) => message.error(error.message || '恢复失败')
  });

  const settings = settingsQuery.data;

  return (
    <section style={pageStyle}>
      <Card variant="borderless" title="存储路径配置" loading={settingsQuery.isLoading}>
        <Typography.Paragraph type="secondary">
          配置知识库原文、编研成果 DOCX、编研母版 DOCX 的本地存储目录。保存后立即生效，无需重启。
        </Typography.Paragraph>
        {settings?.customized ? (
          <Alert type="info" showIcon message="当前使用界面自定义配置" style={{ marginBottom: 16 }} />
        ) : (
          <Alert type="success" showIcon message="当前使用 application.yml 默认配置" style={{ marginBottom: 16 }} />
        )}
        <Form form={form} layout="vertical" onFinish={(values) => saveMutation.mutate(values)}>
          <Form.Item
            name="knowledgeDocumentDir"
            label="知识库文档上传目录"
            rules={[{ required: true, message: '请输入目录路径' }]}
          >
            <Input placeholder="./data/knowledge-documents" />
          </Form.Item>
          <Form.Item name="knowledgeDocumentSaveOriginal" label="保留知识库上传原文" valuePropName="checked">
            <Switch />
          </Form.Item>
          <Form.Item
            name="researchOutputDir"
            label="编研成果 DOCX 目录"
            rules={[{ required: true, message: '请输入目录路径' }]}
          >
            <Input placeholder="./data/generation-outputs" />
          </Form.Item>
          <Form.Item
            name="researchDocxMasterDir"
            label="编研 DOCX 母版目录"
            rules={[{ required: true, message: '请输入目录路径' }]}
          >
            <Input placeholder="./data/generation-docx-masters" />
          </Form.Item>
          <Space>
            <Button type="primary" icon={<SaveOutlined />} htmlType="submit" loading={saveMutation.isPending}>
              保存
            </Button>
            <Button icon={<ReloadOutlined />} loading={resetMutation.isPending} onClick={() => resetMutation.mutate()}>
              恢复默认
            </Button>
          </Space>
        </Form>
      </Card>
    </section>
  );
}

const pageStyle: React.CSSProperties = {
  background: '#f5f7fb',
  minHeight: '100%',
  padding: 24
};
