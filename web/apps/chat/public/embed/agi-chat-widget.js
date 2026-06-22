var AgiChatWidget=function(g){"use strict";const f="agi-chat-widget-style";function I(e,r){const o=document.getElementById(f);o&&o.remove();const c=document.createElement("style");c.id=f,c.textContent=`
    .agi-chat-widget-launcher {
      position: fixed;
      bottom: 24px;
      width: 56px;
      height: 56px;
      border: none;
      border-radius: 50%;
      background: ${r};
      color: #fff;
      font-size: 24px;
      cursor: pointer;
      box-shadow: 0 8px 24px color-mix(in srgb, ${r} 35%, transparent);
      z-index: ${e};
    }
    .agi-chat-widget-launcher.right { right: 24px; }
    .agi-chat-widget-launcher.left { left: 24px; }
    .agi-chat-widget-panel {
      position: fixed;
      bottom: 96px;
      width: min(420px, calc(100vw - 32px));
      height: min(680px, calc(100vh - 120px));
      border: 1px solid #e7ecf3;
      border-radius: 16px;
      overflow: hidden;
      background: #fff;
      box-shadow: 0 16px 48px rgba(15, 23, 42, 0.18);
      z-index: ${e+1};
      display: none;
    }
    .agi-chat-widget-panel.open { display: block; }
    .agi-chat-widget-panel.right { right: 24px; }
    .agi-chat-widget-panel.left { left: 24px; }
    .agi-chat-widget-header {
      height: 48px;
      display: flex;
      align-items: center;
      justify-content: space-between;
      padding: 0 12px 0 16px;
      border-bottom: 1px solid #e7ecf3;
      background: #fafcff;
      font: 600 14px/1.4 system-ui, sans-serif;
      color: #0f172a;
    }
    .agi-chat-widget-close {
      border: none;
      background: transparent;
      font-size: 20px;
      cursor: pointer;
      color: #64748b;
    }
    .agi-chat-widget-frame {
      width: 100%;
      height: calc(100% - 48px);
      border: 0;
    }
  `,document.head.appendChild(c)}function E(e){return((e==null?void 0:e.trim())||window.location.origin).replace(/\/$/,"")}function d(e){window.parent&&window.parent!==window&&window.parent.postMessage(e,"*")}function A(e){var y;const r=E(e.baseUrl),o=e.position==="left"?"left":"right",c=e.zIndex??2147483e3,m=((y=e.primaryColor)==null?void 0:y.trim())||"#1677ff";I(c,m);const t=document.createElement("button");t.type="button",t.className=`agi-chat-widget-launcher ${o}`,t.setAttribute("aria-label",e.title??"打开智能体对话"),t.textContent="💬";const n=document.createElement("div");n.className=`agi-chat-widget-panel ${o}`;const p=document.createElement("div");p.className="agi-chat-widget-header";const w=document.createElement("span");w.textContent=e.title??"智能体助手";const i=document.createElement("button");i.type="button",i.className="agi-chat-widget-close",i.setAttribute("aria-label","关闭"),i.textContent="×",p.append(w,i);const a=document.createElement("iframe");a.className="agi-chat-widget-frame",a.title=e.title??"智能体对话",a.allow="clipboard-write",n.append(p,a),document.body.append(t,n);let b=!1;window.addEventListener("message",l=>{var s;l.source===a.contentWindow&&((s=l.data)==null?void 0:s.type)==="AGI_CHAT_READY"&&d({type:"AGI_CHAT_WIDGET_READY",botId:e.botId})});async function C(){if(b)return;const l=await e.getTicket(),s=new URLSearchParams({embed:"1",ticket:l,primaryColor:m});a.src=`${r}/chat/bots/${encodeURIComponent(e.botId)}?${s.toString()}`,b=!0}async function x(){await C(),n.classList.add("open"),d({type:"AGI_CHAT_WIDGET_OPEN",botId:e.botId})}function u(){n.classList.remove("open"),d({type:"AGI_CHAT_WIDGET_CLOSE",botId:e.botId})}function T(){t.remove(),n.remove(),d({type:"AGI_CHAT_WIDGET_DESTROY",botId:e.botId})}return t.addEventListener("click",()=>{n.classList.contains("open")?u():x()}),i.addEventListener("click",u),{open:x,close:u,destroy:T}}const h={init(e){if(!(e!=null&&e.botId))throw new Error("botId is required");if(typeof e.getTicket!="function")throw new Error("getTicket is required");return A(e)}};return window.AgiChatWidget=h,g.AgiChatWidget=h,Object.defineProperty(g,Symbol.toStringTag,{value:"Module"}),g}({});
