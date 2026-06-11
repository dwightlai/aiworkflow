import type { Organization } from '../api/identity';

export type OrgTreeNode = Organization & { children?: OrgTreeNode[] };

export type TreeSelectNode = {
  value: string;
  title: string;
  key: string;
  disableCheckbox?: boolean;
  selectable?: boolean;
  children?: TreeSelectNode[];
};

export function compareOrganizations(left: Organization, right: Organization) {
  return (left.sortOrder ?? 0) - (right.sortOrder ?? 0)
    || left.code.localeCompare(right.code)
    || left.name.localeCompare(right.name);
}

export function buildOrganizationTree(organizations: Organization[]): OrgTreeNode[] {
  const byId = new Map<string, OrgTreeNode>();
  organizations.forEach((org) => byId.set(org.id, { ...org, children: [] }));
  const roots: OrgTreeNode[] = [];
  byId.forEach((org) => {
    if (org.parentId && byId.has(org.parentId)) {
      byId.get(org.parentId)?.children?.push(org);
    } else {
      roots.push(org);
    }
  });
  byId.forEach((org) => {
    if (org.children?.length === 0) {
      delete org.children;
    } else {
      org.children?.sort(compareOrganizations);
    }
  });
  return roots.sort(compareOrganizations);
}

export function buildOrganizationTreeByType(organizations: Organization[], orgType: string): OrgTreeNode[] {
  return buildOrganizationTree(organizations.filter((org) => org.orgType === orgType));
}

export function toTreeSelectData(nodes: OrgTreeNode[]): TreeSelectNode[] {
  return nodes.map((org) => ({
    value: org.id,
    title: org.name,
    key: org.id,
    children: org.children ? toTreeSelectData(org.children) : undefined
  }));
}

function organizationById(organizations: Organization[]) {
  return new Map(organizations.map((org) => [org.id, org]));
}

function findUnitAncestor(org: Organization, byId: Map<string, Organization>): Organization | null {
  let parentId = org.parentId;
  while (parentId && byId.has(parentId)) {
    const parent = byId.get(parentId)!;
    if (parent.orgType === 'UNIT') {
      return parent;
    }
    parentId = parent.parentId;
  }
  return null;
}

function toDepartmentPickerNode(node: OrgTreeNode, byId: Map<string, Organization>): TreeSelectNode | null {
  if (node.orgType === 'DEPARTMENT') {
    if (!findUnitAncestor(node, byId)) {
      return null;
    }
    const children = (node.children ?? [])
      .map((child) => toDepartmentPickerNode(child, byId))
      .filter((child): child is TreeSelectNode => child != null);
    return {
      key: node.id,
      value: node.id,
      title: node.name,
      children: children.length > 0 ? children : undefined
    };
  }

  const children = (node.children ?? [])
    .map((child) => toDepartmentPickerNode(child, byId))
    .filter((child): child is TreeSelectNode => child != null);
  if (children.length === 0) {
    return null;
  }

  return {
    key: `group-${node.id}`,
    value: `group-${node.id}`,
    title: node.name,
    disableCheckbox: true,
    selectable: false,
    children
  };
}

export function buildDepartmentPickerTree(organizations: Organization[]): TreeSelectNode[] {
  const byId = organizationById(organizations);
  return buildOrganizationTree(organizations)
    .map((node) => toDepartmentPickerNode(node, byId))
    .filter((node): node is TreeSelectNode => node != null);
}

export function filterDepartmentIds(values: string[], organizations: Organization[]) {
  const departmentIds = new Set(
    organizations.filter((org) => org.orgType === 'DEPARTMENT').map((org) => org.id)
  );
  return values.filter((value) => departmentIds.has(value));
}
