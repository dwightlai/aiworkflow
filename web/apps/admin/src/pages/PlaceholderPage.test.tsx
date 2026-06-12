// @vitest-environment jsdom
import '@testing-library/jest-dom/vitest';
import { render, screen } from '@testing-library/react';
import { describe, expect, it } from 'vitest';
import { PlaceholderPage } from './PlaceholderPage';

describe('PlaceholderPage', () => {
  it('renders a module workspace instead of an empty placeholder', () => {
    render(<PlaceholderPage title="智能体" />);

    expect(screen.getByText('智能体')).toBeInTheDocument();
    expect(screen.getByText(/模块工作区/)).toBeInTheDocument();
    expect(screen.getByText('能力规划')).toBeInTheDocument();
    expect(screen.getAllByText(/集成状态/).length).toBeGreaterThan(0);
    expect(screen.getByText('进入工作流运营台')).toBeInTheDocument();
  });
});
