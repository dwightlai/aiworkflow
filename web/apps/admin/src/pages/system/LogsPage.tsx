import { ReloadOutlined, SearchOutlined } from '@ant-design/icons';
import { useQuery } from '@tanstack/react-query';
import { Button, Card, DatePicker, Input, Select, Space, Table, Tag } from 'antd';
import type { ColumnsType } from 'antd/es/table';
import type { Dayjs } from 'dayjs';
import { useMemo, useState } from 'react';
import { listAuditEventTypes, searchAuditLogs, type AuthAuditLog } from '../../api/system';

const { RangePicker } = DatePicker;

function startOfDayIso(date: Date) {
  const value = new Date(date);
  value.setHours(0, 0, 0, 0);
  return value.toISOString();
}

function endOfDayIso(date: Date) {
  const value = new Date(date);
  value.setHours(23, 59, 59, 999);
  return value.toISOString();
}

function defaultFromIso() {
  const value = new Date();
  value.setDate(value.getDate() - 6);
  return startOfDayIso(value);
}

function defaultToIso() {
  return endOfDayIso(new Date());
}

function formatDateTime(value: string) {
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) {
    return value;
  }
  return date.toLocaleString('zh-CN', { hour12: false });
}

export function LogsPage() {
  const [eventType, setEventType] = useState<string>();
  const [userId, setUserId] = useState('');
  const [result, setResult] = useState<string>();
  const [range, setRange] = useState<[Dayjs | null, Dayjs | null] | null>(null);
  const [page, setPage] = useState(1);
  const [pageSize, setPageSize] = useState(20);

  const eventTypesQuery = useQuery({
    queryKey: ['system', 'audit-event-types'],
    queryFn: listAuditEventTypes
  });

  const searchParams = useMemo(() => ({
    eventType: eventType || undefined,
    userId: userId.trim() || undefined,
    result: result || undefined,
    from: range?.[0]?.startOf('day').toISOString() ?? defaultFromIso(),
    to: range?.[1]?.endOf('day').toISOString() ?? defaultToIso(),
    page,
    pageSize
  }), [eventType, userId, result, range, page, pageSize]);

  const logsQuery = useQuery({
    queryKey: ['system', 'audit-logs', searchParams],
    queryFn: () => searchAuditLogs(searchParams)
  });

  const logs = logsQuery.data?.items ?? [];
  const total = logsQuery.data?.total ?? 0;

  const columns: ColumnsType<AuthAuditLog> = [
    {
      title: '时间',
      dataIndex: 'occurredAt',
      width: 180,
      render: (value: string) => formatDateTime(value)
    },
    { title: '事件', dataIndex: 'eventType', width: 160 },
    { title: '用户', dataIndex: 'userId', width: 140, render: (value?: string | null) => value ?? '-' },
    {
      title: '结果',
      dataIndex: 'result',
      width: 100,
      render: (value: string) => (
        <Tag color={value === 'SUCCESS' ? 'green' : value === 'FAILURE' ? 'red' : 'default'}>{value}</Tag>
      )
    },
    { title: '错误码', dataIndex: 'errorCode', width: 120, render: (value?: string | null) => value ?? '-' },
    { title: 'IP', dataIndex: 'clientIp', width: 130, render: (value?: string | null) => value ?? '-' },
    { title: '应用', dataIndex: 'appId', width: 120, render: (value?: string | null) => value ?? '-' }
  ];

  const eventTypeOptions = (eventTypesQuery.data ?? []).map((type) => ({ value: type, label: type }));

  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: 16 }}>
      <Card>
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 16 }}>
          <div>
            <div style={{ fontSize: 18, fontWeight: 600 }}>日志管理</div>
            <div style={{ color: '#8c8c8c', marginTop: 4 }}>查询认证与授权相关审计日志</div>
          </div>
          <Button icon={<ReloadOutlined />} onClick={() => logsQuery.refetch()} loading={logsQuery.isFetching}>
            刷新
          </Button>
        </div>

        <Space wrap style={{ width: '100%' }}>
          <Select
            allowClear
            placeholder="事件类型"
            style={{ width: 180 }}
            options={eventTypeOptions}
            value={eventType}
            onChange={(value) => {
              setEventType(value);
              setPage(1);
            }}
          />
          <Input
            allowClear
            placeholder="用户 ID"
            style={{ width: 180 }}
            value={userId}
            onChange={(event) => setUserId(event.target.value)}
            onPressEnter={() => setPage(1)}
          />
          <Select
            allowClear
            placeholder="结果"
            style={{ width: 120 }}
            options={[
              { value: 'SUCCESS', label: 'SUCCESS' },
              { value: 'FAILURE', label: 'FAILURE' }
            ]}
            value={result}
            onChange={(value) => {
              setResult(value);
              setPage(1);
            }}
          />
          <RangePicker
            value={range}
            onChange={(value) => {
              setRange(value);
              setPage(1);
            }}
          />
          <Button
            type="primary"
            icon={<SearchOutlined />}
            onClick={() => {
              setPage(1);
              logsQuery.refetch();
            }}
          >
            查询
          </Button>
        </Space>
      </Card>

      <Card>
        <Table
          rowKey="id"
          loading={logsQuery.isLoading}
          columns={columns}
          dataSource={logs}
          scroll={{ x: 1200 }}
          pagination={{
            current: page,
            pageSize,
            total,
            showSizeChanger: true,
            onChange: (nextPage, nextPageSize) => {
              setPage(nextPage);
              setPageSize(nextPageSize);
            }
          }}
        />
      </Card>
    </div>
  );
}
