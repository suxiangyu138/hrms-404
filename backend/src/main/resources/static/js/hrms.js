/* =====================================================================
   HRMS-404 人事管理系统 · 前端公共工具（原生 JS，无 jQuery）
   ===================================================================== */
"use strict";

/** 顶栏渲染的当前登录用户（由 Thymeleaf 注入 #ctx data-* 属性） */
const ctxEl = document.getElementById("ctx");
const CTX = ctxEl ? Object.fromEntries(
    ["username", "empName", "empId", "role", "roleName"].map(k => [k, (ctxEl.dataset[k] ?? "").trim()])
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

/** fetch 封装：自动解析 Result{code,message,data}，失败弹 toast 并返回 null */
async function api(url, options) {
    const resp = await fetch(url, Object.assign({ headers: { "Content-Type": "application/json" } }, options));
    let json = null;
    try { json = await resp.json(); } catch (e) { /* 非 JSON 响应 */ }
    if (!json) { toast("服务器响应异常", "error"); return null; }
    if (json.code === 401 && !location.pathname.endsWith("/login")) {
        toast("登录已过期，正在跳转…");
        setTimeout(() => location.href = "/login", 600);
        return null;
    }
    if (json.code !== 200) { toast(json.message || "操作失败", "error", 3600); return null; }
    if (options && options.silent === undefined || !options) { /* 不弹成功提示，由调用方决定 */ }
    return json.data;
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

/* ---------- CSV 导出 ---------- */
function exportCSV(filename, headers, rows) {
    const escapeCell = v => {
        let s = v === null || v === undefined ? "" : String(v);
        // 防止 Excel 公式注入：以 = + - @ 开头的单元格加前导单引号并按文本处理
        if (/^[=+\-@]/.test(s)) s = "'" + s;
        return /[",\n]/.test(s) ? `"${s.replace(/"/g, '""')}"` : s;
    };
    const lines = [headers.map(h => escapeCell(h.label)).join(",")];
    rows.forEach(r => lines.push(headers.map(h => escapeCell(r[h.key])).join(",")));
    // BOM 前缀保证 Excel 以 UTF-8 打开不乱码
    const blob = new Blob(["﻿" + lines.join("\r\n")], { type: "text/csv;charset=utf-8" });
    const a = document.createElement("a");
    a.href = URL.createObjectURL(blob);
    a.download = filename;
    a.click();
    URL.revokeObjectURL(a.href);
}

/* ---------- Excel(.xlsx) 导出：POST 数据到后端通用导出接口，由 Apache POI 生成真实 Excel ---------- */
async function exportXlsx(filename, columns, rows) {
    // 文件名防御：强制携带 .xlsx 后缀
    if (!/\.xlsx$/i.test(filename || "")) {
        filename = (filename || "导出数据") + ".xlsx";
    }
    const resp = await fetch("/api/export/xlsx", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ filename, columns, rows })
    });
    // 后端异常时返回的是 JSON 错误（统一 Result 格式），不能当成文件下载
    const ct = resp.headers.get("content-type") || "";
    if (!ct.includes("spreadsheetml")) {
        let msg = "导出失败，请稍后重试";
        try {
            const err = await resp.json();
            if (err && err.message) msg = err.message;
        } catch (e) { /* 非 JSON 响应，保持默认提示 */ }
        toast(msg, "error");
        return;
    }
    const blob = await resp.blob();
    const url = URL.createObjectURL(blob);
    const a = document.createElement("a");
    a.href = url;
    a.download = filename;          // 写死完整文件名（含 .xlsx），浏览器优先使用该名称
    document.body.appendChild(a);
    a.click();
    a.remove();
    // 延迟释放 blob：点击后立刻 revoke 存在竞态，Chrome 可能因 blob 被回收
    // 导致下载文件名回退为随机 UUID，延迟 3 秒释放规避
    setTimeout(() => URL.revokeObjectURL(url), 3000);
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
