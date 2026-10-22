# Pandora native workspace design

<!-- Hallmark · pre-emit critique: P4 H4 E4 S4 R5 V4. Internal review; independent screenshot scoring remains pending. -->

Genre: technical modern minimal with an editorial Chinese title. Deep sea background, mineral white reading text, copper actions and mint selections. Native Compose only. Bundled Noto Sans SC 400/500/600/700 for all dynamic content; the Noto Serif SC 600 subset is reserved for fixed page titles.

Page composition follows its job:
- Overview: the earliest dated unfinished task is the primary focus, with actual progress, deadline, next-step note and a compact secondary text update action. A second task is a compact row. Today's journal is a short reading excerpt in regular-weight primary ink. Company and personal ownership headings group full-width index entries; counts and arrows align on the right. Date and sync state share the overview title caption, with a deliberate line break at larger text sizes. Failures use an independent status row with retained-content status, expandable complete reasons and explicit retry; the task heading and all-tasks action remain visible. Missing or failed requests do not masquerade as an empty day. An empty task section reports its state; an empty journal section offers recording, instead of combining the two into one generic empty screen.
- Logs: the default is the entire journal, newest dates first. Date filtering is explicit. Entries group into month sections and date groups with a serif day numeral and weekday in a dedicated left column; update time stays subordinate to the author's text and only draft privacy is highlighted as an exception; submission happens in the editor, beside validation and save controls. The empty state contains a direct creation action, never a simulated writing field.
- Calendar: the day agenda is the initial composition, with a seven-day date strip and day/week/month switches. Month and mode form the left toolbar group; period navigation forms the right group. The full month remains available as a date locator, and its agenda displays only the selected day. Selecting a month date preserves the grid and updates its detail. One actual-content dot sits beneath numerals; an independent underline indicates selection and a heavier numeral indicates today. Actual nonzero log/task counts remain in the agenda heading and accessible date descriptions. Agenda rows align update/deadline times in a left column and distinguish regular-weight journal titles from medium-weight tasks. Task state is teal and draft privacy is copper; ordinary submitted-state labels are omitted. An empty agenda guides date selection and keeps the existing header recording action.
- Profile: identity, two preferences and compact account/service rows. Access and connection detail open on demand. Preferences scroll independently of the bottom sign-out action.

Four primary destinations: overview, logs, calendar and profile. AI analysis is unavailable and is stated in Profile's service detail. Recording uses one copper text-and-plus action, browsing and selection use teal. The calendar always retains its header recording entry. First-use logs place recording beside their explanation; overview places it in the empty journal section, and both suppress duplicate header entries. No floating controls cover content. Header, footer and content share the same 600dp width limit on wider screens. Month dates without records use subordinate ink; content dots always come from actual loaded records.

Preserve the existing ViewModel, API, authentication, draft, submission and role contracts. Use only loaded data. Loading and errors remain explicit. Empty space follows actual content; never invent metrics, illustrations or records to fill a screen. Controls preserve native touch targets and feedback. Screens remain scrollable, respect system insets and limit reading width to 600dp. Verify sparse and populated states and 320/375/414/768dp widths before further screenshot critiques.

Exports: `ConsoleTokens`, native M3 `ColorScheme`, `Typography`, `Shapes` in `android/app/src/main/java/com/projectpandora/app/ui/ControlTheme.kt`. No web runtime or downloaded fonts. See `docs/设计盲评迭代.md` for the independent scoring protocol.

Appearance: users select Light or Dark in Profile. Choice is persisted locally and applies to the workspace and its system bars. The landing/login page always uses mineral ivory, deep petrol and copper; login restores the saved workspace appearance. Light is the default; previous automatic settings fall back to Light. No automatic mode or moon-phase code remains.

Calendar revision: the initial view is the full month. All three date scopes preserve the month grid and month navigation. Day scope tints the selected date; week scope tints the full corresponding week. A separate underline identifies the anchor date, actual-content dots remain data-backed. The agenda follows the selected day or week. Month, year, scope and navigation share one stable header, with sync available in its menu.

Overview revision: date and connection state have a fixed-height status region below the stable header; failure details and retry do not replace the task heading. Three brief category entries form one compact horizontal group, with ownership labels and attached counts. Recording uses one compact copper outlined button throughout; draft privacy and ordinary task status use quiet ink. Empty logs skip redundant zero-count/filter rows.

Landing revision: an original imagegen mountain-and-paper landscape is the first frame of a 9-second, 271-frame camera film. Three transitions share identical boundary frames. The page scrubs the single continuous film with scroll and permits reverse traversal. Live copy occupies the quiet upper portion, a fixed direct-login action skips the narrative, and the existing login form retains its contract. No continuous autoplay. Reduced motion and video failure retain the initial frame. The browser implementation is a visual verification artifact in docs/landing-preview/journey.html, separate from the existing management Web app.

## 当前落地页交付版 v5 / 0.4.22
采用固定纸白、矿物青黑与铜色笔迹。首幕不对称山体；第二幕平面日志；第三幕完整月历与三条相连的工作记录。去除旧版桥、古书和透视纸册。标题使用 Noto Serif SC；末幕进入按钮带浅纸衬底，保证笔迹之上的文字对比。用户要求先提供 APK 并等待反馈，因此暂停独立盲评循环。

## 当前恢复版 / 0.4.23-review22（覆盖以上后续修订）
按用户最新要求，工作区恢复第二十二轮评审时的界面：三页页头创建入口、原工作分类列表、默认日视图与日／周／月切换，以及原日志和个人页。保留手动浅色／深色。三幕落地页退出运行路径；原登录页保持布局、控件和操作，仅加入纸白留白、边角墨色与铜色笔迹的生成背景，固定配色。此次不进行新评分循环；详细恢复依据与背景提示见 docs/恢复22号版本说明.md。

