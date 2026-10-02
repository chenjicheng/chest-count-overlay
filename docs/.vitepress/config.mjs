import { defineConfig } from 'vitepress'

const repository = 'https://github.com/chenjicheng/chest-count-overlay'
const project = 'https://modrinth.com/mod/chest-count-overlay'

export default defineConfig({
  base: '/chest-count-overlay/',
  title: 'Chest Count Overlay',
  description: '客户端容器物品数量统计 / Client-side container item totals',
  cleanUrls: false,
  themeConfig: {
    search: { provider: 'local' },
    socialLinks: [{ icon: 'github', link: repository }],
    editLink: { pattern: `${repository}/edit/main/docs/:path` }
  },
  locales: {
    root: {
      label: '简体中文', lang: 'zh-CN',
      themeConfig: {
        nav: [{ text: '使用说明', link: '/guide' }, { text: '开发与发布', link: '/development' }, { text: '下载', link: project }],
        sidebar: [{ text: '箱子数量统计', items: [
          { text: '介绍', link: '/' }, { text: '安装与使用', link: '/guide' },
          { text: '开发与发布', link: '/development' }, { text: '1.0.1 发行说明', link: '/releases/1.0.1' }
        ] }],
        outline: { label: '本页内容', level: [2, 3] },
        docFooter: { prev: '上一页', next: '下一页' }
      }
    },
    en: {
      label: 'English', lang: 'en-US',
      themeConfig: {
        nav: [{ text: 'User guide', link: '/en/guide' }, { text: 'Development', link: '/en/development' }, { text: 'Download', link: project }],
        sidebar: [{ text: 'Chest Count Overlay', items: [
          { text: 'Introduction', link: '/en/' }, { text: 'Installation and usage', link: '/en/guide' },
          { text: 'Development and releases', link: '/en/development' }, { text: '1.0.1 release notes', link: '/releases/1.0.1' }
        ] }],
        outline: { level: [2, 3] }
      }
    }
  }
})
