const {chromium}=require('playwright');const {spawn}=require('child_process');(async()=>{
const [out,fps,dur]=[process.argv[2],+process.argv[3]||60,+process.argv[4]||5.5];
const b=await chromium.launch();const p=await b.newPage({viewport:{width:1920,height:1080},deviceScaleFactor:2});
await p.goto('file://'+process.cwd()+'/anim3.html');await p.waitForTimeout(400);
const ff=spawn('ffmpeg',['-loglevel','error','-y','-f','image2pipe','-framerate',String(fps),'-c:v','png','-i','-','-i','sting7c.wav',
 '-vf','noise=alls=1.5:allf=t,format=yuv420p','-c:v','libx264','-preset','slow','-profile:v','high','-level:v','5.2','-crf','14','-x264-params','aq-mode=3',
 '-c:a','aac','-b:a','320k','-ar','48000','-shortest','-movflags','+faststart',out],{stdio:['pipe','inherit','inherit']});
const n=Math.round(fps*dur);
for(let i=0;i<=n;i++){await p.evaluate(x=>render(x),i/fps);const buf=await p.screenshot({type:'png'});
 if(!ff.stdin.write(buf)) await new Promise(r=>ff.stdin.once('drain',r));}
ff.stdin.end();await new Promise(r=>ff.on('close',r));await b.close();})();
