const {chromium}=require('playwright');(async()=>{const b=await chromium.launch();const p=await b.newPage({viewport:{width:1920,height:1080}});
await p.goto('file://'+process.cwd()+'/anim2.html');await p.waitForTimeout(300);
const fps=30,dur=5.0,fs=require('fs');fs.rmSync('frames',{recursive:true,force:true});fs.mkdirSync('frames');
const only=process.argv[2];
if(only){for(const t of only.split(',')){await p.evaluate(x=>render(x),+t);await p.screenshot({path:`frames/t${t}.png`});}}
else for(let i=0;i<=fps*dur;i++){await p.evaluate(x=>render(x),i/fps);await p.screenshot({path:`frames/f${String(i).padStart(4,'0')}.png`});}
await b.close();})();
