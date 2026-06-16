# 智能编研成果版式设计文档（DOCX 为主 / HTML 为辅）

## 1. 设计背景

当前智能编研模块已经具备基于编研模板、主题库、知识库和工作流生成编研内容的能力。用户可以通过智能编研向导填写主题、选择资料源、选择知识库和编研模板，系统按章节生成大纲、正文、引用关系，并输出 Markdown 或 DOCX 成果。

但在实际业务使用中，智能编研不仅需要“生成内容”，还需要解决“成果如何排版、如何归档、如何报送、如何发布”的问题。

**在档案编研场景中，DOCX 是主交付形态**：用于正式报送、领导审阅、纸质归档、馆室留存和对外交换；版式需符合机关公文/档案编研成品的排版习惯（封面、目录、标题层级、页眉页脚、页码、引用脚注等）。

**HTML 是辅助交付形态**：用于在线预览、图文展陈、时间轴浏览、专题发布和打印 PDF，版式从同一份结构化成果 JSON 衍生渲染，不单独维护内容。

因此，系统需要在现有“编研内容生成”能力基础上，**以 DOCX 版式为主**建设成果模板体系，并同步支持 HTML 在线展示。本期重点模板类型：

1. 普通专题报告（DOCX 主模板）；
2. 图文展陈（DOCX + HTML）；
3. 时间轴专题（DOCX + HTML）。

------

## 2. 总体设计目标

### 2.1 建设目标

智能编研成果版式能力的目标是：

1. 将 AI 生成的编研内容转换为统一的结构化成果 JSON；
2. **以 DOCX 版式模板为主**，输出符合档案编研规范的 Word 成果；
3. 支持普通报告、图文展陈、时间轴专题、简报等多种 DOCX 版式；
4. 在同一份 `content_json` 上衍生 HTML 版式，用于在线预览与专题发布；
5. 支持版式切换、DOCX 下载、HTML 导出、打印 PDF；
6. 为正式归档、领导报送和在线展陈提供双通道交付。

### 2.2 核心原则

1. **DOCX 为主、HTML 为辅**：版式规范、样式命名、章节结构以 DOCX 模板为基准；
2. 内容生成与版式渲染分离；
3. 大模型只生成内容与结构，不直接生成最终 Word/HTML 排版；
4. 系统负责结构化、模板化渲染（DOCX 用模板引擎，HTML 用组件渲染）；
5. 同一份 `content_json` 可映射多种 DOCX/HTML 版式；
6. DOCX 优先支持 `.docx` 母版文件 + 样式占位符，便于单位自定义单位格式。

------

## 3. 总体架构

### 3.1 分层架构

```text
内容生成层
  大模型 + 知识库 + 工作流 → 章节正文、摘要、时间线、表格、引用

结构化成果层
  统一 content_json（标题、章节、图片、时间轴、引用、元数据）

DOCX 版式模板层（主）
  .docx 母版 + 样式映射 + 占位符 → 正式 Word 成果

HTML 版式模板层（辅）
  由 content_json 组件化渲染 → 在线预览 / 展陈 / PDF

成果导出与发布层
  下载 DOCX | 预览 HTML | 打包 ZIP | 打印 PDF | 专题发布
```

### 3.2 流程示意

```text
用户创建智能编研任务
        ↓
选择主题、知识库、编研模板（含默认 DOCX 版式）
        ↓
工作流生成章节内容
        ↓
系统整理为 content_json
        ↓
┌───────────────────────┬───────────────────────┐
│ DOCX 渲染（主路径）    │ HTML 渲染（辅路径）    │
│ 套用 docx 母版        │ 套用 HTML 组件模板     │
│ 生成正式 Word 文件     │ 在线预览 / 展陈       │
└───────────────────────┴───────────────────────┘
        ↓
下载 DOCX / 预览 HTML / 打印 PDF / 发布专题
```

### 3.3 输出形态优先级

| 优先级 | 输出类型 | 用途 |
|--------|----------|------|
| P0 | DOCX | 正式成果、归档、报送、打印 |
| P1 | HTML | 在线阅读、展陈、内网发布 |
| P2 | PDF | 由 DOCX 或 HTML 打印生成 |
| P3 | Markdown | 中间态、技术交换 |

------

## 4. 与现有智能编研模块的关系

现有智能编研模块主要包括：

1. 编研模板；
2. 智能编研向导；
3. 工作流绑定；
4. 章节内容生成；
5. Markdown / DOCX 导出。

新增 HTML 成果版式后，整体关系调整为：

```text
编研模板
├── 内容结构配置
│   ├── 变量字段
│   ├── 章节结构
│   ├── 写作指令
│   ├── 引用要求
│   └── 绑定工作流
│
└── 成果版式配置（DOCX 为主）
    ├── DOCX 版式（默认、主输出）
    │   ├── 普通专题报告
    │   ├── 图文展陈报告
    │   ├── 时间轴专题报告
    │   └── 简报
    └── HTML 版式（衍生、在线展示）
        ├── 图文展陈页
        ├── 时间轴专题页
        └── 普通专题页
```

智能编研生成结果页：

```text
生成成果
├── 内容预览
├── DOCX 预览 / 下载（默认 Tab）
├── HTML 在线预览（可选 Tab）
├── 版式切换（DOCX 模板优先）
├── Markdown 预览
└── 打印 PDF
```

------

# 5. DOCX 成果模板分类（主输出）

系统内置 DOCX 成果模板分为以下几类：

| 模板类型 | template_category | 适用场景 | 主要特点 |
| -------- | ----------------- | -------- | -------- |
| 普通专题报告 | `report` | 调研报告、专题分析、制度解读、编研成果报送 | 封面+目录+章节正文+参考文献，机关公文风格 |
| 图文展陈报告 | `gallery` | 档案展陈、历史资料、项目档案汇编 | 图文混排、图片题注、展品说明表 |
| 时间轴专题报告 | `timeline` | 历史沿革、项目发展、机构变迁 | 分阶段章节+事件条目表+时间线附图 |
| 简报 | `briefing` | 领导参阅、信息摘编 | 短篇幅、要点突出、无复杂目录 |
| 数据汇编 | `dashboard` | 统计汇总、指标说明 | 表格为主、附说明文字 |

本期重点建设：

1. 普通专题报告 DOCX；
2. 图文展陈 DOCX；
3. 时间轴专题 DOCX。

每种 DOCX 模板可绑定对应 HTML 衍生模板（可选），共享 `content_json`。

------

# 6. DOCX 通用版式规范

## 6.1 页面与版心

