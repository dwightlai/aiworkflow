// @vitest-environment jsdom
import '@testing-library/jest-dom/vitest';
import { render, screen } from '@testing-library/react';
import { describe, expect, it } from 'vitest';
import { AdminShell } from './AdminShell';

describe('AdminShell', () => {
  it('renders grouped AI and system menus with page content', () => {
    render(
      <AdminShell title="工作台" breadcrumb={['首页', 'AI 功能', '工作台']}>
        <div>dashboard content</div>
      </AdminShell>
    );

    expect(screen.getByText('AI 功能')).toBeInTheDocument();
    expect(screen.getByText('工作流')).toBeInTheDocument();
    expect(screen.getByText('系统管理')).toBeInTheDocument();
    expect(screen.getByText('用户管理')).toBeInTheDocument();
    expect(screen.getByText('首页 / AI 功能 / 工作台')).toBeInTheDocument();
    expect(screen.getByText('dashboard content')).toBeInTheDocument();
  });
});
