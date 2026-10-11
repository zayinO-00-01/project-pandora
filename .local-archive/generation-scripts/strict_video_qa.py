from pathlib import Path
p=Path('docs/landing-preview/journey.html');s=p.read_text(encoding='utf-8')
start=s.index("document.querySelector('#scan').onclick=");end=s.index('</script>',start)
s=s[:start]+'''document.querySelector('#scan').onclick=async e=>{
 const button=e.currentTarget,report=document.querySelector('#report');manual=true;button.disabled=true;
 const canvas=document.createElement('canvas');canvas.width=720;canvas.height=960;const ctx=canvas.getContext('2d',{willReadFrequently:true}), signatures=new Set(),results=[];
 function pixels(source){ctx.drawImage(source,0,0,720,960);return ctx.getImageData(0,0,720,960).data}
 function signature(data){let hash=2166136261,sum=0;for(let n=0;n<data.length;n+=16){hash=Math.imul(hash^data[n],16777619);sum+=data[n]+data[n+1]+data[n+2]}return {hash:hash>>>0,mean:sum/(data.length/16*3)}}
 async function image(url){const i=new Image();i.src=url;await i.decode();return i}
 try{
 for(let frame=0;frame<=270;frame++){
 target=frame/30;
 if(Math.abs(video.currentTime-target)>.005)await new Promise((resolve,reject)=>{const timer=setTimeout(()=>reject(Error('seek timeout at '+frame)),3000);video.addEventListener('seeked',()=>{clearTimeout(timer);resolve()},{once:true});seek()});
 if(Math.abs(video.currentTime-target)>.02)throw Error('实际视频时间与目标帧不符：'+frame);
 const sample=signature(pixels(video));if(sample.mean<5)throw Error('黑帧：'+frame);signatures.add(sample.hash);results.push({frame,time:video.currentTime,...sample});frameInput.value=frame;report.textContent=`已检查 ${frame+1}/271 帧`;
 }
 if(signatures.size<50)throw Error('画面未形成有效过渡');
 for(let n=1;n<=2;n++){const a=new Uint8ClampedArray(pixels(await image(`transition-${n}-last-v3.png`)));const b=pixels(await image(`transition-${n+1}-first-v3.png`));if(a.some((v,k)=>v!==b[k]))throw Error('接缝像素不一致：'+n)}
 window.frameReport=results;report.textContent=`271 帧通过；${signatures.size} 个不同画面；两处接缝末首帧像素一致。`;
 }catch(error){report.textContent='验证失败：'+error.message}finally{button.disabled=false}
};
'''+s[end:]
p.write_text(s,encoding='utf-8')