| 配置项 | 默认值 | 说明 |
| ------ | ------ | ---- |
| 纸张 | A4（210×297mm） | 纵向 |
| 上边距 | 2.54cm | 可配置 |
| 下边距 | 2.54cm | 含页码区 |
| 左边距 | 3.17cm | 装订侧 |
| 右边距 | 3.17cm | |
| 页眉距边界 | 1.5cm | |
| 页脚距边界 | 1.75cm | |
| 默认行距 | 固定值 28 磅 | 正文段落 |
| 段前段后 | 0 行 | 标题单独配置 |

## 6.2 Word 样式体系（样式 ID 与模板绑定）

DOCX 母版文件预置以下命名样式，渲染引擎按 `content_json` 节点类型套用：

| 样式名 | 西文字体 | 中文字体 | 字号 | 加粗 | 对齐 | 用途 |
| ------ | -------- | -------- | ---- | ---- | ---- | ---- |
| `RW_CoverTitle` | Times New Roman | 小标宋 / 方正小标宋 | 二号(22pt) | 是 | 居中 | 封面主标题 |
| `RW_CoverSubtitle` | Times New Roman | 楷体 | 三号(16pt) | 否 | 居中 | 封面副标题 |
| `RW_CoverMeta` | Times New Roman | 仿宋 | 四号(14pt) | 否 | 居中 | 编研单位、日期 |
| `RW_TocTitle` | Times New Roman | 黑体 | 三号 | 是 | 居中 | 目录标题 |
| `RW_TocEntry` | Times New Roman | 仿宋 | 三号 | 否 | 左对齐 | 目录条目（含制表符引导+页码） |
| `RW_Heading1` | Times New Roman | 黑体 | 三号(16pt) | 是 | 居中 | 一级标题（章） |
| `RW_Heading2` | Times New Roman | 黑体 | 小三(15pt) | 是 | 左对齐 | 二级标题（节） |
| `RW_Heading3` | Times New Roman | 楷体 | 四号(14pt) | 是 | 左对齐 | 三级标题 |
| `RW_Body` | Times New Roman | 仿宋_GB2312 | 三号(16pt) | 否 | 两端对齐 | 正文 |
| `RW_Quote` | Times New Roman | 仿宋 | 三号 | 否 | 两端对齐 | 引用摘录块 |
| `RW_Caption` | Times New Roman | 仿宋 | 小四(12pt) | 否 | 居中 | 图片/表格题注 |
| `RW_Footnote` | Times New Roman | 仿宋 | 小五(9pt) | 否 | 左对齐 | 脚注（引用来源） |
| `RW_TableHeader` | Times New Roman | 黑体 | 小四 | 是 | 居中 | 表头 |
| `RW_TableBody` | Times New Roman | 仿宋 | 小四 | 否 | 居中 | 表体 |
| `RW_AppendixTitle` | Times New Roman | 黑体 | 三号 | 是 | 居中 | 附件/参考文献标题 |

> 单位可在上传自定义 `.docx` 母版时替换字体，但**样式名（RW_*）保持不变**，以保证渲染引擎可映射。

## 6.3 页眉页脚与页码

| 区域 | 内容 | 规则 |
| ---- | ---- | ---- |
| 封面节 | 无页眉页脚 | 独立节，不编页码 |
| 目录节 | 可选页眉：成果简称 | 罗马数字页码 Ⅰ、Ⅱ… |
| 正文节 | 页眉：编研成果标题（ shortened） | 阿拉伯数字页码，居中或外侧 |
| 页脚 | 可选：编研单位 | |

分节符：`封面 | 目录 | 正文 | 参考文献` 各为独立 Section。

## 6.4 封面结构（DOCX）

```text
[分节：封面]
  专题标题          ← RW_CoverTitle
  副标题（可选）     ← RW_CoverSubtitle
  （空行）
  编研单位          ← RW_CoverMeta
  编研时间          ← RW_CoverMeta
  密级（可选）       ← RW_CoverMeta
```

占位符映射：

| 占位符 | content_json 字段 |
| ------ | ----------------- |
| `{{title}}` | `title` |
| `{{subtitle}}` | `subtitle` |
| `{{organization}}` | `cover.organization` |
| `{{publishDate}}` | `cover.publishDate` |
| `{{securityLevel}}` | `cover.securityLevel` |

## 6.5 目录（DOCX）

- 使用 Word 原生 TOC 域或渲染时写入目录段落；
- 目录深度：1～3 级（对应 Heading1～Heading3）；
- 章节标题必须使用 `RW_Heading*` 样式，确保目录可更新；
- 配置项 `tocDepth`：默认 2。

## 6.6 正文与引用（DOCX）

| 元素 | DOCX 实现 |
| ---- | --------- |
| 章节正文 | `RW_Body` 段落 |
| 引用标注 | 上标脚注编号 `[1]`，脚注区列出来源、页码、摘录 |
| 引用列表 | 文末「参考文献」章节，编号列表 |
| 图片 | 嵌入 `InlineImage`，宽度不超过版心，下方 `RW_Caption` 题注 |
| 表格 | Word 表格，表头 `RW_TableHeader` |
| 分点列表 | Word 编号列表或项目符号 |

脚注数据结构：

```json
{
  "citations": [
    {
      "refNo": 1,
      "sourceTitle": "档案数字化建设方案",
      "page": "3",
      "quote": "相关引用片段……",
      "sourceType": "档案文件"
    }
  ]
}
```

## 6.7 DOCX 模板配置项（通用）

```json
{
  "templateName": "档案编研普通报告",
  "templateCode": "archive_report_docx",
  "outputType": "DOCX",
  "templateCategory": "report",
  "docxConfig": {
    "masterFile": "/templates/research/archive_report.docx",
    "pageSize": "A4",
    "marginTopCm": 2.54,
    "marginBottomCm": 2.54,
    "marginLeftCm": 3.17,
    "marginRightCm": 3.17,
    "lineSpacingPt": 28,
    "showCover": true,
    "showToc": true,
    "tocDepth": 2,
    "showPageNumber": true,
    "pageNumberFormat": "arabic",
    "headerText": "{{title}}",
    "footerText": "{{organization}}",
    "footnoteStyle": "numeric",
    "showReferenceSection": true
  },
  "styleMapping": {
    "coverTitle": "RW_CoverTitle",
    "heading1": "RW_Heading1",
    "heading2": "RW_Heading2",
    "body": "RW_Body",
    "caption": "RW_Caption",
    "footnote": "RW_Footnote"
  }
}
```

## 6.8 DOCX 渲染技术路线

| 方式 | 说明 | 优先级 |
| ---- | ---- | ------ |
| **母版填充** | 上传 `.docx` 母版，占位符 `{{title}}`、`{{#sections}}` 循环，Apache POI / docx4j 填充 | 推荐 |
| **样式构建** | 无母版时按 `docxConfig` 动态创建样式（当前 `ResearchDocxExporter` 简化版） | 兜底 |
| **HTML→DOCX** | 不推荐作为主路径，仅作兼容 | 低 |

