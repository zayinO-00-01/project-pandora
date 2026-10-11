from pathlib import Path
root=Path(__file__).resolve().parent.parent
p=root/'docs/landing-preview/journey.html'
s=p.read_text(encoding='utf-8').replace('initial-frame-v2.png','initial-frame-v3.png').replace('landing-journey-v2.mp4','landing-journey-v3.mp4')
s=s.replace('background:var(--color-paper)}.brand','background:transparent}.brand')
s=s.replace('background:linear-gradient(var(--color-paper),transparent 68%)','background:linear-gradient(var(--color-paper),transparent 45%);opacity:.35')
s=s.replace('background:linear-gradient(var(--color-paper),transparent 85%)','background:linear-gradient(var(--color-paper),transparent 45%)')
s=s.replace('.scene video,.scene img{position:absolute;width:100%;height:100%;object-fit:cover;object-position:50% 60%}', '.scene video,.scene img{position:absolute;width:100%;max-width:calc(100svh * .75);height:100%;object-fit:cover;object-position:center}')
s=s.replace('.copy{max-width:480px}', '.copy{max-width:480px}.chapter-number{font-size:11px;letter-spacing:.12em;color:var(--color-copper);margin:0 0 16px}.final-action{display:inline-flex;min-height:48px;background:var(--color-copper);color:var(--color-white);padding:0 20px;border-radius:var(--radius-small);pointer-events:auto;margin-top:12px}.final-action[hidden]{display:none}')
s=s.replace('<div class="copy"><h1', '<div class="copy"><div class="chapter-number" id="chapter-number">01 / 03</div><h1')
s=s.replace('从今天的一条记录开始。</p>', '潘多拉 · 团队与个人的工作记录</p>')
s=s.replace('展开工作之旅 ↓</p></div>', '展开工作之旅 ↓</p><a class="final-action" href="#entry" id="final-action" hidden>开始记录 ↗</a></div>')
start=s.index('const stories=');end=s.index(';let storyIndex',start)
s=s[:start]+"const stories=[['让每一步工作，','都有迹可循。','潘多拉 · 团队与个人的工作记录'],['从一页记录，','理清下一步。','完成的事、遇到的问题、下一步。\\n把日常进展写成可以回看的工作日志。'],['让分散的工作，','回到一条时间线。','接收任务，更新进展。\\n在同一张月历中，回看日志与截止日期。']]"+s[end:]
s=s.replace('const stage=Math.min(2,Math.floor(p*3))','const stage=p<.42?0:p<.78?1:2')
s=s.replace('storyIndex=stage;const title=', "storyIndex=stage;document.querySelector('#chapter-number').textContent='0'+(stage+1)+' / 03';document.querySelector('#final-action').hidden=stage!==2;const title=")
s=s.replace('.narrative p{color:', '.narrative p{white-space:pre-line;color:')
s=s.replace("document.querySelectorAll('header a')", "document.querySelectorAll('header a,.final-action')")
s=s.replace('视频为生成图片的连续运镜。','视频为生成图片的连续运镜与叠化，不包含 AI 物体变形。')
p.write_text(s,encoding='utf-8')
p=root/'docs/landing-preview/tokens.css';s=p.read_text(encoding='utf-8').replace('--color-paper: #f0f3f1','--color-paper: #f3efe5');p.write_text(s,encoding='utf-8')
