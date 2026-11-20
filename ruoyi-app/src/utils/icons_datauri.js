/**
 * 图标 & 贴纸 · data URI（T31.3）
 * 基于交付包 icons_datauri.js，扩充至 21 个功能图标。
 *
 * 用途：CSS mask 方案中作为 --ico 变量值，规避 file:// 与跨域限制。
 * 用法：import { ICON_URI, iconDataUri } from '@/utils/icons_datauri'
 *       style="--ico: url('ICON_URI['ico-home']')"
 *       或 iconDataUri('ico-home') 返回完整 url(...) 字符串。
 *
 * 格式说明：
 *   - 已有图标沿用交付包 base64 编码（A-Za-z0-9+/=，无引号/括号，CSS url() 安全）
 *   - 新增图标使用 charset=utf-8 + encodeURIComponent（等效，体积更小）
 */

/* ---------- 图标 data URI（21 个功能图标） ---------- */
export const ICON_URI = {
  // ===== 交付包原有 15 个（base64） =====
  'ico-activity': 'data:image/svg+xml;base64,PHN2ZyB4bWxucz0iaHR0cDovL3d3dy53My5vcmcvMjAwMC9zdmciIHZpZXdCb3g9IjAgMCAyNCAyNCI+PHBhdGggZD0iTTE5IDNoLTFWMWgtMnYyaC04VjFINnYySDVhMiAyIDAgMCAwLTIgMnYxNGEyIDIgMCAwIDAgMiAyaDE0YTIgMiAwIDAgMCAyLTJWNWEyIDIgMCAwIDAtMi0yem0wIDE2SDVWOWgxNHoiIGZpbGw9ImN1cnJlbnRDb2xvciIvPjwvc3ZnPgo=',
  'ico-arrow': 'data:image/svg+xml;base64,PHN2ZyB4bWxucz0iaHR0cDovL3d3dy53My5vcmcvMjAwMC9zdmciIHZpZXdCb3g9IjAgMCAyNCAyNCI+PHBhdGggZD0iTTkgNmw2IDYtNiA2LTEuNC0xLjRMMTIuMiAxMiA3LjYgNy40eiIgZmlsbD0iY3VycmVudENvbG9yIi8+PC9zdmc+Cg==',
  'ico-booking': 'data:image/svg+xml;base64,PHN2ZyB4bWxucz0iaHR0cDovL3d3dy53My5vcmcvMjAwMC9zdmciIHZpZXdCb3g9IjAgMCAyNCAyNCI+PHBhdGggZD0iTTcgMnYySDVhMiAyIDAgMCAwLTIgMnYxNGEyIDIgMCAwIDAgMiAyaDE0YTIgMiAwIDAgMCAyLTJWNmEyIDIgMCAwIDAtMi0yaC0yVjJoLTJ2Mkg5VjJ6bTEyIDh2MTBINVYxMHoiIGZpbGw9ImN1cnJlbnRDb2xvciIvPjwvc3ZnPgo=',
  'ico-check': 'data:image/svg+xml;base64,PHN2ZyB4bWxucz0iaHR0cDovL3d3dy53My5vcmcvMjAwMC9zdmciIHZpZXdCb3g9IjAgMCAyNCAyNCI+PHBhdGggZD0iTTkgMTYuMiA0LjggMTJsLTEuNCAxLjRMOSAxOSAyMSA3bC0xLjQtMS40eiIgZmlsbD0iY3VycmVudENvbG9yIi8+PC9zdmc+Cg==',
  'ico-feedback': 'data:image/svg+xml;base64,PHN2ZyB4bWxucz0iaHR0cDovL3d3dy53My5vcmcvMjAwMC9zdmciIHZpZXdCb3g9IjAgMCAyNCAyNCI+PHBhdGggZD0iTTIwIDJINGEyIDIgMCAwIDAtMiAydjE4bDQtNGgxNGEyIDIgMCAwIDAgMi0yVjRhMiAyIDAgMCAwLTItMnptLTIgMTJINnYtMmgxMnptMC0zSDZWOWgxMnptMC0zSDZWNmgxMnoiIGZpbGw9ImN1cnJlbnRDb2xvciIvPjwvc3ZnPgo=',
  'ico-home': 'data:image/svg+xml;base64,PHN2ZyB4bWxucz0iaHR0cDovL3d3dy53My5vcmcvMjAwMC9zdmciIHZpZXdCb3g9IjAgMCAyNCAyNCI+PHBhdGggZD0iTTEyIDMgMiAxMWgzdjloNnYtNmgydjZoNnYtOWgzeiIgZmlsbD0iY3VycmVudENvbG9yIi8+PC9zdmc+Cg==',
  'ico-location': 'data:image/svg+xml;base64,PHN2ZyB4bWxucz0iaHR0cDovL3d3dy53My5vcmcvMjAwMC9zdmciIHZpZXdCb3g9IjAgMCAyNCAyNCI+PHBhdGggZD0iTTEyIDJhNyA3IDAgMCAwLTcgN2MwIDUuMyA3IDEzIDcgMTNzNy03LjcgNy0xM2E3IDcgMCAwIDAtNy03em0wIDkuNUEyLjUgMi41IDAgMSAxIDEyIDYuNWEyLjUgMi41IDAgMCAxIDAgNXoiIGZpbGw9ImN1cnJlbnRDb2xvciIvPjwvc3ZnPgo=',
  'ico-logout': 'data:image/svg+xml;base64,PHN2ZyB4bWxucz0iaHR0cDovL3d3dy53My5vcmcvMjAwMC9zdmciIHZpZXdCb3g9IjAgMCAyNCAyNCI+PHBhdGggZD0iTTE3IDhsLTEuNCAxLjQgMS42IDEuNkg5djJoOC4ybC0xLjYgMS42TDE3IDE2bDQtNHpNNSA1aDdWM0g1YTIgMiAwIDAgMC0yIDJ2MTRhMiAyIDAgMCAwIDIgMmg3di0ySDV6IiBmaWxsPSJjdXJyZW50Q29sb3IiLz48L3N2Zz4K',
  'ico-mine': 'data:image/svg+xml;base64,PHN2ZyB4bWxucz0iaHR0cDovL3d3dy53My5vcmcvMjAwMC9zdmciIHZpZXdCb3g9IjAgMCAyNCAyNCI+PHBhdGggZD0iTTEyIDEyYTUgNSAwIDEgMCAwLTEwIDUgNSAwIDAgMCAwIDEwem0wIDJjLTUgMC05IDIuNS05IDUuNVYyMmgxOHYtMi41YzAtMy00LTUuNS05LTUuNXoiIGZpbGw9ImN1cnJlbnRDb2xvciIvPjwvc3ZnPgo=',
  'ico-qrcode': 'data:image/svg+xml;base64,PHN2ZyB4bWxucz0iaHR0cDovL3d3dy53My5vcmcvMjAwMC9zdmciIHZpZXdCb3g9IjAgMCAyNCAyNCI+PHBhdGggZD0iTTMgM2g4djhIM3ptMiAydjRoNFY1em04LTJoOHY4aC04em0yIDJ2NGg0VjV6TTMgMTNoOHY4SDN6bTIgMnY0aDR2LTR6bTEyLTJoMnYyaC0yem00IDBoMnYyaC0yem0tNCA0aDJ2MmgtMnptMiAyaDJ2MmgtMnptMi0yaDJ2MmgtMnptLTYgMGgydjJoLTJ6bTAgNGg2djJoLTZ6IiBmaWxsPSJjdXJyZW50Q29sb3IiLz48L3N2Zz4K',
  'ico-refresh': 'data:image/svg+xml;base64,PHN2ZyB4bWxucz0iaHR0cDovL3d3dy53My5vcmcvMjAwMC9zdmciIHZpZXdCb3g9IjAgMCAyNCAyNCI+PHBhdGggZD0iTTE3LjYgNi40QTggOCAwIDEgMCAyMCAxMmgtMmE2IDYgMCAxIDEtMS44LTQuMkwxMyAxMWg3VjR6IiBmaWxsPSJjdXJyZW50Q29sb3IiLz48L3N2Zz4K',
  'ico-search': 'data:image/svg+xml;base64,PHN2ZyB4bWxucz0iaHR0cDovL3d3dy53My5vcmcvMjAwMC9zdmciIHZpZXdCb3g9IjAgMCAyNCAyNCI+PHBhdGggZD0iTTE1LjUgMTRoLS44bC0uMy0uM2E2LjUgNi41IDAgMSAwLS43LjdsLjMuM3YuOGw1IDUgMS41LTEuNXptLTYgMGE0LjUgNC41IDAgMSAxIDAtOSA0LjUgNC41IDAgMCAxIDAgOXoiIGZpbGw9ImN1cnJlbnRDb2xvciIvPjwvc3ZnPgo=',
  'ico-setting': 'data:image/svg+xml;base64,PHN2ZyB4bWxucz0iaHR0cDovL3d3dy53My5vcmcvMjAwMC9zdmciIHZpZXdCb3g9IjAgMCAyNCAyNCI+PHBhdGggZD0iTTE5LjQgMTNhNy42IDcuNiAwIDAgMCAwLTJsMi0xLjYtMi0zLjQtMi40IDFhNy42IDcuNiAwIDAgMC0xLjctMUwxNSAzSDlsLS4zIDIuNmMtLjYuMi0xLjIuNi0xLjcgMWwtMi40LTEtMiAzLjRMNC42IDExYTcuNiA3LjYgMCAwIDAgMCAybC0yIDEuNiAyIDMuNCAyLjQtMWMuNS40IDEuMS44IDEuNyAxTDkgMjFoNmwuMy0yLjZjLjYtLjIgMS4yLS42IDEuNy0xbDIuNCAxIDItMy40ek0xMiAxNS41YTMuNSAzLjUgMCAxIDEgMC03IDMuNSAzLjUgMCAwIDEgMCA3eiIgZmlsbD0iY3VycmVudENvbG9yIi8+PC9zdmc+Cg==',
  'ico-todo': 'data:image/svg+xml;base64,PHN2ZyB4bWxucz0iaHR0cDovL3d3dy53My5vcmcvMjAwMC9zdmciIHZpZXdCb3g9IjAgMCAyNCAyNCI+PHBhdGggZD0iTTE0IDJINmEyIDIgMCAwIDAtMiAydjE2YTIgMiAwIDAgMCAyIDJoMTJhMiAyIDAgMCAwIDItMlY4em0yIDE2SDh2LTJoOHptMC00SDh2LTJoOHptLTMtNVYzLjVMMTguNSA5eiIgZmlsbD0iY3VycmVudENvbG9yIi8+PC9zdmc+Cg==',
  'ico-workbench': 'data:image/svg+xml;base64,PHN2ZyB4bWxucz0iaHR0cDovL3d3dy53My5vcmcvMjAwMC9zdmciIHZpZXdCb3g9IjAgMCAyNCAyNCI+PHBhdGggZD0iTTMgM2g4djZIM3ptMTAgMGg4djZoLTh6TTMgMTFoOHYxMEgzem0xMCAwaDh2MTBoLTh6IiBmaWxsPSJjdXJyZW50Q29sb3IiLz48L3N2Zz4K',
  // ===== 交付包新增：手机（base64） =====
  'ico-phone-mobile': 'data:image/svg+xml;base64,PHN2ZyB4bWxucz0iaHR0cDovL3d3dy53My5vcmcvMjAwMC9zdmciIHZpZXdCb3g9IjAgMCAyNCAyNCIgd2lkdGg9IjI0IiBoZWlnaHQ9IjI0Ij4KPHBhdGggZmlsbD0iY3VycmVudENvbG9yIiBkPSJNNy41IDJoOWMuNiAwIDEgLjQgMSAxdjE4YzAgLjYtLjQgMS0xIDFoLTljLS42IDAtMS0uNC0xLTFWM2MwLS42LjQtMSAxLTFabTAgMS41djE2LjJjMCAuMS4xLjMuMy4zaDguNGMuMiAwIC4zLS4yLjMtLjNWMy41YzAtLjEtLjEtLjMtLjMtLjNINy44Yy0uMiAwLS4zLjItLjMuM1oiLz4KPHBhdGggZmlsbD0iY3VycmVudENvbG9yIiBkPSJNMTAuNyA0LjRoMi42Yy4yIDAgLjMuMS4zLjNzLS4xLjMtLjMuM2gtMi42Yy0uMiAwLS4zLS4xLS4zLS4zcy4xLS4zLjMtLjNaIi8+CjxjaXJjbGUgZmlsbD0iY3VycmVudENvbG9yIiBjeD0iMTIiIGN5PSIxOC4zIiByPSIxIi8+Cjwvc3ZnPg==',

  // ===== M31 T31.3 新增 14 个（URI-encoded，等效 base64，CSS url() 安全） =====
  'ico-wechat': "data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 24 24'%3E%3Cpath d='M8.5 13.5a1 1 0 1 1 0-2 1 1 0 0 1 0 2zm5 0a1 1 0 1 1 0-2 1 1 0 0 1 0 2zM12 2C6.5 2 2 5.6 2 10c0 2.5 1.3 4.7 3.4 6.2L4.8 19a.5.5 0 0 0 .7.6l3.3-1.7c1 .3 2.1.4 3.2.4 5.5 0 10-3.6 10-8s-4.5-8-10-8z' fill='currentColor'/%3E%3C/svg%3E",
  'ico-seat': "data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 24 24'%3E%3Cpath d='M4 18v-1h16v1a2 2 0 0 1-2 2H6a2 2 0 0 1-2-2zm2-3V5a2 2 0 0 1 2-2h8a2 2 0 0 1 2 2v10H6zm3-2h6V7H9v6z' fill='currentColor'/%3E%3C/svg%3E",
  'ico-delete': "data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 24 24'%3E%3Cpath d='M6 19a2 2 0 0 0 2 2h8a2 2 0 0 0 2-2V7H6v12zM19 4h-3.5l-1-1h-5l-1 1H5v2h14V4z' fill='currentColor'/%3E%3C/svg%3E",
  'ico-share': "data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 24 24'%3E%3Cpath d='M18 16.1c-.8 0-1.4.3-2 .8l-7-4a3 3 0 0 0 0-1.8l7-4A2.8 2.8 0 1 1 18 5.5a2.8 2.8 0 0 1-1.7-.6l-7 4A2.8 2.8 0 1 1 6 13.5a2.8 2.8 0 0 1 1.7-.6l7 4a2.8 2.8 0 1 1 3.3-.8z' fill='currentColor'/%3E%3C/svg%3E",
  'ico-export': "data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 24 24'%3E%3Cpath d='M19 9h-4V3H9v6H5l7 7 7-7zM5 18v2h14v-2H5z' fill='currentColor'/%3E%3C/svg%3E",
  'ico-plus': "data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 24 24'%3E%3Cpath d='M19 13h-6v6h-2v-6H5v-2h6V5h2v6h6v2z' fill='currentColor'/%3E%3C/svg%3E",
  'ico-lock': "data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 24 24'%3E%3Cpath d='M18 8h-1V6A5 5 0 0 0 7 6v2H6a2 2 0 0 0-2 2v10a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V10a2 2 0 0 0-2-2zM12 17a2 2 0 1 1 0-4 2 2 0 0 1 0 4zM9 8V6a3 3 0 0 1 6 0v2H9z' fill='currentColor'/%3E%3C/svg%3E",
  'ico-unlock': "data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 24 24'%3E%3Cpath d='M18 8h-1V6A5 5 0 0 0 7.6 4.2l1.8.9A3 3 0 0 1 15 6v2H6a2 2 0 0 0-2 2v10a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V10a2 2 0 0 0-2-2zm-6 9a2 2 0 1 1 0-4 2 2 0 0 1 0 4z' fill='currentColor'/%3E%3C/svg%3E",
  'ico-edit': "data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 24 24'%3E%3Cpath d='M3 17.25V21h3.75L17.8 9.94l-3.75-3.75L3 17.25zM20.7 7.04a1 1 0 0 0 0-1.41l-2.34-2.34a1 1 0 0 0-1.41 0l-1.83 1.83 3.75 3.75 1.83-1.83z' fill='currentColor'/%3E%3C/svg%3E",
  'ico-calendar': "data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 24 24'%3E%3Cpath d='M19 3h-1V1h-2v2H8V1H6v2H5a2 2 0 0 0-2 2v14a2 2 0 0 0 2 2h14a2 2 0 0 0 2-2V5a2 2 0 0 0-2-2zm0 16H5V9h14v10zM7 11h5v5H7v-5z' fill='currentColor'/%3E%3C/svg%3E",
  'ico-amount': "data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 24 24'%3E%3Cpath d='M11.8 10.9c-2.27-.59-3-1.2-3-2.15 0-1.09 1.01-1.85 2.7-1.85 1.78 0 2.44.85 2.5 2.1h2.21c-.07-1.72-1.12-3.3-3.21-3.81V3h-3v2.16c-1.94.42-3.5 1.68-3.5 3.61 0 2.31 1.91 3.46 4.7 4.13 2.5.6 3 1.48 3 2.41 0 .69-.49 1.79-2.7 1.79-2.06 0-2.87-.92-2.98-2.1H6.3c.12 2.19 1.76 3.42 3.2 3.83V21h3v-2.15c1.95-.37 3.5-1.5 3.5-3.55 0-2.84-2.43-3.81-4.7-4.4z' fill='currentColor'/%3E%3C/svg%3E",
  'ico-people': "data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 24 24'%3E%3Cpath d='M16 11c1.66 0 3-1.34 3-3s-1.34-3-3-3-3 1.34-3 3 1.34 3 3 3zm-8 0c1.66 0 3-1.34 3-3S9.66 5 8 5 5 6.34 5 8s1.34 3 3 3zm0 2c-2.33 0-7 1.17-7 3.5V19h14v-2.5c0-2.33-4.67-3.5-7-3.5zm8 0c-.29 0-.62.02-.97.05 1.16.84 1.97 1.97 1.97 3.45V19h6v-2.5c0-2.33-4.67-3.5-7-3.5z' fill='currentColor'/%3E%3C/svg%3E",
  'ico-report': "data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 24 24'%3E%3Cpath d='M19 3H5a2 2 0 0 0-2 2v14a2 2 0 0 0 2 2h14a2 2 0 0 0 2-2V5a2 2 0 0 0-2-2zM9 17H7v-7h2v7zm4 0h-2V7h2v10zm4 0h-2v-4h2v4z' fill='currentColor'/%3E%3C/svg%3E",
  'ico-dial': "data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 24 24'%3E%3Cpath d='M6.62 10.79a15.05 15.05 0 0 0 6.59 6.59l2.2-2.2a1 1 0 0 1 1.01-.24c1.12.37 2.33.57 3.58.57a1 1 0 0 1 1 1V20a1 1 0 0 1-1 1C10.85 21 3 13.15 3 3.5a1 1 0 0 1 1-1H7.5a1 1 0 0 1 1 1c0 1.25.2 2.46.57 3.58a1 1 0 0 1-.25 1.01l-2.2 2.2z' fill='currentColor'/%3E%3C/svg%3E"
}

