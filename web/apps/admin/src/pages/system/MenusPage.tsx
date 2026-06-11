import { DeleteOutlined, EditOutlined, PlusOutlined, ReloadOutlined } from '@ant-design/icons';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { Button, Card, Drawer, Form, Input, InputNumber, Modal, Select, Space, Switch, Table, Tag, message } from 'antd';
import type { ColumnsType } from 'antd/es/table';
import { useState } from 'react';
import {
  createMenu,
  deleteMenu,
  listMenus,
  updateMenu,
  type SaveMenuRequest,
  type SysMenu
} from '../../api/system';

type MenuFormValues = SaveMenuRequest;

export function MenusPage() {
  const queryClient = useQueryClient();
  const [form] = Form.useForm<MenuFormValues>();
  const [drawerOpen, setDrawerOpen] = useState(false);
  const [editingMenu, setEditingMenu] = useState<SysMenu | null>(null);

  const menusQuery = useQuery({
    queryKey: ['system', 'menus'],
    queryFn: listMenus
  });
  const menus = menusQuery.data?.items ?? [];

  const saveMutation = useMutation({
    mutationFn: (values: MenuFormValues) => {
      const payload: SaveMenuRequest = {
        groupTitle: values.groupTitle,
        menuKey: values.menuKey,
        title: values.title,
        path: values.path,
        sortOrder: values.sortOrder ?? 0,
        visible: values.visible ?? true,
        platformOnly: values.platformOnly ?? false,
        status: values.status ?? 'ENABLED'
      };
      return editingMenu ? updateMenu(editingMenu.id, payload) : createMenu(payload);
    },
    onSuccess: async () => {
      message.success(editingMenu ? '菜单已更新' : '菜单已创建');
      setDrawerOpen(false);
      setEditingMenu(null);
      form.resetFields();
      await queryClient.invalidateQueries({ queryKey: ['system', 'menus'] });
      await queryClient.invalidateQueries({ queryKey: ['system', 'menu-navigation'] });
    }
  });

  const deleteMutation = useMutation({
    mutationFn: (menu: SysMenu) => deleteMenu(menu.id),
    onSuccess: async () => {
      message.success('菜单已删除');
      await queryClient.invalidateQueries({ queryKey: ['system', 'menus'] });
      await queryClient.invalidateQueries({ queryKey: ['system', 'menu-navigation'] });
    }
  });

  const openCreate = () => {
    setEditingMenu(null);
    form.resetFields();
    form.setFieldsValue({ sortOrder: 0, visible: true, platformOnly: false, status: 'ENABLED' });
    setDrawerOpen(true);
  };

  const openEdit = (menu: SysMenu) => {
    setEditingMenu(menu);
    form.setFieldsValue({
      groupTitle: menu.groupTitle,
      menuKey: menu.menuKey,
      title: menu.title,
      path: menu.path,
      sortOrder: menu.sortOrder,
      visible: menu.visible,
      platformOnly: menu.platformOnly,
      status: menu.status
    });
    setDrawerOpen(true);
  };

  const confirmDelete = (menu: SysMenu) => {
    Modal.confirm({
      title: '删除菜单',
      content: `确定删除菜单「${menu.title}」吗？`,
      okType: 'danger',
      onOk: () => deleteMutation.mutateAsync(menu)
    });
  };

  const columns: ColumnsType<SysMenu> = [
    { title: '分组', dataIndex: 'groupTitle', width: 120 },
    { title: 'Key', dataIndex: 'menuKey', width: 140 },
    { title: '标题', dataIndex: 'title', width: 160 },
    { title: '路径', dataIndex: 'path' },
    { title: '排序', dataIndex: 'sortOrder', width: 80 },
    {
      title: '可见',
      dataIndex: 'visible',
      width: 80,
      render: (value: boolean) => (value ? '是' : '否')
    },
    {
      title: '平台专属',
      dataIndex: 'platformOnly',
      width: 100,
      render: (value: boolean) => (value ? <Tag color="purple">是</Tag> : '否')
    },
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
          <Button type="link" icon={<EditOutlined />} onClick={() => openEdit(record)} />
          <Button type="link" danger icon={<DeleteOutlined />} onClick={() => confirmDelete(record)} />
        </Space>
      )
    }
  ];

  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: 16 }}>
      <Card>
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
          <div>
            <div style={{ fontSize: 18, fontWeight: 600 }}>菜单管理</div>
            <div style={{ color: '#8c8c8c', marginTop: 4 }}>配置侧栏导航分组、路径与可见性</div>
          </div>
          <Space>
            <Button icon={<ReloadOutlined />} onClick={() => menusQuery.refetch()} loading={menusQuery.isFetching}>
              刷新
            </Button>
            <Button type="primary" icon={<PlusOutlined />} onClick={openCreate}>
              新建菜单
            </Button>
          </Space>
        </div>
      </Card>

      <Card>
        <Table
          rowKey="id"
          loading={menusQuery.isLoading}
          columns={columns}
          dataSource={menus}
          pagination={false}
          scroll={{ x: 960 }}
        />
      </Card>

      <Drawer
        title={editingMenu ? '编辑菜单' : '新建菜单'}
        width={480}
        open={drawerOpen}
        onClose={() => {
          setDrawerOpen(false);
          setEditingMenu(null);
          form.resetFields();
        }}
        destroyOnClose
        extra={
          <Button type="primary" loading={saveMutation.isPending} onClick={() => form.submit()}>
            保存
          </Button>
        }
      >
        <Form form={form} layout="vertical" onFinish={(values) => saveMutation.mutate(values)}>
          <Form.Item name="groupTitle" label="分组标题" rules={[{ required: true, message: '请输入分组标题' }]}>
            <Input placeholder="如：系统管理" />
          </Form.Item>
          <Form.Item name="menuKey" label="菜单 Key" rules={[{ required: true, message: '请输入菜单 Key' }]}>
            <Input placeholder="如：menus" disabled={!!editingMenu} />
          </Form.Item>
          <Form.Item name="title" label="显示标题" rules={[{ required: true, message: '请输入显示标题' }]}>
            <Input />
          </Form.Item>
          <Form.Item name="path" label="路由路径" rules={[{ required: true, message: '请输入路由路径' }]}>
            <Input placeholder="/system/menus" />
          </Form.Item>
          <Form.Item name="sortOrder" label="排序">
            <InputNumber min={0} style={{ width: '100%' }} />
          </Form.Item>
          <Form.Item name="visible" label="侧栏可见" valuePropName="checked">
            <Switch />
          </Form.Item>
          <Form.Item name="platformOnly" label="仅平台管理员" valuePropName="checked">
            <Switch />
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
