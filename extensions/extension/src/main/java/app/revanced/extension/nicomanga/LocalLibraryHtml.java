package app.revanced.extension.nicomanga;

final class LocalLibraryHtml {
    private LocalLibraryHtml() {}

    static String create(Translations translations) {
        return HTML.replace("__I18N__", translations.toJson())
                .replace("__DIR__", translations.isRtl() ? "rtl" : "ltr");
    }

    private static final String HTML = """
            <!doctype html>
            <html lang="en" dir="__DIR__">
            <head>
              <meta charset="utf-8">
              <meta name="viewport" content="width=device-width,initial-scale=1,viewport-fit=cover">
              <style>
                :root{color-scheme:dark;--bg:#0d0d0f;--card:#1b1b1f;--line:#34343b;--text:#f7f7f8;--muted:#b7b7bf;--accent:#f0c968;--danger:#ef6464}
                *{box-sizing:border-box}html,body{margin:0;min-height:100%;background:var(--bg);color:var(--text);font-family:system-ui,-apple-system,sans-serif}
                body{padding:env(safe-area-inset-top) 16px 24px}.header{position:sticky;top:0;z-index:2;display:flex;align-items:center;gap:12px;background:linear-gradient(var(--bg) 82%,transparent);padding:18px 0 22px}
                h1{font-size:clamp(22px,6vw,34px);margin:0;flex:1}.close{border:1px solid var(--line);background:var(--card);color:var(--text);border-radius:999px;padding:10px 16px;font-weight:700}
                #status{color:var(--muted);min-height:24px}.empty{margin:14vh auto 0;max-width:36ch;text-align:center;color:var(--muted);font-size:18px;line-height:1.6}
                .card{background:var(--card);border:1px solid var(--line);border-radius:18px;padding:16px;margin:0 0 14px;box-shadow:0 8px 24px #0005}
                .card h2{font-size:20px;margin:0 0 8px;overflow-wrap:anywhere}.meta{color:var(--muted);display:flex;flex-wrap:wrap;gap:8px 16px;margin-bottom:12px}
                .progress{height:10px;background:#34343b;border-radius:99px;overflow:hidden;margin:10px 0}.progress>i{display:block;height:100%;background:linear-gradient(90deg,#f0c968,#f09c68);border-radius:inherit}
                .actions{display:flex;gap:10px;flex-wrap:wrap}.actions button{border:0;border-radius:12px;padding:10px 14px;font-weight:800;font-size:15px;background:var(--accent);color:#17130b}
                .actions .remove{background:transparent;color:var(--danger);border:1px solid #713c3c}.percent{font-weight:800;color:var(--accent)}
                @media(max-width:420px){body{padding-inline:12px}.card{padding:14px}.actions button{flex:1}}
              </style>
            </head>
            <body>
              <div class="header"><h1 id="heading"></h1><button class="close" id="close"></button></div>
              <div id="status" role="status" aria-live="polite"></div><main id="items"></main>
              <script>
                'use strict';
                const I18N=__I18N__,DB_NAME='nicomanga-revanced',DB_VERSION=1;
                let db=null,screen='list',recoveryAttempted=false;
                const t=k=>I18N[k]||k;
                const validNumber=(v,f=1)=>Number.isFinite(Number(v))?Math.max(1,Math.trunc(Number(v))):f;
                const normalize=r=>({id:String(r.id||r.title||'').trim().toLowerCase(),title:String(r.title||'Nicomanga').trim(),totalChapters:validNumber(r.totalChapters),addedAt:Number(r.addedAt)||Date.now(),lastChapter:validNumber(r.lastChapter),lastPage:validNumber(r.lastPage),totalPages:validNumber(r.totalPages),completedChapters:Array.isArray(r.completedChapters)?r.completedChapters.map(v=>validNumber(v)):[],updatedAt:Number(r.updatedAt)||Date.now()});
                function openDatabase(){
                  const request=indexedDB.open(DB_NAME,DB_VERSION);
                  request.onupgradeneeded=e=>{const d=e.target.result;if(!d.objectStoreNames.contains('library'))d.createObjectStore('library',{keyPath:'id'});if(!d.objectStoreNames.contains('history'))d.createObjectStore('history',{keyPath:'id'});if(!d.objectStoreNames.contains('meta'))d.createObjectStore('meta',{keyPath:'key'});};
                  request.onsuccess=e=>{db=e.target.result;db.onversionchange=()=>db.close();const tx=db.transaction('meta','readwrite');tx.objectStore('meta').put({key:'schemaVersion',value:DB_VERSION});render();Android.ready();};
                  request.onerror=recoverDatabase;
                  request.onblocked=()=>setStatus(t('storageError'));
                }
                function recoverDatabase(){if(recoveryAttempted){setStatus(t('storageError'));return}recoveryAttempted=true;const del=indexedDB.deleteDatabase(DB_NAME);del.onsuccess=()=>{Android.storageRecovered();openDatabase()};del.onerror=()=>setStatus(t('storageError'));}
                function setStatus(message){document.getElementById('status').textContent=message||''}
                function store(name,mode='readonly'){if(!db)throw new Error('IndexedDB not ready');return db.transaction(name,mode).objectStore(name)}
                function putList(raw){const r=normalize(JSON.parse(raw));const req=store('library','readwrite').put(r);req.onsuccess=()=>{setStatus(t('added'));render()};req.onerror=recoverDatabase;}
                function putHistory(raw){const incoming=normalize(JSON.parse(raw));const objectStore=store('history','readwrite'),get=objectStore.get(incoming.id);get.onsuccess=()=>{const old=get.result||incoming;const done=new Set([...(old.completedChapters||[]),...incoming.completedChapters]);objectStore.put({...old,...incoming,completedChapters:[...done].sort((a,b)=>a-b)}).onsuccess=render};get.onerror=recoverDatabase;}
                function remove(name,id){const req=store(name,'readwrite').delete(id);req.onsuccess=render;req.onerror=recoverDatabase;}
                function all(name,callback){const req=store(name).getAll();req.onsuccess=()=>callback((req.result||[]).sort((a,b)=>(b.updatedAt||b.addedAt)-(a.updatedAt||a.addedAt)));req.onerror=recoverDatabase;}
                function element(tag,className,text){const node=document.createElement(tag);if(className)node.className=className;if(text!==undefined)node.textContent=text;return node;}
                function card(record,isHistory){
                  const node=element('article','card'),title=element('h2','',record.title);node.append(title);
                  const meta=element('div','meta');
                  if(isHistory){meta.append(element('span','',`${t('chapter')} ${record.lastChapter}`),element('span','',`${t('page')} ${record.lastPage}/${record.totalPages}`));
                    const percent=Math.min(100,Math.round(((record.completedChapters||[]).length/validNumber(record.totalChapters))*100));meta.append(element('span','percent',`${percent}% ${t('read')}`));
                    const bar=element('div','progress'),fill=element('i');fill.style.width=percent+'%';bar.append(fill);node.append(meta,bar);
                  }else{meta.append(element('span','',`${record.totalChapters} ${t('chapter')}`));node.append(meta)}
                  const actions=element('div','actions'),resume=element('button','',t('resume')),del=element('button','remove',t('remove'));
                  resume.onclick=()=>Android.resume(JSON.stringify(record));del.onclick=()=>remove(isHistory?'history':'library',record.id);actions.append(resume,del);node.append(actions);return node;
                }
                function render(){if(!db)return;document.getElementById('heading').textContent=screen==='history'?t('history'):t('list');document.getElementById('close').textContent=t('close');setStatus('');all(screen==='history'?'history':'library',rows=>{const root=document.getElementById('items');root.replaceChildren();if(!rows.length){root.append(element('p','empty',screen==='history'?t('emptyHistory'):t('emptyList')));return}rows.forEach(row=>root.append(card(row,screen==='history')));});}
                document.getElementById('close').onclick=()=>Android.close();
                window.NMR={show(next){screen=next==='history'?'history':'list';render()},upsertList:putList,upsertHistory:putHistory};
                openDatabase();
              </script>
            </body></html>
            """;
}