## 当前月历与统一背景版 / 0.4.24-month-paper
依据后续用户要求，视图仅保留完整月历，取消日／周模式入口。点选日期不改变月历结构，下方更新当天记录；有记录的日期显示圆点，当前选中日期有淡底色和下划线，今天使用较粗字重。月份前后切换和回到今天保留。工作区全屏背景复用登录页的同一张图：浅色原纸白，深色通过颜色矩阵把纸白、青黑和铜色分别映射到青黑底、青绿墨迹与暗铜笔迹，无新增位图或动画。原记录卡片保留实色衬底，普通列表透明，导航使用主题底色保证可读。登录页固定原浅色背景；其他页面内容继续沿用恢复版。

## 当前角色权限版 / 0.4.25-role-access
继续使用以上月历与背景。员工、团队长、部门老总、创始人的首页分别显示我的工作、团队总览、部门总览、公司总览；总览新增克制的一行身份与组织入口。组织页按角色分组呈现可向下查看的成员，读取页面独立只读；员工默认进入直属上级申请，领导默认进入下级工作，并可切换查看申请。用户已明确申请仅限直属上级，批准与拒绝由被查看者操作，可撤销已批准授权。草稿、修改操作和其他部门信息不因角色差异开放。详细服务端规则与交付见 docs/角色权限与申请.md。

## 当前纸墨平衡与月历排版 / 0.4.26-paper-calendar
遵照用户补充意见，保留左下浓墨，在右上增加同色墨团形成对角呼应，右侧中段以极淡渗染连接；取消不协调的连续右侧铜线，只留颜料斑点。中央保持纸白阅读区。新资源为 `paper_background_balanced.png`，登录固定浅色，工作区继续使用同图与原深色颜色映射。月历月份、星期、日期统一使用现有 Noto Sans SC，数字启用 tabular figures；月份中等字重，星期 12sp，日期 18sp，今天较粗。月份箭头使用原生矢量图标，取消字体回退的尖括号。

## 独立水墨入场 / 0.4.26-ink-entrance
用户随后要求在登录前添加独立滚动落地页，视频仅作用于落地页，且不得出现工作区内容。采用上述生成纸墨初帧，后续生成山水与水面状态，以实际视频末帧作为下一段参考；飞鸟入画、墨色叠化和细微运镜合成为 60 fps、541 帧视频。三段共享末首帧，前进与反向滚动均按位置定位。顶端始终可直接登录，末段提供进入登录；减少动画或视频失败时保留静态画与入口。进入登录后销毁视频播放器，业务工作区保持静态背景和原有权限。

## 自动背景与登录渐显 / 0.4.27-ink-login（覆盖滚动入场）
按用户最新要求，首屏只显示水墨画与自动播放视频，取消滚动、章节提示、独立进入按钮。视频首帧出现约 2.5 秒后，原登录表单以 1.6 秒淡入；表单自身不再覆盖视频背景。视频播放一次后停在末帧，不自动跳转、不突兀循环；切后台暂停，返回继续，登录成功后释放播放器。减少动画时立即显示静态画与表单；视频失败或解码器未就绪时开放表单，避免无法登录。月历字体及业务权限保持原行为。

## 登录精简与日志窗口 / 0.4.28-log-window
视频首帧约 1 秒后渐显登录表单，保留 1.6 秒渐显；删除落地页解说、宣传标题与说明，只留操作控件和错误。连接设置初始收起。刷新和写日志共用尺寸。历史日志保持完整读取；Shanghai 当天向前七天（两端包含）才可创建、修改、提交。后端检查原日期与目标日期，Android/Web 对旧日志只读展示，历史筛选不受窗口限制。验证和安装包见 docs/日志七天修改规则.md。

## 流畅背景、居中表单与图标按钮 / 0.4.29-smooth-login
水墨原视频离线运动补偿至 120fps 中间帧，经两帧时间滤波输出连续 60fps、541 帧。采用 Baseline H.264、每秒关键帧和受控码率，将文件从 11.6MB 减为 2.64MB；原时长、构图与过渡保留。安卓使用直接解码显示 SurfaceView、异步准备及 60Hz 显示提示；渐显仅作用于表单区域，取消全屏透明合成。登录整体按可用高度居中，小屏和键盘出现时可滚动。两个页头按钮保留铜色／纸白与原圆角材质，改为 36dp 小方形，仅保留加号／旋转图标，48dp 触摸区域及无障碍文字继续保留。验收见 docs/水墨播放与居中登录.md。

登录标题补充：使用 image_gen 生成的透明「潘多拉」半行草字标，约 196dp 宽置于表单上方，与控件一起渐显；将字标纳入整组居中计算。没有恢复说明文字或宣传标题。字标及纸墨背景移入 drawable-nodpi，按原像素解码。

## 水墨鹭鸟 / 0.4.30-ink-herons
移除原简笔线鸟，使用生成的鸟身与两侧羽翼贴图，肩／肘／腕关节三维投影驱动细羽纹理；下压展开、上抬略收翼，翼尖有滞后，中途平滑滑翔。三鸟有不同远近、墨色与拍翅相位，连续穿过上方留白，不在段落交界重复入场。保持 60fps、541 帧、原首末水墨画、现有登录标题与居中控件；离线渲染，手机不计算骨架。源码与验收见 docs/水墨鹭鸟动画.md。
