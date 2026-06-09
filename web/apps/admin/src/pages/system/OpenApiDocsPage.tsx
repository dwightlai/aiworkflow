import { Typography } from 'antd';

function buildSwaggerUrl() {
  const apiOrigin = `${window.location.protocol}//${window.location.hostname}:8080`;
  const configUrl = `${apiOrigin}/v3/api-docs/swagger-config`;
  return `${apiOrigin}/swagger-ui/index.html?configUrl=${encodeURIComponent(configUrl)}&urls.primaryName=open-api`;
}

export function OpenApiDocsPage() {
  return (
    <div style={{ display: 'flex', flexDirection: 'column', height: '100%', minHeight: 0 }}>
      <Typography.Text type="secondary" style={{ marginBottom: 12 }}>
        开放 API 文档（Swagger UI），供第三方系统（如数字档案馆）集成参考。在线文档由后端接口自动生成，静态规范见仓库 `docs/openapi/open-api.yaml`。
      </Typography.Text>
      <iframe
        title="开放 API 文档"
        src={buildSwaggerUrl()}
        style={{ border: 0, flex: 1, minHeight: 680, width: '100%' }}
      />
    </div>
  );
}