本期目标：从当前 Markdown 行解析升级为 **母版 + content_json 结构化渲染**。

------

# 7. 图文展陈 DOCX 模板设计

## 7.1 定位

在普通报告版式基础上，强化**图片、档案展品、题注、展品目录表**，输出仍可打印、归档的 Word 文档（非网页）。

适用：档案专题汇编、展陈说明册、图文并茂的编研成果报送。

## 7.2 文档结构

```text
封面
目录
导语 / 摘要
第一章  专题背景          ← 普通章节
第二章  重点档案展品      ← 图文章节（gallery）
  2.1 展品一：标题
      [图片]
      题注：形成时间、来源、档号
      说明文字
  2.2 展品二 …
第三章  资料解读
第四章  总结
参考文献 / 资料来源
```

## 7.3 图文组件（DOCX）

| 组件 | DOCX 实现 |
| ---- | --------- |
| 单图+说明 | 图片段落 + `RW_Caption` + `RW_Body` 说明 |
| 双图并排 | 1×2 表格无框线，每格一图+题注 |
| 展品目录表 | 表头：序号、题名、档号、形成时间、来源 |
| 档案元数据块 | 边框表格：档号、密级、责任者、归档部门 |

## 7.4 content_json 扩展（gallery 类型章节）

```json
{
  "id": "section_2",
  "title": "二、重点档案展示",
  "type": "gallery",
  "items": [
    {
      "title": "档案数字化项目立项文件",
      "imageRef": "img_001",
      "dh": "004-A-2025-D10-410-0011",
      "date": "2021-03-12",
      "sourceTitle": "项目立项审批材料",
      "description": "该文件标志着企业档案数字化建设正式启动。",
      "citationRef": 1
    }
  ]
}
```

渲染规则：每个 `gallery` item → 二级标题 + 图片 + 题注表 + 正文说明 + 脚注。

## 7.5 对应 HTML 衍生模板

| DOCX 模板 | HTML 衍生 |
| --------- | --------- |
| `archive_gallery_docx` | `archive_gallery`（沉浸式/卡片布局） |

同一 `content_json`，DOCX 偏打印归档，HTML 偏在线展陈。

------

# 8. 时间轴专题 DOCX 模板设计

## 8.1 定位

按时间顺序编排事件，适合历程类编研成果的**正式 Word 报送**。

## 8.2 文档结构

```text
封面（含时间范围 {{timeRange}}）
目录
概述
第一阶段：{{stage.title}}（{{stage.period}}）
  事件条目表 / 分条叙述
  2020-05  事件标题
           事件说明
           [可选图片]
第二阶段：…
阶段总结
参考文献
```

## 8.3 时间轴在 DOCX 中的两种版式

### 8.3.1 表格式（推荐打印）

| 时间 | 事件 | 说明 | 来源 |
| ---- | ---- | ---- | ---- |
| 2020-05 | 完成数字化调研 | … | [1] |

样式：`RW_TableHeader` / `RW_TableBody`。

### 8.3.2 叙述式

每个事件：`RW_Heading3`（时间+标题）+ `RW_Body`（说明）+ 脚注。

### 8.3.3 分阶段

`RW_Heading1` 阶段名 → 其下事件条目（表格式或叙述式）。

## 8.4 content_json（与 HTML 共用）

```json
{
  "templateType": "timeline",
  "timeRange": { "start": "2020", "end": "2026" },
  "stages": [
    {
      "title": "第一阶段：启动准备期",
      "period": "2020—2021",
      "events": [
        {
          "date": "2020-05",
          "title": "完成档案数字化建设调研",
          "description": "……",
          "citations": [{ "refNo": 1 }]
        }
      ]
    }
  ]
}
```

## 8.5 DOCX 配置扩展

```json
{
  "templateCode": "timeline_topic_docx",
  "outputType": "DOCX",
  "templateCategory": "timeline",
  "docxConfig": {
    "timelineLayout": "table",
    "showStageSummary": true,
    "showTimeRangeOnCover": true,
    "eventSort": "asc"
  }
}
```

| timelineLayout | 说明 |
| -------------- | ---- |
| `table` | 事件一览表 |
| `narrative` | 分条叙述 |
| `staged` | 分阶段+子事件 |

------

# 第二部分：HTML 衍生版式（辅）

> 以下 HTML 版式与 DOCX 共用 `content_json`，用于在线预览与专题发布；**版式语义以 DOCX 章节结构为准**，HTML 仅改变视觉呈现。

## 9. HTML 成果模板分类

系统内置 HTML 成果模板分为以下几类：

| 模板类型         | 适用场景                               | 主要特点                           |
| ---------------- | -------------------------------------- | ---------------------------------- |
| 普通专题报告模板 | 调研报告、专题分析、制度解读           | 类似网页文章，结构清晰，阅读体验好 |
| 图文展陈模板     | 档案展陈、历史资料展示、企业文化专题   | 图片、文字、说明、引用并重         |
| 时间轴专题模板   | 历史沿革、项目发展、事件脉络、人物经历 | 按时间顺序展示事件                 |
| 数据看板模板     | 统计分析、成果汇总、项目指标           | 指标卡、图表、表格展示             |
| 简报模板         | 领导参阅、专题快报、信息摘编           | 短内容、重点突出、便于快速阅读     |

本期重点建设：

1. 图文展陈模板；
2. 时间轴专题模板；
3. 普通专题报告模板。

------

## 10. 图文展陈 HTML 模板设计

## 6.1 模板定位

图文展陈模板用于将档案资料、图片、扫描件、历史照片、项目材料、人物资料等内容组织成在线展陈页面。

该模板适合以下场景：

1. 企业发展历程展示；
2. 档案专题编研成果展示；
3. 重大项目档案展；
4. 红色档案专题展；
5. 企业文化专题展；
6. 老照片、文件、实物档案在线展示；
7. 纪念日专题页面；
8. 历史事件图文回顾。

## 6.2 页面结构

图文展陈模板建议采用以下结构：

```text
封面区
├── 专题标题
├── 副标题
├── 背景图
├── 编研单位
└── 发布时间

导语区
├── 专题摘要
├── 编研说明
└── 资料来源概述

目录导航区
├── 固定目录
├── 章节锚点
└── 快速跳转

展陈内容区
├── 章节一：背景介绍
├── 章节二：图文资料
├── 章节三：重点档案
├── 章节四：资料解读
└── 章节五：总结说明

图片画廊区
├── 缩略图
├── 大图预览
├── 图片说明
├── 来源信息
└── 相关档案链接

引用来源区
├── 引用文档
├── 引用页码
├── 原文片段
└── 资料出处

页脚区
├── 编研单位
├── 生成时间
├── 版权说明
└── 技术支持信息
```