/* ---------- 手绘贴纸 data URI（自带颜色，<image> 直引） ---------- */
export const STICKER_URI = {
  'hand-bubble': 'data:image/svg+xml;base64,PHN2ZyB4bWxucz0iaHR0cDovL3d3dy53My5vcmcvMjAwMC9zdmciIHZpZXdCb3g9IjAgMCA2NCA2NCI+PHBhdGggZD0iTTggMTBxLTQgMC00IDV2MjJxMCA1IDQgNWg2djhsOS04aDI1cTQgMCA0LTVWMTVxMC01LTQtNXoiIGZpbGw9IiNGRjhBOEEiIHN0cm9rZT0iI0U4NkE2QSIgc3Ryb2tlLXdpZHRoPSIyLjUiLz48L3N2Zz4K',
  'hand-cloud': 'data:image/svg+xml;base64,PHN2ZyB4bWxucz0iaHR0cDovL3d3dy53My5vcmcvMjAwMC9zdmciIHZpZXdCb3g9IjAgMCA2NCA2NCI+PHBhdGggZD0iTTE0IDQ0cS04IDAtOC04dDgtOHExLTkgOS0xMXQxNCA0cTgtMSAxMiA1dC0xIDEzcS0yIDUtNyA1eiIgZmlsbD0iI0ZGRDlBMCIgc3Ryb2tlPSIjRThBNzU1IiBzdHJva2Utd2lkdGg9IjIuNSIvPjwvc3ZnPgo=',
  'hand-flower': 'data:image/svg+xml;base64,PHN2ZyB4bWxucz0iaHR0cDovL3d3dy53My5vcmcvMjAwMC9zdmciIHZpZXdCb3g9IjAgMCA2NCA2NCI+PHBhdGggZD0iTTMyIDIwcTktMTQgMTQtNnQtOCAxNHExMC04IDE0IDB0LTEyIDEycTYgNCAyIDExdC0xNCAxcTMgOC00IDEydC0xMi04cS04IDYtMTMtMnQ5LTExcS05IDItOS03dDE0LTZxLTYtOCAxLTE0dDEzIDh6IiBmaWxsPSIjRkZCMzQ3IiBzdHJva2U9IiNFMDg2MjAiIHN0cm9rZS13aWR0aD0iMi41Ii8+PC9zdmc+Cg==',
  'hand-heart': 'data:image/svg+xml;base64,PHN2ZyB4bWxucz0iaHR0cDovL3d3dy53My5vcmcvMjAwMC9zdmciIHZpZXdCb3g9IjAgMCA2NCA2NCI+PHBhdGggZD0iTTMyIDU2cS0yLTktMTAtMTZUNiAzMHEwLTExIDEwLTE1dDE2IDRxNi04IDE2LTR0MTAgMTVxMCAxMC04IDE1dC0xMiAxMXoiIGZpbGw9IiNGRjhBOEEiIHN0cm9rZT0iI0U4NkE2QSIgc3Ryb2tlLXdpZHRoPSIyLjUiLz48L3N2Zz4K',
  'hand-leaf': 'data:image/svg+xml;base64,PHN2ZyB4bWxucz0iaHR0cDovL3d3dy53My5vcmcvMjAwMC9zdmciIHZpZXdCb3g9IjAgMCA2NCA2NCI+PHBhdGggZD0iTTU2IDhxNCAyNi0xOCA0MlQ2IDQ2cS0yLTI2IDE4LTQwVDU2IDh6IiBmaWxsPSIjQThEOThBIiBzdHJva2U9IiM3Q0I4NUEiIHN0cm9rZS13aWR0aD0iMi41Ii8+PC9zdmc+Cg==',
  'hand-sparkle': 'data:image/svg+xml;base64,PHN2ZyB4bWxucz0iaHR0cDovL3d3dy53My5vcmcvMjAwMC9zdmciIHZpZXdCb3g9IjAgMCA2NCA2NCI+PHBhdGggZD0iTTMyIDJxMyAyMCA4IDI2dDI2IDZxLTIwIDMtMjYgMTB0LTggMjZxLTMtMjAtOC0yNlQtMiAzNHEyMC0zIDI2LTEwVDMyIDJ6IiBmaWxsPSIjRkZEMTY2IiBzdHJva2U9IiNFMEE5MkUiIHN0cm9rZS13aWR0aD0iMi41Ii8+PC9zdmc+Cg==',
  'hand-star': 'data:image/svg+xml;base64,PHN2ZyB4bWxucz0iaHR0cDovL3d3dy53My5vcmcvMjAwMC9zdmciIHZpZXdCb3g9IjAgMCA2NCA2NCI+PHBhdGggZD0iTTMyIDRxMyAxNCAxMCAyMXQyMSA5cS0xMiAyLTE5IDlUMzIgNjBxLTItMTItOS0xOFQ0IDM0cTEyLTIgMTktOVQzMiA0eiIgZmlsbD0iI0ZGQjM0NyIgc3Ryb2tlPSIjRTA4NjIwIiBzdHJva2Utd2lkdGg9IjIuNSIvPjwvc3ZnPgo=',
  'hand-sun': 'data:image/svg+xml;base64,PHN2ZyB4bWxucz0iaHR0cDovL3d3dy53My5vcmcvMjAwMC9zdmciIHZpZXdCb3g9IjAgMCA2NCA2NCI+PHBhdGggZD0iTTMyIDEyYTIwIDIwIDAgMSAxIDAgNDAgMjAgMjAgMCAwIDEgMC00MHoiIGZpbGw9IiNGRkQxNjYiIHN0cm9rZT0iI0UwQTkyRSIgc3Ryb2tlLXdpZHRoPSIyLjUiLz48L3N2Zz4K'
}

