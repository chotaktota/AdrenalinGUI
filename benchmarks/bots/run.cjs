const { createRequire } = require('node:module')
const path = require('node:path')
const net = require('node:net')
const fs = require('node:fs')
const requireBots = createRequire(path.join(process.env.BENCH_BOTS_ROOT || __dirname, 'package.json'))
const mineflayer = requireBots('mineflayer')
const nbt = requireBots('prismarine-nbt')
const plain = component => typeof component==='string' ? component : (component.text || '')+(component.extra || []).map(plain).join('')
const colored = (component, color, inherited=null) => {
  if(typeof component==='string') return !component || inherited===color
  const current=component.color || inherited
  return (!component.text || current===color) && (component.extra || []).every(child=>colored(child,color,current))
}
const sleep = ms => new Promise(resolve => setTimeout(resolve, ms))
class Rcon {
  constructor() { this.pending = new Map(); this.next = 1; this.buffer = Buffer.alloc(0) }
  async connect() {
    this.socket = net.createConnection({host:'127.0.0.1',port:25580})
    await new Promise((resolve,reject) => { this.socket.once('connect',resolve); this.socket.once('error',reject) })
    this.socket.on('data', data => {
      this.buffer = Buffer.concat([this.buffer,data])
      while (this.buffer.length >= 4 && this.buffer.length >= this.buffer.readInt32LE(0)+4) {
        const length = this.buffer.readInt32LE(0), packet = this.buffer.subarray(4,length+4)
        this.buffer = this.buffer.subarray(length+4)
        const id = packet.readInt32LE(0), type=packet.readInt32LE(4)
        const waiter=this.pending.get(id)
        if (waiter && (type===0 || type===2)) { this.pending.delete(id); waiter(packet.subarray(8,packet.length-2).toString()) }
      }
    })
    await this.send(process.env.BENCH_RCON_PASSWORD || 'benchmark-local-only',3)
  }
  send(body,type=2) {
    const id=this.next++, payload=Buffer.from(body), packet=Buffer.alloc(payload.length+14)
    packet.writeInt32LE(payload.length+10,0); packet.writeInt32LE(id,4); packet.writeInt32LE(type,8); payload.copy(packet,12)
    return new Promise((resolve,reject) => {
      const timeout=setTimeout(()=>{this.pending.delete(id);reject(new Error('RCON timeout: '+body))},15000)
      this.pending.set(id,text=>{clearTimeout(timeout);resolve(text)})
      this.socket.write(packet)
    })
  }
}
async function main() {
  const started = new Date().toISOString(), checks={windows:0,items:0}
  const rcon = new Rcon(); await rcon.connect()
  const bots=[]
  for (let i=0;i<8;i++) {
    const bot=mineflayer.createBot({host:'127.0.0.1',port:25579,username:'Bench'+i,auth:'offline',version:'1.21.11',hideErrors:false})
    bot.on('kicked',reason=>{ console.error('Kicked',bot.username,reason); process.exitCode=1 })
    bot.on('error',error=>console.error('Bot error',bot.username,error.message))
    await new Promise((resolve,reject)=>{const t=setTimeout(()=>reject(new Error('Spawn timeout')),45000);bot.once('spawn',()=>{clearTimeout(t);resolve()})})
    bots.push(bot); await sleep(150)
  }
  console.log('Connected',bots.length,'bots')
  await rcon.send('gamemode creative @a')
  await rcon.send('gamerule minecraft:advance_time false')
  await rcon.send('gamerule minecraft:spawn_mobs false')
  await sleep(3000)
  if (process.env.BENCH_VERIFY === '1') {
    const result = await rcon.send('benchverify Bench0')
    if (!result.includes('проверка пройдена')) throw new Error('Dynamic refresh verification failed: '+result)
    console.log('Dynamic refresh verification passed')
  }
  const cases=['agui/static','bmenu/static','abstract/static','agui/dynamic','bmenu/dynamic','abstract/dynamic']
  async function scenario(key,count) {
    const [engine,mode]=key.split('/')
    await Promise.all(bots.map(async(bot,index)=>{
      await sleep(index*20)
      for(let n=0;n<count;n++) {
        const opened=new Promise((resolve,reject)=>{
          const timer=setTimeout(()=>{bot.removeListener('windowOpen',listener);reject(new Error('Window timeout '+key+' '+bot.username))},8000)
          const listener=window=>{clearTimeout(timer);resolve(window)}
          bot.once('windowOpen',listener)
        })
        bot.chat('/benchopen '+engine+' '+mode)
        await opened
        await sleep(80)
        const itemDeadline = Date.now()+5000
        while (Array.from({length:54},(_,slot)=>bot.currentWindow?.slots[slot]).some(item=>!item || item.name!=='paper' || item.count!==1)) {
          if (Date.now()>itemDeadline) throw new Error('Item delivery timeout '+key+' '+bot.username)
          await sleep(20)
        }
        for(let slot=0;slot<54;slot++) {
          const item=bot.currentWindow.slots[slot]
          if(!item || item.name!=='paper' || item.count!==1) throw new Error('Invalid item '+key+' slot '+slot)
          const name=nbt.simplify(item.components.find(c=>c.type==='custom_name').data)
          const lore=item.components.find(c=>c.type==='lore').data.map(nbt.simplify)
          const expected=mode==='dynamic'?bot.username+' '+slot:'Item '+slot
          if(plain(name)!==expected || !colored(name,'green') || lore.map(plain).join('|')!=='Line one|Line two|Line three|100' || !lore.every((line,i)=>colored(line,i===3?'yellow':'gray'))) throw new Error('Invalid client text '+key+' slot '+slot+' '+JSON.stringify(name))
          checks.items++
        }
        checks.windows++
        if (process.env.BENCH_SMOKE === '1' && index === 0 && n === 0) {
          const output=process.env.BENCH_OUTPUT_DIR || path.join(__dirname,'dumps')
          fs.mkdirSync(output,{recursive:true})
          fs.writeFileSync(path.join(output,key.replace('/','-')+'.json'),JSON.stringify(bot.currentWindow.slots[13],null,2))
        }
        await Promise.race([bot.clickWindow(13,0,0),sleep(500)])
        await sleep(120)
      }
    }))
    console.log(new Date().toISOString(),key,count,'opens per bot completed')
    await sleep(500)
  }
  if (process.env.BENCH_SMOKE === '1') {
    await rcon.send('benchphase smoke')
    for (const key of cases) await scenario(key,3)
    await rcon.send('benchdump')
    await sleep(2000)
    for (const bot of bots) bot.quit()
    rcon.socket.end()
    console.log('Smoke check complete')
    return
  }
  await rcon.send('benchphase warmup')
  for(const key of cases) await scenario(key,30)
  for(let round=0;round<3;round++) {
    await rcon.send('benchphase round'+(round+1))
    const order=round===0?[2,4,0,5,1,3]:round===1?[3,1,5,0,4,2]:[1,5,3,2,0,4]
    for(const index of order) await scenario(cases[index],50)
  }
  await rcon.send('benchdump')
  await sleep(2000)
  for(const bot of bots) bot.quit()
  rcon.socket.end()
  const output=process.env.BENCH_OUTPUT_DIR || path.join(__dirname,'dumps')
  fs.mkdirSync(output,{recursive:true})
  fs.writeFileSync(path.join(output,'client-run.json'),JSON.stringify({started,finished:new Date().toISOString(),checks,bots:8,rounds:3,opensPerBotPerCasePerRound:50,warmupPerBotPerCase:30},null,2))
  console.log('Benchmark complete',JSON.stringify(checks))
}
main().catch(error=>{console.error(error);process.exit(1)})
