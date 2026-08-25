package app.revanced.extension.nicomanga;

final class LocalHomeHtml {
    private LocalHomeHtml() {}

    static String create(Translations translations) {
        return HTML.replace("__I18N__", translations.toJson())
                .replace("__DIR__", translations.isRtl() ? "rtl" : "ltr");
    }

    private static final String HTML = """
            <!doctype html><html lang="en" dir="__DIR__"><head>
            <meta charset="utf-8"><meta name="viewport" content="width=device-width,initial-scale=1,viewport-fit=cover">
            <style>
            :root{color-scheme:dark;--bg:#0d0d0f;--panel:#09090b;--line:#34343b;--text:#f7f7f8;--muted:#b7b7bf;--accent:#f0c968}
            *{box-sizing:border-box}html,body{margin:0;min-height:100%;background:var(--bg);color:var(--text);font-family:Georgia,serif}body{padding:0 12px 28px}
            header{position:sticky;top:0;z-index:4;display:flex;align-items:center;min-height:82px;background:#000;border-bottom:1px solid #777;margin:0 -12px;padding:env(safe-area-inset-top) 18px 0}
            h1{font:700 clamp(25px,7vw,38px) system-ui,sans-serif;text-align:center;flex:1;margin:0}.search{position:absolute;inset-inline-end:16px;border:0;background:transparent;color:#fff;font-size:36px;padding:8px}
            nav{position:sticky;top:82px;z-index:3;display:grid;grid-template-columns:repeat(4,1fr);gap:3px;background:var(--bg);padding:8px 0 10px}
            nav button{min-height:48px;border:1px solid #17171a;border-radius:7px;background:#030304;color:#dfd0b8;font:800 14px Georgia,serif;letter-spacing:.06em}
            nav button.active{color:var(--accent);border-color:#6d5b2d}.notice{display:none;margin:2px 0 12px;padding:14px;border:1px solid #5e512d;border-radius:14px;background:#201d14}.notice.show{display:block}.notice strong{display:block;color:var(--accent);margin-bottom:5px}.notice p{margin:0;color:var(--muted);line-height:1.45}
            main{display:grid;grid-template-columns:repeat(auto-fill,minmax(min(42vw,190px),1fr));gap:14px;align-items:start}.card{position:relative;min-width:0;border:0;border-radius:15px;overflow:hidden;background:#111;color:#fff;padding:0;box-shadow:0 8px 24px #0008;text-align:start;aspect-ratio:2/3}
            .card img{position:absolute;inset:0;width:100%;height:100%;object-fit:cover;background:#202024}.shade{position:absolute;inset:45% 0 0;background:linear-gradient(transparent,#000 40%,#000e);padding:70px 10px 12px;display:flex;flex-direction:column;justify-content:flex-end;overflow:hidden}
            .title{font-size:clamp(14px,3.7vw,19px);font-weight:800;line-height:1.14;max-height:2.3em;overflow:hidden}.meta{font:13px system-ui,sans-serif;color:#ddd;white-space:nowrap;overflow:hidden;text-overflow:ellipsis;margin-top:5px}.genre{color:#ff7474}.chapter{position:absolute;top:9px;inset-inline-end:9px;background:#e0c68f;color:#111;border-radius:8px;padding:4px 8px;font:800 12px system-ui,sans-serif}
            .skeleton{animation:pulse 1.2s infinite alternate;background:#202024}@keyframes pulse{to{background:#303036}}@media(min-width:700px){main{grid-template-columns:repeat(auto-fill,minmax(190px,1fr));gap:18px}.card{max-height:430px}}
            </style></head><body><header><h1 id="heading"></h1><button class="search" id="search">⌕</button></header>
            <nav id="tabs"><button data-key="home">HOME</button><button data-key="new">NEW</button><button data-key="top">TOP</button><button data-key="update">UPDATE</button></nav>
            <section class="notice" id="notice"><strong id="noticeTitle"></strong><p id="noticeBody"></p></section><main id="grid"></main>
            <script>'use strict';const I18N=__I18N__,t=k=>I18N[k]||k;let payload={},category='home',showNotice=false;const grid=document.getElementById('grid');
            function text(v){return String(v==null?'':v)}function skeleton(){grid.replaceChildren();for(let i=0;i<8;i++){const e=document.createElement('div');e.className='card skeleton';grid.append(e)}}
            function card(r){const b=document.createElement('button');b.className='card';b.onclick=()=>Android.openManga(text(r.id));const img=document.createElement('img');img.alt='';img.loading='lazy';img.src=text(r.cover);const badge=document.createElement('span');badge.className='chapter';badge.textContent='Ch '+text(r.lastChapter||'?');const shade=document.createElement('span');shade.className='shade';const title=document.createElement('span');title.className='title';title.textContent=text(r.name||'Nicomanga');const meta=document.createElement('span');meta.className='meta';meta.textContent=text(r.authors||r.artists);const genre=document.createElement('span');genre.className='meta genre';genre.textContent=text(r.genres).split(',')[0];shade.append(title,meta,genre);b.append(img,badge,shade);return b}
            function render(){document.getElementById('heading').textContent=t('home');document.getElementById('search').title=t('search');document.getElementById('noticeTitle').textContent=t('devNoticeTitle');document.getElementById('noticeBody').textContent=t('devNoticeBody');document.getElementById('notice').classList.toggle('show',showNotice);document.querySelectorAll('nav button').forEach(b=>b.classList.toggle('active',b.dataset.key===category));const rows=payload[category==='home'?'top':category];if(!Array.isArray(rows)||!rows.length){skeleton();return}grid.replaceChildren(...rows.map(card))}
            document.getElementById('tabs').onclick=e=>{const key=e.target.dataset.key;if(key){category=key;render();scrollTo({top:0,behavior:'smooth'})}};document.getElementById('search').onclick=()=>Android.search();window.NMRHome={update(raw){try{payload=JSON.parse(raw);localStorage.setItem('homePayload',raw)}catch(e){}render()},notice(v){showNotice=!!v;render()}};try{payload=JSON.parse(localStorage.getItem('homePayload')||'{}')}catch(e){}skeleton();render();Android.ready();</script>
            </body></html>
            """;
}
