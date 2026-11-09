import type { ThemeConfig } from 'antd'

/**
 * Ant Design 主题映射。
 * 只使用海军蓝 / 冷蓝 / 中性灰（严禁紫色），与 tokens.css 保持一致。
 * 色值变更请同步 src/styles/tokens.css。
 */
export const antdTheme: ThemeConfig = {
  token: {
    colorPrimary: '#0369a1',
    colorInfo: '#0369a1',
    colorSuccess: '#15803d',
    colorWarning: '#b45309',
    colorError: '#dc2626',
    colorTextBase: '#020617',
    colorText: '#0f172a',
    colorTextSecondary: '#475569',
    colorTextTertiary: '#64748b',
    colorTextQuaternary: '#94a3b8',
    colorBorder: '#d7dfe8',
    colorBorderSecondary: '#e8ecf1',
    colorBgBase: '#ffffff',
    colorBgContainer: '#ffffff',
    colorBgLayout: 'transparent',
    colorFillAlter: 'rgba(241, 245, 249, 0.72)',
    borderRadius: 10,
    borderRadiusLG: 14,
    borderRadiusSM: 8,
    fontFamily:
      "'Inter', -apple-system, BlinkMacSystemFont, 'Segoe UI', 'PingFang SC', 'Hiragino Sans GB', 'Microsoft YaHei', 'Noto Sans SC', sans-serif",
    fontSize: 13,
    controlHeight: 34,
    lineWidth: 1,
    motionDurationMid: '0.22s',
    motionEaseOut: 'cubic-bezier(0.22, 1, 0.36, 1)',
  },
  components: {
    Button: {
      primaryShadow: 'none',
      defaultShadow: 'none',
      dangerShadow: 'none',
    },
    Table: {
      headerBg: 'rgba(248, 250, 252, 0.9)',
      headerSplitColor: 'transparent',
      rowHoverBg: 'rgba(240, 249, 255, 0.65)',
      borderColor: '#e8ecf1',
      headerBorderRadius: 12,
    },
    Segmented: {
      itemSelectedBg: '#ffffff',
      trackBg: 'rgba(241, 245, 249, 0.9)',
    },
    Input: {
      activeShadow: '0 0 0 3px rgba(3, 105, 161, 0.08)',
    },
    Layout: {
      bodyBg: 'transparent',
      headerBg: 'transparent',
      siderBg: 'transparent',
    },
    Modal: {
      contentBg: '#ffffff',
      headerBg: '#ffffff',
    },
  },
}
