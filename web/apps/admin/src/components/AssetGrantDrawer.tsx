import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { Button, Drawer, Form, Radio, Space, TreeSelect, Typography, message } from 'antd';
import { useEffect, useMemo } from 'react';
import { listAssetGrants, saveAssetGrants, type AssetGrant, type AssetType } from '../api/assetGrants';
import { listOrganizations } from '../api/identity';
import { resolveIdentityTenantId } from '../api/auth';
import { buildDepartmentPickerTree, buildOrganizationTreeByType, filterDepartmentIds, toTreeSelectData } from '../utils/organizationTree';

export interface AssetGrantDrawerProps {
  open: boolean;
  assetType: AssetType;
  assetId?: string;
  assetName?: string;
  ownerUnitId?: string | null;
  onClose: () => void;
  onSaved?: (ownerUnitId?: string | null) => void;
}

type GrantFormValues = {
  unitIds: string[];
  unitScope: 'SELF' | 'SUBTREE';
  departmentIds: string[];
  departmentScope: 'SELF' | 'SUBTREE';
};

function isOwnerGrant(grant: AssetGrant, ownerUnitId?: string | null) {
  return Boolean(ownerUnitId)
    && grant.unitId === ownerUnitId
    && !grant.departmentId
    && (grant.unitScope ?? 'SELF') === 'SELF'
    && (grant.departmentScope ?? 'SELF') === 'SELF';
}

function toFormValues(grants: AssetGrant[], ownerUnitId?: string | null): GrantFormValues {
  const extra = grants.filter((grant) => !isOwnerGrant(grant, ownerUnitId));
  if (extra.length === 0) {
    return { unitIds: [], unitScope: 'SELF', departmentIds: [], departmentScope: 'SELF' };
  }
  const allUnits = extra.some((grant) => !grant.unitId);
  const allDepartments = extra.some((grant) => !grant.departmentId);
  const unitIds = allUnits
    ? []
    : [...new Set(extra.map((grant) => grant.unitId).filter(Boolean))] as string[];
  const departmentIds = allDepartments
    ? []
    : [...new Set(extra.map((grant) => grant.departmentId).filter(Boolean))] as string[];
  const reference = extra[0];
  return {
    unitIds,
    unitScope: reference?.unitScope ?? 'SELF',
    departmentIds,
    departmentScope: reference?.departmentScope ?? 'SELF'
  };
}

