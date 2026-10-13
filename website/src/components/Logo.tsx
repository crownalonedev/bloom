import { useId } from "react";

/** Bloom mark: five notched sakura petals arranged around an open centre. Original artwork. */
export function BloomMark({ size = 28, className }: { size?: number; className?: string }) {
  const id = useId().replace(/:/g, "");
  const petal = "M0 -2 C 5 -6, 7 -14, 3.2 -19.5 L 0 -16.5 L -3.2 -19.5 C -7 -14, -5 -6, 0 -2 Z";
  return (
    <svg width={size} height={size} viewBox="-22 -22 44 44" aria-hidden="true" className={className}>
      <defs>
        <linearGradient id={`p${id}`} x1="0" y1="-20" x2="0" y2="0" gradientUnits="userSpaceOnUse">
          <stop offset="0" stopColor="#F9D6E8" />
          <stop offset="1" stopColor="#E58DBA" />
        </linearGradient>
      </defs>
      {[0, 72, 144, 216, 288].map((angle) => (
        <path key={angle} d={petal} fill={`url(#p${id})`} transform={`rotate(${angle})`} />
      ))}
      <circle r="3.1" fill="#B9A0DD" />
    </svg>
  );
}

export function Logo() {
  return (
    <span className="inline-flex items-center gap-2.5">
      <BloomMark />
      <span className="font-display text-[1.35rem] font-bold leading-none tracking-tight">Bloom</span>
    </span>
  );
}