## 6.3 页面布局

图文展陈模板建议内置三种布局：

### 6.3.1 单栏沉浸式布局

适合正式专题展示，图片较多，页面风格偏展览。

```text
顶部大封面
↓
导语
↓
章节内容
↓
图文卡片
↓
引用来源
```

特点：

1. 页面视觉冲击强；
2. 适合图片、扫描件、档案照片展示；
3. 适合对外发布。

### 6.3.2 左侧目录 + 右侧正文布局

适合内容较长、章节较多的图文报告。

```text
左侧：章节目录
右侧：正文内容 + 图文资料
```

特点：

1. 章节跳转方便；
2. 适合内部阅读；
3. 适合长篇专题成果。

### 6.3.3 卡片式展陈布局

适合把档案资料作为一个个“展品”展示。

```text
专题说明
↓
展品卡片列表
↓
点击查看详情
```

每个卡片包括：

1. 档案标题；
2. 档案图片；
3. 形成时间；
4. 形成单位；
5. 简要说明；
6. 来源引用；
7. 查看原文。

## 6.4 图文展陈组件

图文展陈模板应支持以下组件：

| 组件         | 说明                                   |
| ------------ | -------------------------------------- |
| 封面组件     | 展示专题标题、副标题、背景图、编研单位 |
| 导语组件     | 展示专题摘要和编研说明                 |
| 章节组件     | 展示章节标题、正文、引用               |
| 图文卡片组件 | 一张图片配一段说明                     |
| 图片画廊组件 | 多图展示，支持缩略图和大图预览         |
| 档案展品组件 | 展示档案题名、时间、来源、图片、说明   |
| 引用来源组件 | 展示引用文档、页码、原文片段           |
| 附件下载组件 | 提供相关材料下载入口                   |
| 相关推荐组件 | 展示相关专题、相关档案、相关知识       |

## 6.5 内容数据结构

图文展陈成果建议采用以下 JSON 结构：

```json
{
  "resultId": "R202606160001",
  "outputType": "HTML",
  "templateType": "gallery",
  "title": "企业档案数字化建设专题展陈",
  "subtitle": "从纸质档案到智能档案管理",
  "summary": "本专题围绕企业档案数字化建设过程进行梳理，展示关键文件、项目节点和建设成果。",
  "cover": {
    "imageUrl": "/assets/research/cover.jpg",
    "backgroundColor": "#8B1E1E",
    "organization": "某某集团档案中心",
    "publishDate": "2026-06-16"
  },
  "sections": [
    {
      "id": "section_1",
      "title": "一、建设背景",
      "type": "text",
      "content": "这里是章节正文内容……",
      "citations": [
        {
          "sourceId": "doc_001",
          "sourceTitle": "档案数字化建设方案",
          "page": 3,
          "quote": "相关引用片段……"
        }
      ]
    },
    {
      "id": "section_2",
      "title": "二、重点档案展示",
      "type": "gallery",
      "items": [
        {
          "title": "档案数字化项目立项文件",
          "imageUrl": "/assets/research/archive_001.jpg",
          "date": "2021-03-12",
          "sourceTitle": "项目立项审批材料",
          "description": "该文件标志着企业档案数字化建设正式启动。",
          "citationId": "cite_001"
        }
      ]
    }
  ],
  "references": [
    {
      "id": "cite_001",
      "title": "项目立项审批材料",
      "sourceType": "档案文件",
      "page": 1,
      "fileUrl": "/files/doc_001.pdf"
    }
  ]
}
```

## 6.6 模板配置项

图文展陈模板配置示例：

```json
{
  "templateName": "档案图文展陈模板",
  "templateCode": "archive_gallery",
  "outputType": "HTML",
  "templateCategory": "gallery",
  "layoutConfig": {
    "layoutMode": "immersive",
    "pageWidth": "1200px",
    "showSidebarToc": true,
    "fixedToc": true,
    "showCover": true,
    "showSummary": true,
    "showReferences": true
  },
  "styleConfig": {
    "theme": "archive_red",
    "primaryColor": "#8B1E1E",
    "backgroundColor": "#F8F4EC",
    "fontFamily": "Microsoft YaHei, SimSun",
    "titleFontSize": "36px",
    "bodyFontSize": "16px",
    "lineHeight": "1.8"
  },
  "componentConfig": {
    "galleryMode": "grid",
    "imagePreview": true,
    "showImageCaption": true,
    "showArchiveMetadata": true,
    "citationDisplay": "side_panel"
  },
  "exportConfig": {
    "allowHtmlExport": true,
    "allowZipExport": true,
    "allowPrintPdf": true
  }
}
```

## 6.7 图文展陈生成要求

在使用图文展陈模板时，大模型生成内容时应遵循以下要求：

1. 每个章节应生成明确的主题说明；
2. 涉及图片、扫描件、档案材料时，应生成图片说明；
3. 每个展品应包含标题、形成时间、来源、说明；
4. 重要观点必须绑定引用来源；
5. 不允许编造图片来源、档案编号、形成日期；
6. 无法确认的信息应标记为“待核实”；
7. 章节内容要适合网页阅读，段落不宜过长；
8. 图片说明应简洁、客观、可展示。

------

## 11. 时间轴专题 HTML 模板设计

## 7.1 模板定位

时间轴专题模板用于按照时间顺序展示事件发展脉络，适合历史沿革、项目进展、政策演变、人物经历、机构变迁等场景。

适用场景包括：

1. 企业发展历程；
2. 档案馆建设历程；
3. 重大项目建设过程；
4. 制度政策演变；
5. 组织机构调整过程；
6. 领导任免与机构沿革；
7. 产品发展历程；
8. 历史事件专题回顾。

## 7.2 页面结构

时间轴专题模板建议采用以下结构：

```text
封面区
├── 专题标题
├── 副标题
├── 时间范围
└── 编研说明

概览区
├── 专题摘要
├── 关键节点数量
├── 涉及时间跨度
└── 资料来源数量

时间轴导航区
├── 年份导航
├── 阶段筛选
└── 事件类型筛选

时间轴主体区
├── 阶段一
│   ├── 时间节点
│   ├── 事件标题
│   ├── 事件说明
│   ├── 相关图片
│   └── 引用来源
├── 阶段二
└── 阶段三

阶段总结区
├── 阶段特征
├── 关键变化
└── 影响分析

引用来源区
├── 来源文档
├── 原文片段
└── 资料说明
```

## 7.3 时间轴布局类型

时间轴专题模板建议内置三种布局：

### 7.3.1 纵向时间轴

适合长篇历史专题。

```text
2020年
  └── 事件一
2021年
  └── 事件二
2022年
  └── 事件三
```

特点：

1. 阅读自然；
2. 移动端适配好；
3. 适合事件较多的专题。