export function AssetGrantDrawer({
  open,
  assetType,
  assetId,
  assetName,
  ownerUnitId,
  onClose,
  onSaved
}: AssetGrantDrawerProps) {
  const queryClient = useQueryClient();
  const [form] = Form.useForm<GrantFormValues>();
  const identityTenantId = resolveIdentityTenantId();
  const organizationsQuery = useQuery({
    queryKey: ['identity-organizations', identityTenantId],
    queryFn: () => listOrganizations(identityTenantId),
    enabled: open
  });
  const grantsQuery = useQuery({
    queryKey: ['asset-grants', assetType, assetId],
    queryFn: () => listAssetGrants(assetType, assetId!),
    enabled: open && Boolean(assetId)
  });
  const organizations = organizationsQuery.data?.items ?? [];
  const unitTreeData = useMemo(
    () => toTreeSelectData(buildOrganizationTreeByType(organizations, 'UNIT')),
    [organizations]
  );
  const departmentTreeData = useMemo(
    () => buildDepartmentPickerTree(organizations),
    [organizations]
  );

  const saveMutation = useMutation({
    mutationFn: (values: GrantFormValues) => {
      if (!assetId) {
        throw new Error('请选择资产');
      }
      return saveAssetGrants({
        assetType,
        assetId,
        ownerUnitId: ownerUnitId ?? null,
        unitIds: values.unitIds ?? [],
        unitScope: values.unitScope ?? 'SELF',
        departmentIds: values.departmentIds ?? [],
        departmentScope: values.departmentScope ?? 'SELF'
      });
    },
    onSuccess: async (response) => {
      message.success('授权已保存');
      await queryClient.invalidateQueries({ queryKey: ['asset-grants', assetType, assetId] });
      if (assetType === 'KNOWLEDGE_BASE') {
        await queryClient.invalidateQueries({ queryKey: ['knowledge-bases'] });
      }
      if (assetType === 'BOT') {
        await queryClient.invalidateQueries({ queryKey: ['bots'] });
      }
      if (assetType === 'WORKFLOW') {
        await queryClient.invalidateQueries({ queryKey: ['workflows'] });
      }
      if (assetType === 'MODEL_PROVIDER') {
        await queryClient.invalidateQueries({ queryKey: ['model-providers'] });
      }
      onSaved?.(response.ownerUnitId);
      onClose();
    },
    onError: (error) => {
      message.error((error as Error).message);
    }
  });

  useEffect(() => {
    if (!open || !assetId || organizationsQuery.isLoading || !grantsQuery.data) {
      return;
    }
    const effectiveOwnerUnitId = ownerUnitId ?? grantsQuery.data.ownerUnitId ?? null;
    form.setFieldsValue(toFormValues(grantsQuery.data.grants, effectiveOwnerUnitId));
  }, [open, assetId, assetType, ownerUnitId, form, grantsQuery.data, organizationsQuery.isLoading]);

  return (
    <Drawer
      title={assetName ? `授权 - ${assetName}` : '授权'}
      open={open}
      width={720}
      destroyOnClose
      onClose={onClose}
      footer={(
        <Space style={{ display: 'flex', justifyContent: 'flex-end' }}>
          <Button onClick={onClose}>关闭</Button>
          <Button type="primary" loading={saveMutation.isPending} onClick={() => form.submit()}>保存授权</Button>
        </Space>
      )}
    >
      <Form form={form} layout="vertical" initialValues={{ unitIds: [], unitScope: 'SELF', departmentIds: [], departmentScope: 'SELF' }} onFinish={(values) => saveMutation.mutate(values)}>
        <Form.Item label="授权单位">
          <Space direction="vertical" size={8} style={{ width: '100%' }}>
            <Form.Item name="unitIds" noStyle>
              <TreeSelect
                treeCheckable
                multiple
                allowClear
                showCheckedStrategy={TreeSelect.SHOW_ALL}
                treeDefaultExpandAll
                placeholder="不选表示全部单位"
                style={{ width: '100%' }}
                loading={organizationsQuery.isLoading}
                treeData={unitTreeData}
                maxTagCount="responsive"
              />
            </Form.Item>
            <Space wrap>
              <Form.Item name="unitScope" noStyle rules={[{ required: true }]}>
                <Radio.Group optionType="button" options={[{ value: 'SELF', label: '仅本单位' }, { value: 'SUBTREE', label: '本级及下属' }]} />
              </Form.Item>
              <Typography.Text type="secondary">不选=全部单位</Typography.Text>
            </Space>
          </Space>
        </Form.Item>
        <Form.Item label="授权部门">
          <Space direction="vertical" size={8} style={{ width: '100%' }}>
            <Form.Item name="departmentIds" noStyle getValueFromEvent={(values: string[]) => filterDepartmentIds(values ?? [], organizations)}>
              <TreeSelect
                treeCheckable
                multiple
                allowClear
                showCheckedStrategy={TreeSelect.SHOW_CHILD}
                treeDefaultExpandAll
                placeholder="不选表示全部部门"
                style={{ width: '100%' }}
                loading={organizationsQuery.isLoading}
                treeData={departmentTreeData}
                maxTagCount="responsive"
              />
            </Form.Item>
            <Space wrap>
              <Form.Item name="departmentScope" noStyle rules={[{ required: true }]}>
                <Radio.Group optionType="button" options={[{ value: 'SELF', label: '仅本部门' }, { value: 'SUBTREE', label: '本级及下属' }]} />
              </Form.Item>
              <Typography.Text type="secondary">不选=全部部门</Typography.Text>
            </Space>
          </Space>
        </Form.Item>
      </Form>
    </Drawer>
  );
}
