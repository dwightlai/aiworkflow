# -*- coding: utf-8 -*-
from pathlib import Path
from docx import Document
from docx.enum.text import WD_ALIGN_PARAGRAPH
from docx.oxml.ns import qn
from docx.shared import Cm, Pt, RGBColor

ROOT = Path(__file__).resolve().parent
IMG = ROOT / "images" / "manual"
OUT = ROOT / "aiworkflow-system-design.docx"


def set_cn_font(run, name="宋体", size=12, bold=False):
    run.font.name = name
    run._element.rPr.rFonts.set(qn("w:eastAsia"), name)
    run.font.size = Pt(size)
    run.font.bold = bold


def para(doc, text, size=12, bold=False, align=None, space_after=6):
    p = doc.add_paragraph()
    if align:
        p.alignment = align
    r = p.add_run(text)
    set_cn_font(r, size=size, bold=bold)
    p.paragraph_format.space_after = Pt(space_after)
    return p


def heading(doc, text, level=1):
    p = doc.add_heading(text, level=level)
    for r in p.runs:
        set_cn_font(r, name="黑体", size=16 if level == 1 else 14, bold=True)
    return p


def table(doc, headers, rows, col_widths=None):
    t = doc.add_table(rows=1 + len(rows), cols=len(headers))
    t.style = "Table Grid"
    for i, h in enumerate(headers):
        c = t.rows[0].cells[i]
        c.text = h
        for p in c.paragraphs:
            for r in p.runs:
                set_cn_font(r, bold=True, size=10)
    for ri, row in enumerate(rows):
        for ci, val in enumerate(row):
            c = t.rows[ri + 1].cells[ci]
            c.text = str(val)
            for p in c.paragraphs:
                for r in p.runs:
                    set_cn_font(r, size=10)
    if col_widths:
        for row in t.rows:
            for i, w in enumerate(col_widths):
                row.cells[i].width = Cm(w)
    doc.add_paragraph()
    return t


def image(doc, name, caption, width_cm=15):
    path = IMG / name
    if not path.exists():
        para(doc, f"[缺少截图: {name}]", size=10)
        return
    p = doc.add_paragraph()
    p.alignment = WD_ALIGN_PARAGRAPH.CENTER
    r = p.add_run()
    r.add_picture(str(path), width=Cm(width_cm))
    cap = doc.add_paragraph()
    cap.alignment = WD_ALIGN_PARAGRAPH.CENTER
    cr = cap.add_run(caption)
    set_cn_font(cr, size=10)
    cap.paragraph_format.space_after = Pt(12)


def module_section(doc, no, title, screenshot, caption, elements, rules, logic=None):
    heading(doc, f"5.{no} {title}", level=2)
    heading(doc, f"5.{no}.1 列表/主界面", level=3)
    heading(doc, f"5.{no}.1.1 页面原型", level=4)
    image(doc, screenshot, caption)
    heading(doc, f"5.{no}.1.2 页面元素", level=4)
    table(doc, ["元素名称", "类型", "说明"], elements, [4, 2.5, 9])
    heading(doc, f"5.{no}.1.3 规则说明", level=4)
    for line in rules:
        para(doc, line, size=11)
    if logic:
        heading(doc, f"5.{no}.1.4 交互逻辑", level=4)
        for line in logic:
            para(doc, line, size=11)