### 7.3.2 横向时间轴

适合阶段较少、强调整体进程的专题。

```text
2020 → 2021 → 2022 → 2023 → 2024
```

特点：

1. 展示直观；
2. 适合首页概览；
3. 适合大屏或专题首页。

### 7.3.3 分阶段时间轴

适合企业发展、项目建设、政策演进等场景。

```text
第一阶段：启动准备期
第二阶段：建设推进期
第三阶段：深化应用期
第四阶段：智能升级期
```

特点：

1. 能体现阶段特征；
2. 适合编研总结；
3. 适合领导汇报和专题展示。

## 7.4 时间轴组件

时间轴专题模板应支持以下组件：

| 组件           | 说明                             |
| -------------- | -------------------------------- |
| 时间轴封面组件 | 展示专题名称、时间范围、背景图   |
| 时间概览组件   | 展示起止年份、事件数量、资料数量 |
| 年份导航组件   | 按年份快速跳转                   |
| 阶段导航组件   | 按阶段筛选事件                   |
| 时间节点组件   | 展示时间、事件标题、说明         |
| 事件详情组件   | 展示事件正文、图片、引用来源     |
| 阶段总结组件   | 对某一阶段进行归纳分析           |
| 来源引用组件   | 展示支撑该事件的档案资料         |
| 时间轴筛选组件 | 按事件类型、来源类型、关键词筛选 |

## 7.5 内容数据结构

时间轴专题成果建议采用以下 JSON 结构：

```json
{
  "resultId": "R202606160002",
  "outputType": "HTML",
  "templateType": "timeline",
  "title": "企业档案数字化建设历程",
  "subtitle": "2020—2026 年建设脉络梳理",
  "timeRange": {
    "start": "2020",
    "end": "2026"
  },
  "summary": "本专题按照时间顺序梳理企业档案数字化建设的重要节点、阶段特征和建设成果。",
  "overview": {
    "eventCount": 18,
    "stageCount": 4,
    "sourceCount": 26
  },
  "stages": [
    {
      "id": "stage_1",
      "title": "第一阶段：启动准备期",
      "period": "2020—2021",
      "summary": "该阶段主要完成制度准备、项目立项和基础资料摸底。",
      "events": [
        {
          "id": "event_001",
          "date": "2020-05",
          "title": "完成档案数字化建设调研",
          "description": "企业组织开展档案数字化现状调研，形成初步建设方案。",
          "eventType": "调研论证",
          "images": [
            {
              "url": "/assets/research/event_001.jpg",
              "caption": "档案数字化调研材料"
            }
          ],
          "citations": [
            {
              "sourceId": "doc_001",
              "sourceTitle": "档案数字化调研报告",
              "page": 2,
              "quote": "相关引用片段……"
            }
          ]
        }
      ]
    }
  ],
  "references": [
    {
      "id": "doc_001",
      "title": "档案数字化调研报告",
      "sourceType": "内部资料",
      "date": "2020-05",
      "fileUrl": "/files/doc_001.pdf"
    }
  ]
}
```

## 7.6 模板配置项

时间轴专题模板配置示例：

```json
{
  "templateName": "时间轴专题模板",
  "templateCode": "timeline_topic",
  "outputType": "HTML",
  "templateCategory": "timeline",
  "layoutConfig": {
    "timelineMode": "vertical",
    "showCover": true,
    "showOverview": true,
    "showYearNav": true,
    "showStageNav": true,
    "showEventFilter": true,
    "showStageSummary": true
  },
  "styleConfig": {
    "theme": "enterprise_blue",
    "primaryColor": "#1F4E79",
    "backgroundColor": "#F5F7FA",
    "fontFamily": "Microsoft YaHei, SimSun",
    "timelineLineStyle": "solid",
    "eventCardStyle": "card"
  },
  "componentConfig": {
    "eventDisplay": "card",
    "citationDisplay": "collapse",
    "imageDisplay": "thumbnail",
    "allowExpandEvent": true,
    "allowCollapseStage": true
  },
  "exportConfig": {
    "allowHtmlExport": true,
    "allowZipExport": true,
    "allowPrintPdf": true
  }
}
```

## 7.7 时间轴生成要求

在使用时间轴专题模板时，大模型生成内容应遵循以下要求：

1. 必须优先从知识库、主题库和引用资料中抽取真实时间信息；
2. 不得编造时间、事件、人物、机构名称；
3. 对时间不明确的事件，应标注为“时间待核实”；
4. 时间节点应按时间升序排列；
5. 每个事件应包含事件标题、事件说明、资料来源；
6. 重要事件应生成影响分析或阶段意义；
7. 可以按年份、阶段、事件类型进行归类；
8. 同一事件如果来源冲突，应标记“来源存在差异，需人工确认”。

------

# 12. 版式配置设计（DOCX 为主）

## 12.1 版式配置入口

在「编研模板」模块中增加「输出版式」配置页。

```text
基础信息 | 章节结构 | 工作流绑定 | 输出版式 | 预览
```

「输出版式」Tab 结构（**DOCX 置顶**）：

```text
DOCX 版式（默认输出）          ← 主配置
├── 普通专题报告
├── 图文展陈报告
├── 时间轴专题报告
└── 简报

HTML 版式（在线展示，可选）    ← 辅配置
├── 普通专题页
├── 图文展陈页
└── 时间轴专题页

导出选项
├── 默认下载格式：DOCX
├── 是否生成 HTML 预览
└── 是否允许打印 PDF
```

## 12.2 DOCX 版式配置项（主）

### 12.2.1 母版与页面

| 配置项 | 说明 |
| ------ | ---- |
| docx 母版文件 | 上传 `.docx`，含 RW_* 样式 |
| 纸张 / 页边距 | A4、上下左右边距（cm） |
| 行距 | 正文固定行距（磅） |
| 是否生成封面 | |
| 是否生成目录 | 及目录层级 |
| 页眉页脚文案 | 支持 `{{title}}` 等占位符 |
| 页码格式 | 无 / 阿拉伯 / 罗马（目录节） |
| 分节策略 | 封面、目录、正文、参考文献 |

### 12.2.2 样式映射

| 配置项 | 说明 |
| ------ | ---- |
| styleMapping | content 节点类型 → Word 样式名 |
| 单位自定义字体 | 覆盖母版字体，保留样式 ID |
| 标题编号 | 是否自动编号（一、（一）、1.） |

### 12.2.3 组件与版式

| 配置项 | 说明 |
| ------ | ---- |
| 引用展示 | 脚注 / 尾注 / 文末参考文献 |
| 图片布局 | 居中单图 / 双图并排 / 表格展品 |
| 时间轴布局 | table / narrative / staged（见 8.5） |
| 表格样式 | 边框、表头底色 |

