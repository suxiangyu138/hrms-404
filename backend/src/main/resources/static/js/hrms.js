/* =====================================================================
   HRMS-404 人事管理系统 · 前端公共工具（原生 JS，无 jQuery）
   ===================================================================== */
"use strict";

/** 顶栏渲染的当前登录用户（由 Thymeleaf 注入 #ctx data-* 属性） */
const ctxEl = document.getElementById("ctx");
const CTX = ctxEl ? Object.fromEntries(
    ["userId", "username", "empName", "empId", "role", "roleName"].map(k => [k, (ctxEl.dataset[k] ?? "").trim()])
) : {};

function isRole(...roles) { return roles.includes(CTX.role); }

/** 简单 XSS 转义 */
function esc(v) {
    if (v === null || v === undefined) return "";
    return String(v).replace(/[&<>"']/g, c => ({ "&": "&amp;", "<": "&lt;", ">": "&gt;", '"': "&quot;", "'": "&#39;" }[c]));
}

/** ISO 日期(yyyy-MM-dd) → 中文日期（2026年9月7日） */
function fmtDateZh(s) {
    if (!s) return "-";
    const m = String(s).match(/^(\d{4})-(\d{2})-(\d{2})/);
    return m ? `${m[1]}年${Number(m[2])}月${Number(m[3])}日` : String(s);
}

/** ISO 日期时间 → 时间（HH:mm），展示用 */
function fmtTime(s) {
    const m = String(s || "").match(/(\d{2}:\d{2})(:\d{2})?/);
    return m ? m[1] : "-";
}

/** 手机号中间四位脱敏 */
function maskPhone(p) {
    if (!p) return "-";
    return String(p).replace(/^(\d{3})\d{4}(\d{4})$/, "$1****$2");
}

/** Toast 轻提示 */
function toast(msg, type = "success", ms = 2600) {
    let host = document.getElementById("toastHost");
    if (!host) { host = document.createElement("div"); host.id = "toastHost"; document.body.appendChild(host); }
    const t = document.createElement("div");
    t.className = "hrms-toast" + (type === "error" ? " err" : "");
    t.textContent = msg;
    host.appendChild(t);
    setTimeout(() => t.remove(), ms);
}

/** fetch 封装：自动解析 Result{code,message,data}，失败弹 toast 并返回 null。
 *
 * 返回约定（读写分开，因为 null 在两处的含义不同）：
 * - 写操作 POST/PUT/DELETE：成功一律返回「非 null」。后端 Result<Void> 接口（改密码、
 *   离职、删除等）data 为 null，这里用 true 作成功哨兵，调用方统一的 `if (r !== null)`
 *   才成立，不会再把「成功但无返回内容」误判为失败而漏掉 toast 和列表刷新。
 * - 读操作 GET：原样返回 data。GET 的 data=null 是有效业务语义
 *   （如 /api/attendance/today 的「今日未打卡」），不能替换掉。
 * 用 ?? 而非 ||：data 为 0（如薪资生成 0 条）是有效业务值，必须原样返回。 */
async function api(url, options) {
    let resp;
    try {
        resp = await fetch(url, Object.assign({ headers: { "Content-Type": "application/json" } }, options));
    } catch (e) {
        // 后端未启动 / 网络中断：给出提示而不是抛出未处理的 Promise 异常
        toast("无法连接服务器，请确认后端已启动", "error", 3600);
        return null;
    }
    let json = null;
    try { json = await resp.json(); } catch (e) { /* 非 JSON 响应 */ }
    if (!json) { toast("服务器响应异常", "error"); return null; }
    if (json.code === 401 && !location.pathname.endsWith("/login")) {
        toast("登录已过期，正在跳转…");
        setTimeout(() => location.href = "/login", 600);
        return null;
    }
    if (json.code !== 200) { toast(json.message || "操作失败", "error", 3600); return null; }
    const method = ((options && options.method) || "GET").toUpperCase();
    if (method === "GET") return json.data === undefined ? null : json.data;
    return json.data ?? true;
}

/** 通用确认弹窗 */
function confirmDlg(message, okText = "确定") {
    return new Promise(resolve => {
        let modalEl = document.getElementById("hrmsConfirm");
        if (!modalEl) {
            modalEl = document.createElement("div");
            modalEl.id = "hrmsConfirm";
            modalEl.className = "modal fade";
            modalEl.innerHTML = `<div class="modal-dialog modal-dialog-centered modal-sm">
                <div class="modal-content rounded-4 shadow">
                  <div class="modal-body pt-4 pb-2 text-center">
                    <p class="mt-1 mb-1 fw-semibold" id="hrmsConfirmMsg"></p>
                  </div>
                  <div class="modal-footer justify-content-center pb-3 pt-0 border-0">
                    <button class="btn btn-outline-secondary btn-sm px-4" data-bs-dismiss="modal">取消</button>
                    <button class="btn btn-brand btn-sm px-4" id="hrmsConfirmOk">${esc(okText)}</button>
                  </div>
                </div></div>`;
            document.body.appendChild(modalEl);
        }
        modalEl.querySelector("#hrmsConfirmMsg").textContent = message;
        modalEl.querySelector("#hrmsConfirmOk").onclick = () => resolve(true);
        modalEl.addEventListener("hidden.bs.modal", () => resolve(false), { once: true });
        const modal = bootstrap.Modal.getOrCreateInstance(modalEl);
        modal.show();
    });
}

/** 隐藏 modal（确认后调用） */
function hideModal(id) {
    const el = document.getElementById(id);
    if (el) bootstrap.Modal.getOrCreateInstance(el).hide();
}

/* ---------- 格式化 ---------- */
const pad = n => String(n).padStart(2, "0");
const monthNow = () => { const d = new Date(); return `${d.getFullYear()}-${pad(d.getMonth() + 1)}`; };
const dateNow = () => { const d = new Date(); return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}`; };
const fmtDate = s => (s || "").slice(0, 10);
const fmtDateTime = s => (s || "").slice(0, 16);
const money = v => Number(v ?? 0).toLocaleString("zh-CN", { minimumFractionDigits: 2, maximumFractionDigits: 2 });

const ATT_TEXT = { 0: "未完成", 1: "正常", 2: "迟到", 3: "早退", 4: "迟到早退" };
function attBadge(status) {
    const cls = { 1: "st-on", 2: "st-late", 3: "st-early", 4: "st-late" }[status] || "";
    return `<span class="st-badge ${cls}">${esc(ATT_TEXT[status] ?? status)}</span>`;
}
const empStatusBadge = s => s === 1
    ? '<span class="status-pill status-on">在职</span>'
    : '<span class="status-pill status-off">已离职</span>';

/* ---------- Excel(.xlsx) 导出 ----------
   后端用 Apache POI 生成真正的 .xlsx（不是 CSV 改名），前端只负责把二进制流落盘。

   这里必须走 fetch，不能用原生 form 提交。form 提交等于把响应交给浏览器去「导航」：
   浏览器会先拿当前页面去加载这个响应，发现是附件才中止导航改成下载 —— 成功时页面会闪一下，
   失败时（会话过期返回 401 JSON、参数异常返回 400 JSON、服务端异常返回错误页）
   这串响应会直接替换掉当前页面，用户看到的就是「点一下导出，整个页面没了」。
   走 fetch 则非文件响应全部留在 JS 里转成 toast，页面毫发无损。

   返回 true 表示已拿到文件并触发下载；调用方据此决定要不要提示「已导出」。 */
async function exportXlsx(filename, columns, rows) {
    // 文件名防御：强制携带 .xlsx 后缀
    if (!/\.xlsx$/i.test(filename || "")) {
        filename = (filename || "导出数据") + ".xlsx";
    }

    let resp;
    try {
        resp = await fetch("/api/export/xlsx", {
            method: "POST",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify({ filename, columns, rows })
        });
    } catch (e) {
        toast("无法连接服务器，导出失败", "error", 3600);
        return false;
    }

    // 只有 xlsx 的 MIME 才是文件；其余一律按失败处理（统一 Result JSON / 错误页）
    const ct = resp.headers.get("content-type") || "";
    if (!resp.ok || !ct.includes("spreadsheetml")) {
        if (resp.status === 401) {
            toast("登录已过期，正在跳转…");
            setTimeout(() => location.href = "/login", 800);
            return false;
        }
        let msg = "导出失败，请稍后重试";
        try {
            const err = await resp.json();
            if (err && err.message) msg = err.message;
        } catch (e) { /* 非 JSON（如 500 错误页），保留默认提示 */ }
        toast(msg, "error", 3600);
        return false;
    }

    const blob = await resp.blob();
    const url = URL.createObjectURL(blob);
    const a = document.createElement("a");
    a.href = url;
    a.download = filenameFromDisposition(resp.headers.get("content-disposition")) || filename;
    // 必须先入文档再点击：游离节点上的 click() 在部分浏览器不触发下载
    document.body.appendChild(a);
    a.click();
    a.remove();
    // 必须延迟释放：点击后浏览器是异步去读 blob 的，同步 revoke 会把流掐断，
    // 轻则文件损坏，重则文件名回退成 blob URL 的随机 UUID
    setTimeout(() => URL.revokeObjectURL(url), 10000);
    return true;
}

/** 解析 Content-Disposition 里的文件名：优先 RFC 5987 的 filename*（中文走这里），退回 ASCII 的 filename */
function filenameFromDisposition(disposition) {
    if (!disposition) return "";
    const star = /filename\*=UTF-8''([^;]+)/i.exec(disposition);
    if (star) {
        try {
            return decodeURIComponent(star[1]);
        } catch (e) { /* 编码异常则走下面的 ASCII 兜底 */ }
    }
    const plain = /filename="?([^";]+)"?/i.exec(disposition);
    return plain ? plain[1] : "";
}

/** 渲染 Bootstrap 表格数据行 */
function tbodyHtml(list, rowBuilder) {
    if (!list || !list.length) return "";
    return list.map(rowBuilder).join("");
}

/** 空状态 404 占位 HTML */
const empty404 = (tip = "404：暂无数据，去新增一条吧") => `
    <div class="empty-404">
      <div class="big">404 NOT FOUND</div>
      <div class="mt-1">${esc(tip)}</div>
    </div>`;
