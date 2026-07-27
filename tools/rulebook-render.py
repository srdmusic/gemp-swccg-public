#!/usr/bin/env python3
"""Rulebook renderer: rules.json -> self-contained HTML logic tree.

Outputs:
  resources/rulebook/rulebook.html           standalone file (doctype-wrapped)
  resources/rulebook/rulebook-artifact.html  fragment form for Artifact publishing
Regenerate any time with: python3 tools/rulebook-extract.py && python3 tools/rulebook-render.py
"""
import json
import os

REPO = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
RB = os.path.join(REPO, "resources", "rulebook")

PHASE_ORDER = ["SETUP", "ACTIVATE", "DRAW", "CONTROL", "DEPLOY", "BATTLE", "MOVE",
               "PULL", "SHIELDS", "FORCE_LOSS", "TARGETING", "RESPONSE", "PASS",
               "LEGACY_COORDINATOR", "OBJECTIVE", "OTHER"]



def build(data):
    head = data["head"]
    counts = data["counts"]
    payload = json.dumps(data["rules"], separators=(",", ":")).replace("</", "<\\/")
    phases = json.dumps(PHASE_ORDER)

    body = """
<title>Rando Rulebook @ __HEAD__</title>
<style>
:root{
  --bg:#f6f7f4; --panel:#ffffff; --panel2:#eceee8; --line:#d8dbd2;
  --ink:#23282e; --ink2:#5c6570; --ink3:#8a939e;
  --accent:#a97b17; --accent-soft:#f3e5c3;
  --boost:#2e7d4f; --boost-bg:#e2efe6; --pen:#a8443c; --pen-bg:#f4e3e1;
  --veto:#7c231d; --veto-bg:#efd0cc; --chip:#eef0ea;
}
@media (prefers-color-scheme: dark){:root{
  --bg:#14181f; --panel:#1b212b; --panel2:#232b37; --line:#2e3743;
  --ink:#dde3ea; --ink2:#95a0ac; --ink3:#67707c;
  --accent:#e8b23c; --accent-soft:#33301f;
  --boost:#5cb37e; --boost-bg:#1e3227; --pen:#d97b71; --pen-bg:#3a2422;
  --veto:#e89a92; --veto-bg:#4a231f; --chip:#242c38;
}}
:root[data-theme="dark"]{
  --bg:#14181f; --panel:#1b212b; --panel2:#232b37; --line:#2e3743;
  --ink:#dde3ea; --ink2:#95a0ac; --ink3:#67707c;
  --accent:#e8b23c; --accent-soft:#33301f;
  --boost:#5cb37e; --boost-bg:#1e3227; --pen:#d97b71; --pen-bg:#3a2422;
  --veto:#e89a92; --veto-bg:#4a231f; --chip:#242c38;
}
:root[data-theme="light"]{
  --bg:#f6f7f4; --panel:#ffffff; --panel2:#eceee8; --line:#d8dbd2;
  --ink:#23282e; --ink2:#5c6570; --ink3:#8a939e;
  --accent:#a97b17; --accent-soft:#f3e5c3;
  --boost:#2e7d4f; --boost-bg:#e2efe6; --pen:#a8443c; --pen-bg:#f4e3e1;
  --veto:#7c231d; --veto-bg:#efd0cc; --chip:#eef0ea;
}
*{box-sizing:border-box}
body{background:var(--bg);color:var(--ink);margin:0;
  font:16px/1.5 "Avenir Next","Avenir","Segoe UI",system-ui,sans-serif}
.wrap{max-width:1060px;margin:0 auto;padding:20px 16px 80px}
header.top h1{font-size:26px;font-weight:600;margin:0;text-wrap:balance}
header.top .sub{color:var(--ink2);font-size:14px;margin-top:4px}
.mono{font-family:"SF Mono",Menlo,Consolas,monospace;font-variant-numeric:tabular-nums}
.bar{position:sticky;top:0;z-index:5;background:var(--bg);padding:12px 0 10px;
  border-bottom:1px solid var(--line);display:flex;flex-direction:column;gap:10px}
.bar input[type=search]{width:100%;padding:10px 14px;font-size:16px;border:1px solid var(--line);
  border-radius:8px;background:var(--panel);color:var(--ink)}
.bar input[type=search]:focus{outline:2px solid var(--accent);outline-offset:1px}
.chips{display:flex;flex-wrap:wrap;gap:6px}
.chip{border:1px solid var(--line);background:var(--chip);color:var(--ink2);border-radius:16px;
  padding:4px 12px;font-size:13px;cursor:pointer;user-select:none;letter-spacing:.02em}
.chip.on{background:var(--accent-soft);border-color:var(--accent);color:var(--ink);font-weight:600}
.chip:focus-visible{outline:2px solid var(--accent)}
.toolrow{display:flex;flex-wrap:wrap;gap:6px;align-items:center}
.toolrow .spacer{flex:1}
.btn{border:1px solid var(--line);background:var(--panel);color:var(--ink);border-radius:8px;
  padding:6px 12px;font-size:13px;cursor:pointer}
.btn.on{background:var(--accent-soft);border-color:var(--accent);font-weight:600}
.btn:focus-visible{outline:2px solid var(--accent)}
.phase{margin-top:26px}
.phase>h2{font-size:14px;letter-spacing:.14em;color:var(--ink2);font-weight:700;
  text-transform:uppercase;margin:0 0 8px;display:flex;gap:10px;align-items:baseline}
.phase>h2 .n{color:var(--ink3);font-weight:400;letter-spacing:0}
.policy{background:var(--panel);border:1px solid var(--line);border-radius:10px;margin-bottom:8px;overflow:hidden}
.policy>.head{display:flex;align-items:center;gap:12px;padding:10px 14px;cursor:pointer;user-select:none}
.policy>.head:hover{background:var(--panel2)}
.policy>.head .name{font-weight:600;font-size:15px}
.policy>.head .meta{color:var(--ink3);font-size:13px}
.policy>.head .stack{margin-left:auto;font-size:13px;display:flex;gap:8px}
.stack .pos{color:var(--boost)} .stack .neg{color:var(--pen)}
.arm{display:flex;gap:12px;padding:10px 14px;border-top:1px solid var(--line);align-items:flex-start}
.arm.flagged{box-shadow:inset 3px 0 0 var(--accent)}
.delta{min-width:74px;text-align:right;font-weight:700;font-size:15px;padding:2px 8px;border-radius:6px}
.delta.pos{color:var(--boost);background:var(--boost-bg)}
.delta.neg{color:var(--pen);background:var(--pen-bg)}
.delta.veto{color:var(--veto);background:var(--veto-bg)}
.delta.dyn{color:var(--ink2);background:var(--chip);font-weight:600}
.armbody{flex:1;min-width:0}
.reason{font-size:15px;overflow-wrap:anywhere}
.meta2{margin-top:3px;font-size:12.5px;color:var(--ink3);display:flex;flex-wrap:wrap;gap:4px 10px}
.meta2 .id{color:var(--ink2)}
.tag{background:var(--chip);border-radius:4px;padding:0 6px}
.tag.kV{color:var(--veto)} .tag.kO{color:var(--ink2)} .tag.kB{color:var(--accent)}
.flagbtn{border:1px solid var(--line);background:none;border-radius:6px;color:var(--ink3);
  cursor:pointer;padding:4px 8px;font-size:13px;flex-shrink:0}
.flagbtn.on{color:var(--accent);border-color:var(--accent);font-weight:700}
.flagpop{border-top:1px dashed var(--line);padding:10px 14px;background:var(--panel2);
  display:flex;flex-wrap:wrap;gap:8px;align-items:center}
.flagpop button{border:1px solid var(--line);border-radius:6px;background:var(--panel);
  color:var(--ink);padding:6px 12px;cursor:pointer;font-size:13px}
.flagpop button.sel{background:var(--accent-soft);border-color:var(--accent);font-weight:700}
.flagpop input{flex:1;min-width:180px;padding:6px 10px;border:1px solid var(--line);
  border-radius:6px;background:var(--panel);color:var(--ink);font-size:14px}
.count{color:var(--ink2);font-size:14px;margin:14px 2px}
.empty{color:var(--ink3);padding:30px;text-align:center}
@media (prefers-reduced-motion:no-preference){.policy .armwrap{transition:none}}
</style>
<div class="wrap">
<header class="top">
  <h1>Rando Rulebook</h1>
  <div class="sub">Every scoring rule in the AI, generated straight from the code.
   HEAD <span class="mono">__HEAD__</span> · __TOTAL__ arms ·
   regenerate: <span class="mono">tools/rulebook-extract.py && tools/rulebook-render.py</span></div>
</header>
<div class="bar">
  <input id="q" type="search" placeholder="Search reasons, rule ids, V-tags, policies… (e.g. rack, V51, drain)" aria-label="Search rules">
  <div class="chips" id="phasechips" role="group" aria-label="Phase filters"></div>
  <div class="toolrow">
    <button class="btn" id="fVeto">Vetoes</button>
    <button class="btn" id="fBoost">Boosts</button>
    <button class="btn" id="fPen">Penalties</button>
    <button class="btn" id="fDyn">Computed</button>
    <button class="btn" id="fWeak">Weak IDs</button>
    <button class="btn" id="fFlag">Flagged</button>
    <span class="spacer"></span>
    <button class="btn" id="dom">Dominance sort</button>
    <button class="btn" id="exp">Export flags</button>
  </div>
</div>
<div class="count" id="count"></div>
<main id="tree"></main>
</div>
<script>
const RULES=__PAYLOAD__;
const PHASES=__PHASES__;
RULES.forEach((r,i)=>{r._k=r.id+"|"+r.file+"|"+r.line;r._i=i;});
let flags={};try{flags=JSON.parse(localStorage.getItem("rulebook-flags")||"{}");}catch(e){}
const state={q:"",phases:new Set(),veto:false,boost:false,pen:false,dyn:false,weak:false,flag:false,dom:false,open:new Set(),pop:null};
function saveFlags(){localStorage.setItem("rulebook-flags",JSON.stringify(flags));}
function deltaClass(r){if(r.delta===null)return"dyn";if(r.delta<=-1500)return"veto";return r.delta<0?"neg":"pos";}
function deltaText(r){if(r.delta===null)return"ƒ(x)";const d=r.delta;return(d>0?"+":"")+(Number.isInteger(d)?d:d.toFixed(1));}
function esc(s){return(s||"").replace(/[&<>"]/g,c=>({"&":"&amp;","<":"&lt;",">":"&gt;",'"':"&quot;"}[c]));}
function match(r){
  if(state.phases.size&&!state.phases.has(r.phase))return false;
  if(state.veto&&!(r.delta!==null&&r.delta<=-1500||r.kind==="VETO"))return false;
  if(state.boost&&!(r.delta!==null&&r.delta>0))return false;
  if(state.pen&&!(r.delta!==null&&r.delta<0&&r.delta>-1500))return false;
  if(state.dyn&&r.delta_kind!=="expression")return false;
  if(state.weak&&(r.id_source==="typed"))return false;
  if(state.flag&&!flags[r._k])return false;
  if(state.q){const q=state.q.toLowerCase();
    const hay=(r.id+" "+r.reason+" "+r.policy+" "+(r.provenance||[]).join(" ")+" "+(r.method||"")).toLowerCase();
    if(!hay.includes(q))return false;}
  return true;}
function groupKey(r){return r.phase+"::"+r.policy;}
function render(){
  const tree=document.getElementById("tree");
  const vis=RULES.filter(match);
  document.getElementById("count").textContent=vis.length+" of "+RULES.length+" rules shown"+(Object.keys(flags).length?" · "+Object.keys(flags).length+" flagged":"");
  const groups={};
  vis.forEach(r=>{(groups[groupKey(r)]=groups[groupKey(r)]||[]).push(r);});
  const autoOpen=!!state.q||state.flag||vis.length<=40;
  let html="";
  for(const ph of PHASES){
    const keys=Object.keys(groups).filter(k=>k.startsWith(ph+"::")).sort();
    if(!keys.length)continue;
    const phN=keys.reduce((a,k)=>a+groups[k].length,0);
    html+='<section class="phase"><h2>'+ph.replace(/_/g," ")+' <span class="n">'+phN+'</span></h2>';
    for(const k of keys){
      const arms=groups[k].slice();
      if(state.dom)arms.sort((a,b)=>Math.abs(b.delta??0)-Math.abs(a.delta??0));
      else arms.sort((a,b)=>a.line-b.line);
      const pol=k.split("::")[1];
      let pos=0,neg=0;arms.forEach(r=>{if(r.delta>0)pos+=r.delta;if(r.delta<0)neg+=r.delta;});
      const open=autoOpen||state.open.has(k);
      html+='<div class="policy"><div class="head" data-g="'+esc(k)+'" role="button" tabindex="0" aria-expanded="'+open+'">'
        +'<span class="name">'+esc(pol)+'</span><span class="meta">'+arms.length+' arms</span>'
        +'<span class="stack mono"><span class="pos">+'+Math.round(pos)+'</span><span class="neg">'+Math.round(neg)+'</span></span></div>';
      if(open){html+='<div class="armwrap">';
        for(const r of arms){
          const f=flags[r._k];
          html+='<div class="arm'+(f?" flagged":"")+'" data-k="'+esc(r._k)+'">'
            +'<span class="delta mono '+deltaClass(r)+'" title="'+esc(r.delta_expr||"")+'">'+deltaText(r)+'</span>'
            +'<div class="armbody"><div class="reason">'+esc(r.reason)+'</div>'
            +'<div class="meta2"><span class="id mono">'+esc(r.id)+'</span>'
            +(r.kind?'<span class="tag k'+r.kind[0]+'">'+r.kind+'</span>':"")
            +(r.provenance||[]).map(v=>'<span class="tag">'+esc(v)+'</span>').join("")
            +(r.id_source!=="typed"?'<span class="tag">'+esc(r.id_source)+'</span>':"")
            +'<span class="mono">'+esc(r.policy)+'.java:'+r.line+'</span></div>'
            +(f&&f.note?'<div class="meta2">🔖 '+esc(f.verdict)+": "+esc(f.note)+'</div>':(f?'<div class="meta2">🔖 '+esc(f.verdict)+'</div>':""))
            +'</div>'
            +'<button class="flagbtn'+(f?" on":"")+'" data-k="'+esc(r._k)+'" aria-label="Flag this rule">'+(f?esc(f.verdict):"flag")+'</button></div>';
          if(state.pop===r._k){
            html+='<div class="flagpop" data-k="'+esc(r._k)+'">';
            for(const v of["kill","nerf","boost","question"]){
              html+='<button data-v="'+v+'" class="'+(f&&f.verdict===v?"sel":"")+'">'+v+'</button>';}
            html+='<input placeholder="note (optional)" value="'+esc(f&&f.note||"")+'">'
              +'<button data-v="__clear">clear</button></div>';}
        }
        html+='</div>';}
      html+='</div>';
    }
    html+='</section>';
  }
  tree.innerHTML=html||'<div class="empty">No rules match. Clear a filter.</div>';
}
document.getElementById("tree").addEventListener("click",e=>{
  const head=e.target.closest(".head");
  if(head){const g=head.dataset.g;state.open.has(g)?state.open.delete(g):state.open.add(g);render();return;}
  const fb=e.target.closest(".flagbtn");
  if(fb){state.pop=state.pop===fb.dataset.k?null:fb.dataset.k;render();return;}
  const pop=e.target.closest(".flagpop");
  if(pop&&e.target.dataset.v){
    const k=pop.dataset.k;const note=pop.querySelector("input").value.trim();
    if(e.target.dataset.v==="__clear"){delete flags[k];}
    else{flags[k]={verdict:e.target.dataset.v,note:note};}
    saveFlags();state.pop=null;render();}
});
document.getElementById("tree").addEventListener("keydown",e=>{
  if(e.key==="Enter"&&e.target.classList.contains("head")){e.target.click();}});
document.getElementById("tree").addEventListener("change",e=>{
  const pop=e.target.closest(".flagpop");
  if(pop&&e.target.tagName==="INPUT"){const k=pop.dataset.k;
    if(flags[k]){flags[k].note=e.target.value.trim();saveFlags();}}});
const chipbox=document.getElementById("phasechips");
PHASES.forEach(p=>{
  if(!RULES.some(r=>r.phase===p))return;
  const b=document.createElement("button");
  b.className="chip";b.textContent=p.replace(/_/g," ");b.setAttribute("aria-pressed","false");
  b.onclick=()=>{state.phases.has(p)?state.phases.delete(p):state.phases.add(p);
    b.classList.toggle("on");b.setAttribute("aria-pressed",b.classList.contains("on"));render();};
  chipbox.appendChild(b);});
let deb;
document.getElementById("q").addEventListener("input",e=>{
  clearTimeout(deb);deb=setTimeout(()=>{state.q=e.target.value.trim();render();},120);});
for(const[bid,key]of[["fVeto","veto"],["fBoost","boost"],["fPen","pen"],["fDyn","dyn"],["fWeak","weak"],["fFlag","flag"],["dom","dom"]]){
  const b=document.getElementById(bid);
  b.onclick=()=>{state[key]=!state[key];b.classList.toggle("on");render();};}
document.getElementById("exp").onclick=()=>{
  const out=Object.entries(flags).map(([k,v])=>{
    const r=RULES.find(x=>x._k===k)||{};
    return{id:r.id,verdict:v.verdict,note:v.note||"",delta:r.delta,reason:r.reason,
      policy:r.policy,file:r.file,line:r.line,phase:r.phase};});
  const blob=new Blob([JSON.stringify({exported:new Date().toISOString(),head:"__HEAD__",flags:out},null,1)],{type:"application/json"});
  const a=document.createElement("a");a.href=URL.createObjectURL(blob);
  a.download="rulebook-flags.json";a.click();
  navigator.clipboard&&navigator.clipboard.writeText(JSON.stringify(out,null,1)).catch(()=>{});};
render();
</script>
"""
    body = (body
            .replace("__HEAD__", head)
            .replace("__TOTAL__", str(counts["total"]))
            .replace("__PAYLOAD__", payload)
            .replace("__PHASES__", phases))
    return body


def main():
    data = json.load(open(os.path.join(RB, "rules.json")))
    frag = build(data)
    with open(os.path.join(RB, "rulebook-artifact.html"), "w") as f:
        f.write(frag)
    with open(os.path.join(RB, "rulebook.html"), "w") as f:
        f.write("<!doctype html>\n<html>\n<head>\n<meta charset=\"utf-8\">\n"
                "<meta name=\"viewport\" content=\"width=device-width,initial-scale=1\">\n"
                "</head>\n<body>\n" + frag + "\n</body>\n</html>\n")
    print("wrote rulebook.html (%d KB) and rulebook-artifact.html" %
          (os.path.getsize(os.path.join(RB, "rulebook.html")) // 1024))


if __name__ == "__main__":
    main()