### 12.2.4 关联 HTML 模板

| 配置项 | 说明 |
| ------ | ---- |
| linkedHtmlTemplateId | 可选，绑定对应 HTML 衍生模板 |
| autoRenderHtml | 生成 DOCX 后是否自动渲染 HTML 预览 |

## 12.3 HTML 版式配置项（辅）

HTML 版式配置分为以下几类：

### 12.3.1 页面布局配置

| 配置项       | 说明                                     |
| ------------ | ---------------------------------------- |
| 页面宽度     | 设置内容区域宽度，如 960px、1200px、100% |
| 是否显示封面 | 控制是否生成专题封面                     |
| 是否显示摘要 | 控制是否显示导语或摘要                   |
| 是否显示目录 | 控制是否显示章节目录                     |
| 目录位置     | 左侧、顶部、右侧、隐藏                   |
| 是否固定目录 | 滚动时目录是否固定                       |
| 是否响应式   | 是否适配移动端                           |
| 内容布局     | 单栏、双栏、卡片式、沉浸式               |

### 12.3.2 主题样式配置

| 配置项   | 说明                               |
| -------- | ---------------------------------- |
| 主题风格 | 政务蓝、档案红、简洁灰、企业品牌色 |
| 主色     | 页面主色                           |
| 背景色   | 页面背景色                         |
| 标题字体 | 标题字体                           |
| 正文字体 | 正文字体                           |
| 正文字号 | 默认正文大小                       |
| 行高     | 正文行距                           |
| 卡片圆角 | 卡片样式                           |
| 阴影效果 | 是否显示卡片阴影                   |

### 12.3.3 组件配置

| 配置项         | 说明                             |
| -------------- | -------------------------------- |
| 图片展示方式   | 单图、网格、轮播、瀑布流         |
| 时间轴展示方式 | 纵向、横向、分阶段               |
| 引用展示方式   | 脚注、侧边栏、折叠面板、悬浮卡片 |
| 表格展示方式   | 普通表格、紧凑表格、可横向滚动   |
| 图表展示方式   | 静态图表、ECharts 图表           |
| 附件展示方式   | 链接、按钮、卡片                 |

### 12.3.4 导出配置

| 配置项             | 说明                       |
| ------------------ | -------------------------- |
| 是否允许 HTML 导出 | 允许下载单 HTML 文件       |
| 是否允许 ZIP 导出  | HTML + CSS + 图片资源打包  |
| 是否允许打印 PDF   | 通过浏览器或服务端生成 PDF |
| 是否允许公开发布   | 生成可访问链接             |
| 是否允许嵌入门户   | 生成 iframe 或嵌入代码     |

------

# 13. 智能编研向导调整

现有智能编研向导为五步：

1. 编研主题；
2. 主题库；
3. 知识库；
4. 编研模板；
5. 生成成果。

增加 HTML 版式后，建议调整为：

```text
步骤 1：编研主题
步骤 2：主题库
步骤 3：知识库
步骤 4：编研模板
步骤 5：输出版式
步骤 6：生成成果
```

如果不想增加步骤，也可以在第 4 步“编研模板”中选择默认输出模板，在第 5 步生成成果后允许切换。

推荐本期采用：

```text
保持五步不变，在第 5 步生成成果页面增加“版式切换”能力。
```

原因：

1. 对用户操作影响小；
2. 不破坏现有流程；
3. 生成后再切换版式更直观；
4. 同一份内容可以导出多种成果形式。

------

# 14. 生成成果页面设计

## 14.1 页面布局

生成成果页面建议采用三栏布局：

```text
左侧：章节目录 / 时间轴导航
中间：HTML 成果预览
右侧：成果操作 / 版式配置 / 引用来源
```

### 左侧区域

根据模板类型显示不同导航：

普通报告模板：

```text
章节目录
一、背景概述
二、发展脉络
三、资料分析
四、结论建议
```

图文展陈模板：

```text
展陈目录
专题导语
重点档案
图文资料
资料解读
引用来源
```

时间轴专题模板：

```text
时间导航
2020年
2021年
2022年
2023年

阶段导航
启动准备期
建设推进期
深化应用期
智能升级期
```

### 中间区域

显示 HTML 成果预览。

支持：

1. 实时预览；
2. 滚动阅读；
3. 图片大图预览；
4. 时间轴展开收起；
5. 引用来源点击查看；
6. 移动端预览切换。

### 右侧区域

右侧区域包括：

```text
成果信息
├── 成果标题
├── 当前模板
├── 生成时间
└── 任务状态

版式切换
├── DOCX：普通专题 / 图文展陈 / 时间轴 / 简报
└── HTML：对应衍生模板（可选）

导出操作
├── 下载 DOCX（默认）
├── 下载 Markdown
├── 导出 HTML / ZIP
├── 打印 PDF
└── 发布专题

引用来源
├── 来源文档
├── 引用片段
├── 页码
└── 查看原文
```

## 14.2 操作按钮

生成成果页应提供以下操作：

| 操作 | 说明 |
| ---- | ---- |
| 切换 DOCX 版式 | 同一份成果切换不同 Word 模板（主） |
| 下载 DOCX | 下载正式 Word 文件 |
| 预览 DOCX | 在线预览（转换预览或插件） |
| 切换 HTML 版式 | 衍生在线模板 |
| 预览 HTML | 查看网页效果 |
| 重新生成章节 | 针对某章节重新生成 |
| 导出 HTML / ZIP | 在线专题打包 |
| 打印 PDF | 浏览器或服务端生成 |
| 发布专题 | HTML 在线地址 |
| 查看引用 | 资料来源 |

------

# 15. 数据库设计

## 15.1 编研成果模板表

表名：`research_output_template`

| 字段 | 类型 | 说明 |
| ---- | ---- | ---- |
| id | bigint | 主键 |
| template_name | varchar | 模板名称 |
| template_code | varchar | 模板编码 |
| output_type | varchar | **DOCX / HTML / MARKDOWN / PDF**（默认 DOCX） |
| template_category | varchar | report / gallery / timeline / briefing |
| description | varchar | 说明 |
| **docx_config** | json | **DOCX 母版路径、边距、目录、样式映射** |
| layout_config | json | HTML 页面布局 |
| style_config | json | HTML 主题样式 |
| component_config | json | 组件配置 |
| export_config | json | 导出开关 |
| **template_file_path** | varchar | **DOCX 母版 `.docx` 路径（主）** |
| linked_html_template_id | bigint | 关联 HTML 衍生模板 ID |
| preview_image | varchar | 预览图 |
| status | varchar | enabled / disabled |
| created_by | bigint | 创建人 |
| created_time | datetime | 创建时间 |
| updated_time | datetime | 更新时间 |

## 15.2 编研成果表

表名：`research_compile_result`

