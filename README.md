# 制度与监管政策寻租行为分析平台

本项目用于分析企业内部规章制度、业务流程文件，以及外部导入的监管政策文本中可能存在的寻租风险。平台以“寻租风险分析”为旗舰功能，围绕制度文本归集、规则兜底识别、LLM 语义增强、风险复核、报告预览和系统审计形成 MVP 闭环；制度冲突检测、AI 记录、看板报告等模块均服务于这一核心分析链路。

寻租分析在未配置 LLM 时会使用本地规则完成可复现的风险识别，包括自由裁量、审批集中、流程透明度不足、监督约束不足、利益分配和处罚弹性等指标；配置 OpenAI-compatible LLM 后，会在规则兜底基础上增强制度摘要、风险归因、条款解释、风险评分和整改建议质量。

`案例扩展分析` 入口用于围绕真实事件举一反三检索相关制度。用户可输入案例事实、选择已导入制度、粘贴官方公开链接，系统会输出相关法律公文、组织责任链、具体寻租风险点和待手动导入材料清单；未能自动抓取的 PDF 或缺失材料会明确标记。

## 本地基础设施

阶段 0 提供 MySQL 8 和 Redis 7 的 Docker Compose 基础环境。

推荐端口：

| 服务 | 端口 |
| --- | --- |
| Gateway | 8080 |
| Backend | 8081 |
| Frontend | 5173 |
| MySQL | 3306 |
| Redis | 6379 |

推荐版本：

| 软件 | 推荐版本 |
| --- | --- |
| JDK | 21 LTS 或 17 LTS |
| Maven | 3.9+ |
| Node.js | 当前 LTS |
| pnpm | 9+ |
| Docker Desktop | 最新稳定版 |

启动基础设施：

```powershell
Copy-Item .env.example .env
cd docker
docker compose --env-file ../.env up -d
```

如果镜像拉取超时，可以在 `.env` 中临时切换镜像来源，例如：

```env
MYSQL_IMAGE=public.ecr.aws/docker/library/mysql:8.4
REDIS_IMAGE=public.ecr.aws/docker/library/redis:7.4-alpine
```

也可以在 Docker Desktop 的 Docker Engine 配置中调整 `registry-mirrors`，删除不可用或超时的镜像源后重启 Docker Desktop。

查看状态：

```powershell
cd docker
docker compose ps
```

## 数据库初始化

初始化脚本位于 `database/init`：

| 脚本 | 内容 |
| --- | --- |
| `001_schema.sql` | 数据库、表结构、索引 |
| `002_seed_roles.sql` | 预置角色 |
| `003_seed_dicts.sql` | 基础字典 |
| `004_seed_configs.sql` | 系统参数和 AI 提示词模板 |
| `005_seed_demo_data.sql` | 演示账号、菜单和真实监管制度样本 |

当前本地 `.env` 中 MySQL 端口为 `3307`。如需手动执行初始化脚本：

```powershell
docker cp database/init/. rent-seeking-mysql:/tmp/rent_stage1_init
docker exec rent-seeking-mysql sh -c 'set -e; for f in /tmp/rent_stage1_init/*.sql; do mysql -uroot -p"$MYSQL_ROOT_PASSWORD" --default-character-set=utf8mb4 < "$f"; done'
```

演示账号：

| 账号 | 密码 | 角色 |
| --- | --- | --- |
| admin | admin123 | 系统管理员 |
| regulation | regulation123 | 制度管理员 |
| analyst | analyst123 | 分析人员 |
| viewer | viewer123 | 只读用户 |

## 后端服务

后端工程位于 `backend`，默认端口为 `8081`。本机没有全局 Maven 时，可使用项目本地 `.tools` 下的 Maven：

```powershell
cd backend
..\.tools\apache-maven-3.9.9\bin\mvn.cmd spring-boot:run "-Dspring-boot.run.profiles=local"
```

健康检查：

```text
http://localhost:8081/api/system/health
```

## LLM 配置

LLM 参数可在网页中维护：登录后进入 `系统参数`，在 `LLM 配置` 区域填写：

| 参数键 | 说明 |
| --- | --- |
| `ai.enabled` | 是否启用 LLM 增强寻租分析，`true` 或 `false` |
| `ai.provider` | 当前支持 `openai-compatible` |
| `ai.base-url` | OpenAI-compatible Base URL，例如 `https://api.openai.com/v1` |
| `ai.api-key` | LLM API Key，页面不明文展示 |
| `ai.model` | 模型名称 |
| `ai.timeout` | 调用超时时间，例如 `30s` |

环境变量仍可作为首次启动或兜底配置；数据库中的系统参数优先生效。未配置 LLM 时，旗舰寻租分析会自动使用本地规则降级，不阻断演示流程；配置 LLM 后，风险摘要、原因解释、整改建议和结构化风险项会优先采用模型结果，并保留人工复核入口。

受保护接口会要求 JWT：

```text
http://localhost:8081/api/system/configs
```

停止基础设施：

```powershell
cd docker
docker compose down
```
