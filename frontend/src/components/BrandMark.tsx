/** 品牌标识：盾牌 + 对勾，纯 SVG（不使用 emoji 作为图标） */
export function BrandMark({ size = 20 }: { size?: number }) {
  return (
    <svg
      width={size}
      height={size}
      viewBox="0 0 24 24"
      fill="none"
      aria-hidden="true"
      focusable="false"
    >
      <path
        d="M12 2.6 20 5.6v6.1c0 4.8-3.2 8.2-8 9.7-4.8-1.5-8-4.9-8-9.7V5.6l8-3Z"
        stroke="currentColor"
        strokeWidth="1.6"
        strokeLinejoin="round"
      />
      <path
        d="m8.6 11.9 2.3 2.2 4.5-4.4"
        stroke="currentColor"
        strokeWidth="1.6"
        strokeLinecap="round"
        strokeLinejoin="round"
      />
    </svg>
  )
}