| 字段                       | 类型     | 说明            |
| -------------------------- | -------- | --------------- |
| id                         | bigint   | 主键            |
| task_id                    | bigint   | 编研任务 ID     |
| title                      | varchar  | 成果标题        |
| topic                      | varchar  | 编研主题        |
| summary                    | text     | 成果摘要        |
| content_markdown           | longtext | Markdown 内容   |
| content_json | json | 结构化成果（DOCX/HTML 共用） |
| **content_docx_path** | varchar | **生成的 DOCX 文件路径** |
| default_output_template_id | bigint | 默认 **DOCX** 输出模板 |
| status                     | varchar  | 状态            |
| created_time               | datetime | 创建时间        |
| updated_time               | datetime | 更新时间        |

## 15.3 DOCX 渲染结果表

表名：`research_docx_render_result`

| 字段 | 类型 | 说明 |
| ---- | ---- | ---- |
| id | bigint | 主键 |
| result_id | bigint | 编研成果 ID |
| output_template_id | bigint | DOCX 模板 ID |
| docx_path | varchar | 生成的 `.docx` 路径 |
| render_status | varchar | SUCCEEDED / FAILED |
| render_error | text | 错误信息 |
| file_size | bigint | 文件大小 |
| created_time | datetime | 创建时间 |

## 15.4 HTML 渲染结果表

表名：`research_html_render_result`

| 字段               | 类型     | 说明          |
| ------------------ | -------- | ------------- |
| id                 | bigint   | 主键          |
| result_id          | bigint   | 编研成果 ID   |
| output_template_id | bigint   | 输出模板 ID   |
| html_path          | varchar  | HTML 文件路径 |
| zip_path           | varchar  | ZIP 包路径    |
| pdf_path           | varchar  | PDF 文件路径  |
| render_status      | varchar  | 渲染状态      |
| render_error       | text     | 错误信息      |
| publish_status     | varchar  | 发布状态      |
| publish_url        | varchar  | 发布访问地址  |
| created_time       | datetime | 创建时间      |
| updated_time       | datetime | 更新时间      |

## 15.5 编研引用来源表

表名：`research_citation`

| 字段         | 类型     | 说明        |
| ------------ | -------- | ----------- |
| id           | bigint   | 主键        |
| result_id    | bigint   | 编研成果 ID |
| section_id   | varchar  | 所属章节 ID |
| source_id    | varchar  | 来源文档 ID |
| source_title | varchar  | 来源标题    |
| source_type  | varchar  | 来源类型    |
| page_no      | varchar  | 页码        |
| quote_text   | text     | 引用片段    |
| file_url     | varchar  | 原文地址    |
| created_time | datetime | 创建时间    |

------

# 16. 接口设计

## 16.1 查询输出模板列表

```text
GET /api/research/output-templates?outputType=DOCX&templateCategory=report
```

返回示例（DOCX 优先）：

```json
{
  "code": 0,
  "data": [
    {
      "id": 1,
      "templateName": "档案编研普通报告",
      "templateCode": "archive_report_docx",
      "outputType": "DOCX",
      "templateCategory": "report",
      "linkedHtmlTemplateId": 10,
      "previewImage": "/assets/templates/archive_report_docx.png"
    }
  ]
}
```

## 16.2 渲染 DOCX 成果（主）

```text
POST /api/research/results/{resultId}/render-docx
```

请求：

```json
{
  "outputTemplateId": 1,
  "renderMode": "preview"
}
```

响应：

```json
{
  "code": 0,
  "data": {
    "renderId": 1001,
    "downloadUrl": "/api/research/outputs/{outputId}/docx",
    "status": "SUCCEEDED"
  }
}
```

## 16.3 下载 DOCX

```text
GET /api/research/outputs/{outputId}/docx
```

（与现有 `ResearchController` 对齐，增强为模板化渲染产物。）

## 16.4 切换 DOCX 版式

```text
POST /api/research/results/{resultId}/switch-docx-template
```

```json
{ "outputTemplateId": 2 }
```

## 16.5 渲染 HTML 成果（辅）

请求参数：

```json
{
  "outputTemplateId": 1,
  "renderMode": "preview"
}
```

返回示例：

```json
{
  "code": 0,
  "data": {
    "renderId": 1001,
    "previewUrl": "/research/preview/1001",
    "status": "SUCCEEDED"
  }
}
```

## 16.6 切换 HTML 版式

```text
POST /api/research/results/{resultId}/switch-html-template
```

```json
{ "outputTemplateId": 10 }
```

## 16.7 导出 HTML

```text
POST /api/research/results/{resultId}/export-html
```

## 16.8 打印 PDF

```text
POST /api/research/results/{resultId}/export-pdf
```

```json
{ "outputTemplateId": 1, "paperSize": "A4", "source": "DOCX" }
```

`source`：`DOCX`（优先）或 `HTML`。

------

# 17. 技术实现方案

## 17.1 内容生成

（同前，工作流输出 Markdown + 章节 JSON + 引用 + 图片 + 时间线。）

## 17.2 结构化转换

```text
工作流生成 Markdown + 局部 JSON
        ↓
后端解析和校验 → content_json
        ↓
┌─────────────────┬─────────────────┐
│ DOCX 渲染（主）  │ HTML 渲染（辅）  │
└─────────────────┴─────────────────┘
```

## 17.3 DOCX 渲染（主）

### 17.3.1 当前实现

`ResearchDocxExporter`：基于 Apache POI，将 Markdown 按行解析为标题/正文（简化版）。

### 17.3.2 目标实现

| 组件 | 职责 |
| ---- | ---- |
| `ResearchDocxTemplateEngine` | 加载 `.docx` 母版，填充占位符 |
| `ResearchDocxStyleRenderer` | 按 `styleMapping` 写入段落、标题、脚注 |
| `ResearchDocxGalleryRenderer` | 图文章节：图片+题注+展品表 |
| `ResearchDocxTimelineRenderer` | 时间轴：表格式/叙述式 |
| `ResearchDocxCitationRenderer` | 脚注与参考文献节 |

渲染顺序：封面 → 目录域 → 正文各 section → 参考文献 → 分节页码。

### 17.3.3 依赖

- Apache POI `XWPFDocument`（已有）
- 可选 docx4j（复杂域、TOC 更新）
- 图片：从 `imageRef` 读取字节流嵌入

## 17.4 HTML 前端渲染（辅）

```text
Vue / React
Markdown-it / marked
ECharts
图片预览组件
虚拟滚动，可选
CSS Variables
响应式布局
```

组件结构：

```text
ResearchHtmlRenderer
├── ResearchCover
├── ResearchSummary
├── ResearchToc
├── ResearchSection
├── ResearchGallery
├── ResearchTimeline
├── ResearchCitationPanel
├── ResearchReferenceList
└── ResearchFooter
```

