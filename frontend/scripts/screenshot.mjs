/**
 * 界面自检脚本：用本机 Edge 无头模式打开各路由并截图。
 * 仅用于开发期视觉回归，不参与生产构建。
 *
 * 用法：
 *   1. 另开终端运行 npm run dev（默认 http://127.0.0.1:5173）
 *   2. node scripts/screenshot.mjs
 * 截图输出到仓库根目录 screenshots/
 */
import { mkdir } from 'node:fs/promises'
import path from 'node:path'
import puppeteer from 'puppeteer-core'

const BASE = process.env.PREVIEW_URL ?? 'http://localhost:5173'
const EDGE = process.env.EDGE_PATH ?? 'C:\\Program Files (x86)\\Microsoft\\Edge\\Application\\msedge.exe'
const OUT_DIR = path.resolve(import.meta.dirname, '..', '..', 'screenshots')
const AUTH_KEY = 'chexian.auth.demo'

const demoUser = {
  userId: 'demo-user',
  userName: '演示用户',
  role: 'user',
}
const demoAgent = {
  userId: 'demo-agent',
  userName: '演示业务员',
  role: 'agent',
}

const SHOTS = [
  { name: '01-login', route: '/login', auth: null },
  { name: '02-entry', route: '/', auth: demoUser },
  { name: '03-chat', route: '/chat', auth: demoUser },
  { name: '03b-chat-plan', route: '/chat', auth: demoUser, scroll: '.chat-thread' },
  { name: '03c-placeholder-modal', route: '/chat', auth: demoUser, click: '.chat-head-actions .ant-btn-primary' },
  { name: '04-orders', route: '/orders', auth: demoUser },
  { name: '05-policy', route: '/policy', auth: demoUser },
  { name: '06-handoff', route: '/handoff', auth: demoUser },
  { name: '07-workbench', route: '/workbench', auth: demoAgent },
]

const browser = await puppeteer.launch({
  executablePath: EDGE,
  headless: true,
  args: ['--no-sandbox', '--disable-gpu', '--hide-scrollbars', '--font-render-hinting=none'],
})

const page = await browser.newPage()
const problems = []
page.on('pageerror', (error) => problems.push(`[pageerror] ${error.message}`))
page.on('console', (message) => {
  if (message.type() === 'error') problems.push(`[console] ${message.text()}`)
})

await mkdir(OUT_DIR, { recursive: true })
await page.setViewport({ width: 1440, height: 900, deviceScaleFactor: 1 })

for (const shot of SHOTS) {
  // 先进入同源页面，才能写入 localStorage
  await page.goto(`${BASE}/login`, { waitUntil: 'domcontentloaded' })
  await page.evaluate(
    (key, value) => {
      if (value) localStorage.setItem(key, JSON.stringify(value))
      else localStorage.removeItem(key)
    },
    AUTH_KEY,
    shot.auth,
  )

  await page.goto(`${BASE}${shot.route}`, { waitUntil: 'networkidle0' })
  if (shot.auth) {
    await page.waitForSelector('.app-shell', { timeout: 15000 })
  }
  await new Promise((resolve) => setTimeout(resolve, 450))

  if (shot.scroll) {
    await page.evaluate((selector) => {
      const element = document.querySelector(selector)
      if (element) element.scrollTop = element.scrollHeight
    }, shot.scroll)
    await new Promise((resolve) => setTimeout(resolve, 250))
  }

  if (shot.click) {
    await page.click(shot.click)
    await new Promise((resolve) => setTimeout(resolve, 500))
  }

  await page.screenshot({ path: path.join(OUT_DIR, `${shot.name}.png`) })
  console.log(`✓ ${shot.name} ← ${shot.route}`)
}

// 移动端：确认侧边栏折叠与抽屉入口
await page.setViewport({ width: 390, height: 844, deviceScaleFactor: 2 })
await page.evaluate((key, value) => localStorage.setItem(key, JSON.stringify(value)), AUTH_KEY, demoUser)
await page.goto(`${BASE}/chat`, { waitUntil: 'networkidle0' })
await page.waitForSelector('.app-shell', { timeout: 15000 })
await new Promise((resolve) => setTimeout(resolve, 400))
await page.screenshot({ path: path.join(OUT_DIR, '08-chat-mobile.png') })
console.log('✓ 08-chat-mobile ← /chat (390x844)')

// 移动端导航抽屉：确认侧边栏可展开
await page.click('.app-topbar .mobile-only')
await new Promise((resolve) => setTimeout(resolve, 500))
await page.screenshot({ path: path.join(OUT_DIR, '09-nav-drawer.png') })
console.log('✓ 09-nav-drawer ← 侧边栏抽屉')

await browser.close()

if (problems.length > 0) {
  console.log('\n运行期告警：')
  for (const problem of problems) console.log(`- ${problem}`)
  process.exitCode = 0
} else {
  console.log('\n无控制台错误')
}
