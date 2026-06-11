import { DeleteOutlined, EditOutlined, PlusOutlined, ReloadOutlined } from '@ant-design/icons';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { Button, Card, Drawer, Form, Input, InputNumber, Modal, Select, Space, Table, Tag, message } from 'antd';
import type { ColumnsType } from 'antd/es/table';
import { useMemo, useState } from 'react';
import {
  createDictionary,
  createDictionaryItem,
  deleteDictionary,
  deleteDictionaryItem,
  listDictionaries,
  listDictionaryItems,
  updateDictionary,
  updateDictionaryItem,
  type DataDictionary,
  type DataDictionaryItem,
  type SaveDictionaryItemRequest,
  type SaveDictionaryRequest,
  type UpdateDictionaryRequest
} from '../../api/system';

type DictionaryFormValues = SaveDictionaryRequest & UpdateDictionaryRequest;
type ItemFormValues = SaveDictionaryItemRequest;

export function DictionaryPage() {
  const queryClient = useQueryClient();
  const [dictForm] = Form.useForm<DictionaryFormValues>();
  const [itemForm] = Form.useForm<ItemFormValues>();
  const [dictDrawerOpen, setDictDrawerOpen] = useState(false);
  const [itemDrawerOpen, setItemDrawerOpen] = useState(false);
  const [editingDictionary, setEditingDictionary] = useState<DataDictionary | null>(null);
  const [editingItem, setEditingItem] = useState<DataDictionaryItem | null>(null);
  const [selectedDictionaryId, setSelectedDictionaryId] = useState<string | null>(null);

  const dictionariesQuery = useQuery({
    queryKey: ['system', 'dictionaries'],
    queryFn: listDictionaries
  });
  const dictionaries = dictionariesQuery.data?.items ?? [];

  const selectedDictionary = useMemo(
    () => dictionaries.find((item) => item.id === selectedDictionaryId) ?? null,
    [dictionaries, selectedDictionaryId]
  );

  const itemsQuery = useQuery({
    queryKey: ['system', 'dictionary-items', selectedDictionaryId],
    queryFn: () => listDictionaryItems(selectedDictionaryId!),
    enabled: !!selectedDictionaryId
  });
  const items = itemsQuery.data?.items ?? [];

  const saveDictMutation = useMutation({
    mutationFn: (values: DictionaryFormValues) => {
      if (editingDictionary) {
        return updateDictionary(editingDictionary.id, {
          name: values.name,
          description: values.description,
          status: values.status ?? 'ENABLED'
        });
      }
      return createDictionary({
        code: values.code,
        name: values.name,
        description: values.description,
        status: values.status ?? 'ENABLED'
      });
    },
    onSuccess: async () => {
      message.success(editingDictionary ? '字典已更新' : '字典已创建');
      setDictDrawerOpen(false);
      setEditingDictionary(null);
      dictForm.resetFields();
      await queryClient.invalidateQueries({ queryKey: ['system', 'dictionaries'] });
    }
  });

  const deleteDictMutation = useMutation({
    mutationFn: (dictionary: DataDictionary) => deleteDictionary(dictionary.id),
    onSuccess: async (_, dictionary) => {
      message.success('字典已删除');
      if (selectedDictionaryId === dictionary.id) {
        setSelectedDictionaryId(null);
      }
      await queryClient.invalidateQueries({ queryKey: ['system', 'dictionaries'] });
    }
  });

  const saveItemMutation = useMutation({
    mutationFn: (values: ItemFormValues) => {
      const payload: SaveDictionaryItemRequest = {
        label: values.label,
        value: values.value,
        description: values.description,
        sortOrder: values.sortOrder ?? 0,
        status: values.status ?? 'ENABLED'
      };
      if (editingItem) {
        return updateDictionaryItem(editingItem.id, payload);
      }
      return createDictionaryItem(selectedDictionaryId!, payload);
    },
    onSuccess: async () => {
      message.success(editingItem ? '字典项已更新' : '字典项已创建');
      setItemDrawerOpen(false);
      setEditingItem(null);
      itemForm.resetFields();
      await queryClient.invalidateQueries({ queryKey: ['system', 'dictionary-items', selectedDictionaryId] });
    }
  });

  const deleteItemMutation = useMutation({
    mutationFn: (item: DataDictionaryItem) => deleteDictionaryItem(item.id),
    onSuccess: async () => {
      message.success('字典项已删除');
      await queryClient.invalidateQueries({ queryKey: ['system', 'dictionary-items', selectedDictionaryId] });
    }
  });

  const dictColumns: ColumnsType<DataDictionary> = [
    { title: '编码', dataIndex: 'code', width: 180 },
    { title: '名称', dataIndex: 'name' },
    {
      title: '状态',
      dataIndex: 'status',
      width: 90,
      render: (value: string) => (
        <Tag color={value === 'ENABLED' ? 'green' : 'default'}>{value === 'ENABLED' ? '启用' : value}</Tag>
      )
    },
    {
      title: '操作',
      key: 'actions',
      width: 120,
      render: (_, record) => (
        <Space onClick={(event) => event.stopPropagation()}>
          <Button type="link" icon={<EditOutlined />} onClick={() => openEditDictionary(record)} />
          <Button type="link" danger icon={<DeleteOutlined />} onClick={() => confirmDeleteDictionary(record)} />
        </Space>
      )
    }
  ];

  const itemColumns: ColumnsType<DataDictionaryItem> = [
    { title: '标签', dataIndex: 'label', width: 140 },
    { title: '值', dataIndex: 'value', width: 160 },
    { title: '描述', dataIndex: 'description' },
    { title: '排序', dataIndex: 'sortOrder', width: 80 },
    {
      title: '状态',
      dataIndex: 'status',
      width: 90,
      render: (value: string) => (
        <Tag color={value === 'ENABLED' ? 'green' : 'default'}>{value === 'ENABLED' ? '启用' : value}</Tag>
      )
    },
    {
      title: '操作',
      key: 'actions',
      width: 120,
      render: (_, record) => (
        <Space>
          <Button type="link" icon={<EditOutlined />} onClick={() => openEditItem(record)} />
          <Button type="link" danger icon={<DeleteOutlined />} onClick={() => confirmDeleteItem(record)} />
        </Space>
      )
    }
  ];

  const openCreateDictionary = () => {
    setEditingDictionary(null);
    dictForm.resetFields();
    dictForm.setFieldsValue({ status: 'ENABLED' });
    setDictDrawerOpen(true);
  };

  const openEditDictionary = (dictionary: DataDictionary) => {
    setEditingDictionary(dictionary);
    dictForm.setFieldsValue({
      code: dictionary.code,
      name: dictionary.name,
      description: dictionary.description ?? undefined,
      status: dictionary.status
    });
    setDictDrawerOpen(true);
  };

  const confirmDeleteDictionary = (dictionary: DataDictionary) => {
    Modal.confirm({
      title: '删除字典',
      content: `确定删除字典「${dictionary.name}」及其全部字典项吗？`,
      okType: 'danger',
      onOk: () => deleteDictMutation.mutateAsync(dictionary)
    });
  };

  const openCreateItem = () => {
    if (!selectedDictionaryId) {
      message.warning('请先选择字典');
      return;
    }
    setEditingItem(null);
    itemForm.resetFields();
    itemForm.setFieldsValue({ sortOrder: 0, status: 'ENABLED' });
    setItemDrawerOpen(true);
  };

  const openEditItem = (item: DataDictionaryItem) => {
    setEditingItem(item);
    itemForm.setFieldsValue({
      label: item.label,
      value: item.value,
      description: item.description ?? undefined,
      sortOrder: item.sortOrder,
      status: item.status
    });
    setItemDrawerOpen(true);
  };

  const confirmDeleteItem = (item: DataDictionaryItem) => {
    Modal.confirm({
      title: '删除字典项',
      content: `确定删除字典项「${item.label}」吗？`,
      okType: 'danger',
      onOk: () => deleteItemMutation.mutateAsync(item)
    });
  };

  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: 16 }}>
      <Card>
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
          <div>
            <div style={{ fontSize: 18, fontWeight: 600 }}>数据字典</div>
            <div style={{ color: '#8c8c8c', marginTop: 4 }}>维护系统枚举与下拉选项</div>
          </div>
          <Space>
            <Button icon={<ReloadOutlined />} onClick={() => dictionariesQuery.refetch()} loading={dictionariesQuery.isFetching}>
              刷新
            </Button>
            <Button type="primary" icon={<PlusOutlined />} onClick={openCreateDictionary}>
              新建字典
            </Button>
          </Space>
        </div>
      </Card>

      <div style={{ display: 'grid', gridTemplateColumns: '1fr 1.2fr', gap: 16 }}>
        <Card title="字典列表">
          <Table
            rowKey="id"
            size="small"
            loading={dictionariesQuery.isLoading}
            columns={dictColumns}
            dataSource={dictionaries}
            pagination={false}
            rowSelection={{
              type: 'radio',
              selectedRowKeys: selectedDictionaryId ? [selectedDictionaryId] : [],
              onChange: (keys) => setSelectedDictionaryId(String(keys[0] ?? ''))
            }}
            onRow={(record) => ({
              onClick: () => setSelectedDictionaryId(record.id)
            })}
          />
        </Card>

        <Card
          title={selectedDictionary ? `字典项 · ${selectedDictionary.name}` : '字典项'}
          extra={
            <Button type="primary" size="small" icon={<PlusOutlined />} disabled={!selectedDictionaryId} onClick={openCreateItem}>
              新建项
            </Button>
          }
        >
          <Table
            rowKey="id"
            size="small"
            loading={itemsQuery.isLoading}
            columns={itemColumns}
            dataSource={items}
            pagination={false}
            locale={{ emptyText: selectedDictionaryId ? '暂无字典项' : '请选择左侧字典' }}
          />
        </Card>
      </div>

      <Drawer
        title={editingDictionary ? '编辑字典' : '新建字典'}
        width={480}
        open={dictDrawerOpen}
        onClose={() => {
          setDictDrawerOpen(false);
          setEditingDictionary(null);
          dictForm.resetFields();
        }}
        destroyOnClose
        extra={
          <Button type="primary" loading={saveDictMutation.isPending} onClick={() => dictForm.submit()}>
            保存
          </Button>
        }
      >
        <Form form={dictForm} layout="vertical" onFinish={(values) => saveDictMutation.mutate(values)}>
          <Form.Item name="code" label="编码" rules={[{ required: true, message: '请输入编码' }]}>
            <Input disabled={!!editingDictionary} placeholder="common_status" />
          </Form.Item>
          <Form.Item name="name" label="名称" rules={[{ required: true, message: '请输入名称' }]}>
            <Input />
          </Form.Item>
          <Form.Item name="description" label="描述">
            <Input.TextArea rows={3} />
          </Form.Item>
          <Form.Item name="status" label="状态">
            <Select
              options={[
                { value: 'ENABLED', label: '启用' },
                { value: 'DISABLED', label: '停用' }
              ]}
            />
          </Form.Item>
        </Form>
      </Drawer>

      <Drawer
        title={editingItem ? '编辑字典项' : '新建字典项'}
        width={480}
        open={itemDrawerOpen}
        onClose={() => {
          setItemDrawerOpen(false);
          setEditingItem(null);
          itemForm.resetFields();
        }}
        destroyOnClose
        extra={
          <Button type="primary" loading={saveItemMutation.isPending} onClick={() => itemForm.submit()}>
            保存
          </Button>
        }
      >
        <Form form={itemForm} layout="vertical" onFinish={(values) => saveItemMutation.mutate(values)}>
          <Form.Item name="label" label="标签" rules={[{ required: true, message: '请输入标签' }]}>
            <Input />
          </Form.Item>
          <Form.Item name="value" label="值" rules={[{ required: true, message: '请输入值' }]}>
            <Input />
          </Form.Item>
          <Form.Item name="description" label="描述">
            <Input.TextArea rows={3} />
          </Form.Item>
          <Form.Item name="sortOrder" label="排序">
            <InputNumber min={0} style={{ width: '100%' }} />
          </Form.Item>
          <Form.Item name="status" label="状态">
            <Select
              options={[
                { value: 'ENABLED', label: '启用' },
                { value: 'DISABLED', label: '停用' }
              ]}
            />
          </Form.Item>
        </Form>
      </Drawer>
    </div>
  );
}