## 17.5 HTML 导出

HTML 导出支持两种方式：

### 17.5.1 单文件 HTML

将 CSS、必要 JS、内容 JSON 内嵌到一个 HTML 文件中。

优点：

1. 下载方便；
2. 便于离线查看；
3. 便于归档。

缺点：

1. 图片 Base64 后文件较大；
2. 不适合超大图文专题。

### 17.5.2 ZIP 打包

将以下资源打包：

```text
index.html
assets/css/style.css
assets/js/app.js
assets/images/*
assets/files/*
```

优点：

1. 适合图文展陈；
2. 图片资源独立；
3. 便于部署到静态服务器。

## 17.6 PDF 导出

优先：**由 DOCX 打印为 PDF**（保真）或浏览器打印 HTML 预览。

本期：浏览器打印；后续：LibreOffice/服务端将 DOCX 转 PDF。

------

# 18. 权限与安全设计

## 18.1 权限控制

HTML 成果涉及编研内容、引用材料和图片资源，必须纳入资产授权体系。

权限控制包括：

1. 谁可以查看编研成果；
2. 谁可以切换版式；
3. 谁可以导出 HTML；
4. 谁可以下载 ZIP；
5. 谁可以打印 PDF；
6. 谁可以发布专题；
7. 谁可以查看引用原文；
8. 谁可以管理输出模板。

## 18.2 发布控制

专题发布应支持以下状态：

```text
未发布
已发布
已下线
已过期
```

发布配置包括：

1. 访问范围；
2. 是否需要登录；
3. 是否允许外部访问；
4. 是否允许下载附件；
5. 是否显示引用来源；
6. 是否允许搜索引擎索引。

## 18.3 内容安全

系统应对 HTML 输出进行安全处理：

1. 过滤危险 HTML 标签；
2. 防止 XSS；
3. 限制外链脚本；
4. 图片和附件统一走文件访问鉴权；
5. 用户输入内容进行转义；
6. 禁止模型直接生成可执行脚本；
7. 发布前支持人工审核。

------

# 19. 本期建设范围建议

## 19.1 本期建议实现

1. **DOCX 输出模板表与母版机制**；
2. 内置普通专题 / 图文展陈 / 时间轴 **DOCX 模板**；
3. `content_json` 结构化存储；
4. **DOCX 模板化渲染**（升级 `ResearchDocxExporter`）；
5. 生成成果页 **默认 DOCX 下载与预览**；
6. 关联 HTML 衍生模板与在线预览；
7. HTML / ZIP 导出、浏览器打印 PDF；
8. 引用脚注与参考文献节。

## 19.2 暂不建议本期实现

以下能力建议后续版本实现：

1. 可视化拖拽版式设计器；
2. 在线 Word 编辑器；
3. 多人协同编辑；
4. 复杂动画展陈；
5. 三维展厅；
6. 全量专题门户；
7. 复杂权限水印；
8. 服务端高保真 PDF 排版；
9. 跨成果专题聚合；
10. 自动生成宣传海报。

------

# 20. 典型业务流程

## 20.1 图文展陈成果生成流程

```text
用户进入智能编研 → 选择资料与模板
        ↓
系统生成 content_json
        ↓
默认渲染 DOCX 图文展陈版式 → 下载 Word 报送/归档
        ↓
（可选）渲染 HTML 展陈页 → 预览 / 发布
```

## 20.2 时间轴专题成果生成流程

```text
用户进入智能编研 → 选择时间轴类模板
        ↓
系统抽取时间、事件、来源 → content_json
        ↓
渲染 DOCX（表格式/分阶段）→ 下载
        ↓
（可选）HTML 时间轴预览 → 导出 PDF / 发布
```

------

# 21. 页面原型说明

## 21.1 编研模板 - 输出版式页

```text
输出版式配置
├── 默认输出：DOCX（主）
├── DOCX 模板选择
│   ├── 普通专题报告
│   ├── 图文展陈报告
│   └── 时间轴专题报告
├── DOCX 母版上传 / 页面边距 / 样式映射
├── 关联 HTML 模板（可选）
└── HTML 主题与布局（辅）
```

## 21.2 智能编研 - 生成成果页

页面结构：

```text
顶部操作区
├── 当前成果标题
├── 当前版式
├── 切换版式
├── 导出 HTML
├── 下载 DOCX
├── 打印 PDF
└── 发布专题

左侧导航区
├── 章节目录
├── 年份导航
└── 阶段导航

中间预览区
├── HTML 成果预览
├── 图文展陈内容
└── 时间轴内容

右侧信息区
├── 成果信息
├── 引用来源
├── 样式配置
└── 导出记录
```

------

# 22. 验收标准

## 22.1 DOCX 普通报告验收（主）

1. 默认输出为 DOCX；
2. 含封面、目录、章节标题层级、正文、参考文献；
3. 样式符合配置（仿宋正文、黑体标题等）；
4. 页眉页脚、页码正确；
5. 引用以脚注或参考文献列出；
6. 可切换 DOCX 版式后重新生成。

## 22.2 DOCX 图文展陈验收

1. 图文章节含图片、题注、说明；
2. 展品元数据表（档号、时间、来源）正确；
3. Word 可正常打开、打印。

## 22.3 DOCX 时间轴验收

1. 事件按时间升序；
2. 表格式/叙述式版式可配置；
3. 分阶段结构正确。

## 22.4 HTML 衍生版式验收（辅）

1. 可以选择图文展陈模板生成 HTML 成果；
2. 页面包含封面、摘要、章节、图片、引用来源；
3. 图片支持缩略图和大图预览；
4. 图文卡片可以展示标题、说明、来源；
5. 引用来源可以点击查看；
6. 页面支持响应式展示；
7. 可以导出 HTML；
8. 可以打包 ZIP；
9. 可以打印 PDF。

## 22.5 通用验收

1. 同一份 `content_json` 可输出 DOCX 与 HTML；
2. DOCX 版式为主配置，HTML 为可选衍生；
3. 引用与权限控制有效。

------

# 23. 总结

智能编研成果交付**以 DOCX 为主、HTML 为辅**：DOCX 用于正式报送、归档与打印；HTML 用于在线展陈与专题发布。二者共用 `content_json`，版式语义以 DOCX 章节结构为基准。

本期优先：

1. DOCX 模板体系与母版渲染；
2. 普通 / 图文 / 时间轴三类 DOCX 版式；
3. 成果结构化与 DOCX 下载；
4. HTML 衍生预览与发布。

```text
资料检索 → 智能生成 → content_json
        ↓
   DOCX 渲染（主）→ 下载 / 归档 / 打印
        ↓
   HTML 渲染（辅）→ 预览 / 展陈 / 发布
```