def main():
    doc = Document()
    sec = doc.sections[0]
    sec.page_height = Cm(29.7)
    sec.page_width = Cm(21)
    sec.left_margin = Cm(2.5)
    sec.right_margin = Cm(2.5)
    sec.top_margin = Cm(2.5)
    sec.bottom_margin = Cm(2.5)

    # 封面
    para(doc, "密级：内部公开", size=12, align=WD_ALIGN_PARAGRAPH.CENTER)
    para(doc, "『AI Workflow 企业 AI 应用平台』", size=14, bold=True, align=WD_ALIGN_PARAGRAPH.CENTER, space_after=12)
    para(doc, "『AI Workflow 管理端』", size=14, bold=True, align=WD_ALIGN_PARAGRAPH.CENTER, space_after=24)
    para(doc, "软件设计说明书", size=22, bold=True, align=WD_ALIGN_PARAGRAPH.CENTER, space_after=36)

    table(doc, ["签字", "日期"], [["", ""], ["", ""]], [8, 8])
    table(
        doc,
        ["版本号", "简要说明", "变更人", "变更日期", "复核人"],
        [["1.0", "新建", "", "2026-06-11", ""]],
        [2, 5, 3, 3, 3],
    )
    doc.add_page_break()

    # 目录（手工）
    heading(doc, "目录", 1)
    toc = [
        "1 概述", "2 业务场景", "3 用户角色", "4 业务流程",
        "5 应用功能设计", "  5.1 全局规则", "  5.2 功能清单",
        "  5.3 登录与工作台", "  5.4 智能体 Bots", "  5.5 工作流",
        "  5.6 工作流设计器", "  5.7 运行监控", "  5.8 知识库",
        "  5.9 模型配置", "  5.10 编研模板", "  5.11 智能编研",
        "  5.12 系统管理", "6 系统架构设计",
    ]
    for item in toc:
        para(doc, item, size=11)
    doc.add_page_break()

    # 1 概述
    heading(doc, "1 概述", 1)
    para(
        doc,
        "本说明书依据当前 AI Workflow 管理端已实现功能编写，描述系统定位、业务场景、用户角色、"
        "核心业务流程及各功能模块的页面原型、页面元素与业务规则，供产品、研发、测试与实施人员使用。",
        size=11,
    )
    para(
        doc,
        "AI Workflow 是面向企业的 AI 应用搭建平台，提供模型配置、知识库、工作流编排、智能体封装、"
        "运行监控、智能编研、资产授权与多租户组织权限管理。前后端分离：后端 Spring Boot 3.3 + PostgreSQL/达梦，"
        "前端 React 18 + Ant Design 5 管理控制台。",
        size=11,
    )

    # 2 业务场景
    heading(doc, "2 业务场景", 1)
    table(
        doc,
        ["场景编号", "场景名称", "说明"],
        [
            ["S01", "知识库问答", "创建知识库并上传资料，编排检索+大模型工作流，封装为智能体对外服务"],
            ["S02", "流程自动化", "通过 DAG 编排 HTTP、条件分支、循环等节点，对接业务 API"],
            ["S03", "问题分类分流", "问题分类节点按关键词/模型路由到不同处理分支"],
            ["S04", "档案智能编研", "编研模板定义章节，向导选择资料源与知识库，工作流生成专题成果"],
            ["S05", "多租户运营", "平台管理员创建租户，租户内维护组织、用户、资产授权"],
            ["S06", "第三方集成", "业务系统通过开放 API（AppCode+ApiKey）调用 Bot/工作流/知识库"],
        ],
        [2, 4, 10],
    )

    # 3 用户角色
    heading(doc, "3 用户角色", 1)
    table(
        doc,
        ["角色", "编码", "职责"],
        [
            ["平台管理员", "platform_admin", "租户管理、系统菜单/字典、全局审计、跨租户运维"],
            ["单位管理员", "unit_admin", "本租户组织用户、资产授权、业务资源配置"],
            ["业务配置员", "configurator", "工作流、知识库、Bot、模型、编研模板配置"],
            ["审计员", "auditor", "查看日志与运行记录，不可修改业务数据"],
            ["第三方应用", "integration_app", "通过开放 API 调用已授权资产，无管理端 UI"],
        ],
        [3, 4, 9],
    )

    # 4 业务流程
    heading(doc, "4 业务流程", 1)
    heading(doc, "4.1 流程列表", 2)
    table(
        doc,
        ["流程编号", "流程名称", "触发方式"],
        [
            ["P01", "用户登录", "管理端输入账号密码"],
            ["P02", "工作流发布", "设计器保存草稿后点击发布"],
            ["P03", "工作流执行", "设计器调试、Bot 对话、开放 API 触发"],
            ["P04", "工作流归档/恢复", "工作流卡片归档；已归档 tab 恢复发布"],
            ["P05", "智能编研", "智能编研五步向导提交任务"],
            ["P06", "资产授权", "资产授权页为组织/部门/角色配置 USE 权限"],
        ],
        [2, 4, 8],
    )

    heading(doc, "4.2 工作流生命周期", 2)
    heading(doc, "4.2.1 流程图形", 3)
    para(doc, "创建(DRAFT) → 编辑编排 → 保存草稿 → 发布(PUBLISHED) → 运行/绑定Bot → 归档(ARCHIVED) → 恢复发布", size=11)
    heading(doc, "4.2.2 节点定义", 3)
    table(
        doc,
        ["状态", "说明", "可执行操作"],
        [
            ["DRAFT", "草稿，可编辑", "编辑、发布、删除"],
            ["PUBLISHED", "已发布，可被 Bot/API 调用", "运行、编辑(新草稿)、归档"],
            ["ARCHIVED", "已归档，列表隐藏", "恢复发布、删除"],
        ],
        [3, 5, 6],
    )
    heading(doc, "4.2.3 规则说明", 3)
    for r in [
        "发布前 DAG 须通过校验：唯一 START、至少一个 END、无环、边引用有效。",
        "恢复发布：若曾有发布版本则回到 PUBLISHED，否则回到 DRAFT。",
        "已发布版本不可修改，再次发布生成新版本号。",
    ]:
        para(doc, r, size=11)

    heading(doc, "4.3 智能体对话流程", 2)
    para(doc, "用户消息 → BotService →（绑定工作流则执行 DAG / 直连则调用 LLM / 可选 RAG 检索）→ 保存会话 → 返回回复", size=11)

    # 5 应用功能设计
    heading(doc, "5 应用功能设计", 1)

    heading(doc, "5.1 全局规则", 2)
    heading(doc, "5.1.1 删除规则", 3)
    para(doc, "删除操作需二次确认；被工作流引用的知识库、被 Bot 绑定的工作流删除前系统提示依赖关系。", size=11)
    heading(doc, "5.1.2 启用/禁用规则", 3)
    para(doc, "智能体、模型、编研模板等支持启用/禁用；禁用后不在下拉选择列表中出现，已有绑定关系提示处理。", size=11)
    heading(doc, "5.1.3 翻页设置", 3)
    para(doc, "列表默认分页展示，支持切换每页条数；工作流卡片页为卡片网格不分页。", size=11)
    heading(doc, "5.1.4 列表数据筛选", 3)
    para(doc, "工作流、运行监控、资产授权等支持名称/ID 筛选；工作流支持全部/草稿/已发布/已归档 Tab 过滤，输入即生效。", size=11)

    heading(doc, "5.2 功能清单", 2)
    table(
        doc,
        ["序号", "模块", "路由", "主要功能"],
        [
            ["1", "登录", "/login", "租户编码、用户名密码登录"],
            ["2", "工作台", "/", "统计概览、最近工作流、快捷入口"],
            ["3", "智能体 Bots", "/bots", "CRUD、运行、对话、绑定工作流/模型/知识库"],
            ["4", "工作流", "/workflows", "卡片列表、创建、导入 DSL、归档/恢复"],
            ["5", "工作流设计器", "/workflows/{id}/designer", "节点编排、调试、发布"],
            ["6", "运行监控", "/workflow-runs", "执行记录、节点明细"],
            ["7", "知识库", "/knowledge", "知识库 CRUD、文档、向量库配置"],
            ["8", "模型配置", "/models", "对话/嵌入/多模态模型"],
            ["9", "编研模板", "/research/templates", "章节 schema、绑定工作流"],
            ["10", "智能编研", "/research/compile", "五步向导、成果下载"],
            ["11", "租户管理", "/system/tenants", "租户 CRUD、工作台"],
            ["12", "组织用户", "/system/users", "组织树、用户、角色"],
            ["13", "资产授权", "/system/asset-grants", "Bot/知识库/工作流/模型授权"],
            ["14", "菜单/字典/日志", "/system/*", "系统配置与审计"],
        ],
        [1.2, 3, 4.5, 7],
    )

    # 5.3 登录与工作台
    heading(doc, "5.3 登录与工作台", 2)
    heading(doc, "5.3.1 登录页", 3)
    heading(doc, "5.3.1.1 页面原型", 4)
    image(doc, "login.png", "图 5-1 登录页")
    table(doc, ["元素名称", "类型", "说明"], [
        ["租户编码", "输入框", "可选，留空登录默认租户"],
        ["用户名", "输入框", "必填"],
        ["密码", "密码框", "必填"],
        ["登录", "按钮", "提交认证，成功跳转工作台"],
    ], [4, 2.5, 9])
    heading(doc, "5.3.2 工作台", 3)
    heading(doc, "5.3.2.1 页面原型", 4)
    image(doc, "dashboard.png", "图 5-2 工作台")
    table(doc, ["元素名称", "类型", "说明"], [
        ["统计卡片", "卡片组", "总工作流、今日执行、成功率、平均耗时"],
        ["最近工作流", "列表", "快捷进入编辑/调试"],
        ["运行态势", "图表", "成功/失败/运行中占比"],
        ["快捷入口", "按钮组", "运行监控、新建工作流、集成指南"],
    ], [4, 2.5, 9])

    module_section(doc, 4, "智能体 Bots", "bots.png", "图 5-3 智能体列表",
        [
            ["新增智能体", "按钮", "打开表单"],
            ["搜索", "输入框", "按名称过滤"],
            ["智能体清单", "表格", "名称、应用方式、模型、知识库、会话数、状态"],
            ["运行/编辑/删除", "操作", "行级操作"],
        ],
        [
            "应用方式：绑定工作流 或 直连智能体（模型+可选多知识库）。",
            "仅已发布工作流出现在绑定列表。",
            "启用状态 Bot 方可被开放 API 调用（需授权）。",
        ],
        ["点击运行打开对话抽屉；编辑保存后列表刷新。"],
    )

    module_section(doc, 5, "工作流", "workflows.png", "图 5-4 工作流运营台",
        [
            ["创建工作流/导入 DSL", "按钮", "新建或导入"],
            ["状态 Tab", "标签", "全部/草稿/已发布/已归档"],
            ["搜索框", "输入", "按名称实时过滤"],
            ["工作流卡片", "卡片", "名称、版本、状态、操作区"],
            ["设置/运行/编辑/归档/删除", "按钮", "卡片底部操作"],
        ],
        [
            "空白工作流卡片快速创建。",
            "归档后进入已归档 Tab；恢复发布回到已发布或草稿。",
            "卡片展示最新版本号与更新时间。",
        ],
    )

    module_section(doc, 6, "工作流设计器", "workflow-designer.png", "图 5-5 工作流设计器",
        [
            ["节点库", "面板", "12 种节点类型"],
            ["画布", "SVG", "拖拽、连线、缩放、自动布局"],
            ["属性/调试", "侧栏", "节点配置与调试 JSON"],
            ["保存草稿/发布/运行", "按钮", "版本与执行"],
        ],
        [
            "节点类型：START、END、LLM、PROMPT、KNOWLEDGE_RETRIEVAL、HTTP_TOOL、CONDITION、"
            "QUESTION_CLASSIFIER、TEXT_TRANSFORM、CONTENT_TEMPLATE、LOOP。",
            "模板语法 {{ variable.path }} 全节点通用。",
        ],
        ["选中节点打开配置面板；调试面板输入 JSON 运行并查看节点 IO。"],
    )

    module_section(doc, 7, "运行监控", "workflow-runs.png", "图 5-6 运行监控台",
        [
            ["统计卡片", "卡片", "成功率、平均耗时、失败数、记录总数"],
            ["执行记录表", "表格", "执行ID、工作流ID、状态、耗时、节点数"],
            ["详情", "链接", "节点级输入输出"],
        ],
        ["状态：RUNNING / SUCCEEDED / FAILED；失败可下钻节点错误信息。"],
    )

    module_section(doc, 8, "知识库", "knowledge.png", "图 5-7 知识库中心",
        [
            ["向量库配置", "按钮", "ES 等连接管理"],
            ["新增知识库", "按钮", "创建知识库"],
            ["知识库清单", "表格", "向量维度、分段策略、检索模式、文档/切片数"],
            ["编辑/管理文档/删除", "操作", "行级"],
        ],
        [
            "检索模式：关键词/向量/混合。",
            "被工作流引用时删除需确认。",
        ],
    )

    module_section(doc, 9, "模型配置", "models.png", "图 5-8 模型配置",
        [
            ["新增模型", "按钮", "接入 Provider"],
            ["模型清单", "表格", "类型、用途、能力、价格、Base URL、状态"],
        ],
        ["用途分对话/嵌入/多模态；工作流 LLM 节点仅选对话用途且启用的模型。"],
    )

    module_section(doc, 10, "编研模板", "research-templates.png", "图 5-9 编研模板",
        [
            ["新增模板", "按钮", "维护 schema"],
            ["模板清单", "表格", "编码、章节数、绑定工作流、状态"],
            ["预览/编辑/删除", "操作", "行级"],
        ],
        ["保存时同步工作流快照；启用后可在智能编研向导选择。"],
    )

    module_section(doc, 11, "智能编研", "research-compile.png", "图 5-10 智能编研向导",
        [
            ["步骤条", "向导", "主题→主题库→知识库→模板→成果"],
            ["编研主题/面向对象", "表单", "步骤1"],
            ["工作流编排预览", "面板", "选择模板后展示节点"],
        ],
        ["提交后异步执行绑定工作流；成果区可查看大纲、正文并下载 DOCX。"],
    )

    heading(doc, "5.12 系统管理", 2)
    subs = [
        ("5.12.1 租户管理", "system-tenants.png", "图 5-11 租户管理", [
            ["新建租户", "按钮", "编码、名称"],
            ["租户列表", "表格", "编码、名称、状态、管理/编辑/删除"],
        ], ["仅 platform_admin 可创建租户。"]),
        ("5.12.2 组织用户", "system-users.png", "图 5-12 组织用户", [
            ["组织树", "树", "单位/部门"],
            ["用户列表", "表格", "姓名、登录名、组织、角色"],
            ["新增用户", "按钮", "创建账号"],
        ], ["Tab：组织架构/用户/角色。"]),
        ("5.12.3 资产授权", "asset-grants.png", "图 5-13 资产授权", [
            ["资产 Tab", "标签", "智能体/知识库/工作流/大模型"],
            ["配置授权", "链接", "按组织部门角色授权 USE"],
        ], ["开放 API 按调用方上下文过滤可见资产。"]),
        ("5.12.4 菜单管理", "system-menus.png", "图 5-14 菜单管理", [
            ["菜单列表", "表格", "分组、Key、路径、排序、可见性"],
        ], ["修改后刷新页面生效。"]),
        ("5.12.5 数据字典", "system-dictionary.png", "图 5-15 数据字典", [
            ["字典列表", "表格", "编码、名称、状态"],
            ["字典项", "表格", "标签、值、排序"],
        ], ["左侧选字典，右侧维护字典项。"]),
        ("5.12.6 日志管理", "system-logs.png", "图 5-16 日志管理", [
            ["筛选", "表单", "事件类型、用户、结果、时间"],
            ["日志列表", "表格", "时间、事件、用户、结果、IP"],
        ], ["默认近 7 天；支持 LOGIN/LOGOUT/API_KEY_USED 等。"]),
    ]
    for title, img, cap, elements, rules in subs:
        heading(doc, title, 3)
        heading(doc, title.split(" ", 1)[1] + " - 页面原型", 4)
        image(doc, img, cap)
        heading(doc, "页面元素", 4)
        table(doc, ["元素名称", "类型", "说明"], elements, [4, 2.5, 9])
        heading(doc, "规则说明", 4)
        for r in rules:
            para(doc, r, size=11)

    # 6 系统架构
    doc.add_page_break()
    heading(doc, "6 系统架构设计", 1)
    heading(doc, "6.1 总体架构", 2)
    para(
        doc,
        "┌─────────────┐   REST/JSON   ┌──────────────────────────────────┐\n"
        "│ Admin SPA   │ ────────────→ │ Spring Boot 3.3 (com.mw.ai.agi)   │\n"
        "│ React+Vite  │               │ workflow/knowledge/bot/auth/...    │\n"
        "└─────────────┘               └──────────────┬───────────────────┘\n"
        "┌─────────────┐   AppCode+Key                │ PostgreSQL / 达梦\n"
        "│ 第三方系统   │ ──→ /api/open/** ────────────┘ Flyway 迁移\n"
        "└─────────────┘",
        size=10,
    )
    heading(doc, "6.2 技术栈", 2)
    table(
        doc,
        ["层次", "技术", "说明"],
        [
            ["后端", "Java 17, Spring Boot 3.3, MyBatis-Plus", "REST API + 工作流引擎"],
            ["前端", "React 18, Ant Design 5, Vite", "管理控制台 SPA"],
            ["数据库", "PostgreSQL 16 / 达梦 DM8", "Flyway V1–V31"],
            ["集成", "OpenFeign, Open API", "第三方应用与 Java SDK"],
        ],
        [3, 5, 7],
    )
    heading(doc, "6.3 部署说明", 2)
    table(
        doc,
        ["组件", "端口", "说明"],
        [
            ["aiworkflow-server", "8080", "后端 API、Swagger"],
            ["admin 前端", "5173(dev)", "Vite 开发；生产构建 dist 静态部署"],
            ["PostgreSQL", "5432", "docker-compose 可选"],
        ],
        [4, 3, 7],
    )

    doc.save(OUT)
    print(OUT)


if __name__ == "__main__":
    main()