/**
 * 获取图标 data URI（优先 ICON_URI，未命中回退文件路径）
 * @param {string} name 图标名（如 ico-home / ico-phone-mobile）
 * @returns {string} data URI 字符串
 */
export function iconDataUri(name) {
  return ICON_URI[name] || ''
}

/**
 * 生成 CSS mask 所需的完整 url(...) 值
 * @param {string} name 图标名
 * @returns {string} 如 "url('data:image/svg+xml;base64,...')"
 */
export function iconMaskUrl(name) {
  const uri = ICON_URI[name]
  return uri ? `url('${uri}')` : ''
}

/**
 * 21 个功能图标名称→中文映射（T31.3 设计方案 §十）
 * 手机/微信/座位/确认/删除/分享/导出/设置/投诉/头像/加号/锁/解锁/编辑/日历/金额/人数/报表/推广码/拨号/二维码
 */
export const ICON_NAMES = {
  'ico-phone-mobile': '手机',
  'ico-wechat': '微信',
  'ico-seat': '座位',
  'ico-check': '确认',
  'ico-delete': '删除',
  'ico-share': '分享',
  'ico-export': '导出',
  'ico-setting': '设置',
  'ico-feedback': '投诉',
  'ico-mine': '头像',
  'ico-plus': '加号',
  'ico-lock': '锁',
  'ico-unlock': '解锁',
  'ico-edit': '编辑',
  'ico-calendar': '日历',
  'ico-amount': '金额',
  'ico-people': '人数',
  'ico-report': '报表',
  'ico-qrcode': '推广码',
  'ico-dial': '拨号',
  'ico-qr': '二维码'
}
