// The homepage signature: a branch drawn along the Minecraft version line. Blossoms open only where Bloom has an
// adapter; the 1.9 - 1.21.0 stretch stays bare, because Bloom does not support it.

const blossoms = [
  { x: 92, y: 118, label: "1.7.10", note: "Java 8", delay: 0.9 },
  { x: 210, y: 92, label: "1.8.x", note: "Java 8", delay: 1.1 },
  { x: 742, y: 98, label: "1.21.x", note: "Paper, Java 21", delay: 1.7 },
  { x: 872, y: 72, label: "26.x", note: "Paper, Java 25", delay: 1.9 },
];

function Flower({ x, y, delay }: { x: number; y: number; delay: number }) {
  const petal = "M0 -1.5 C 4 -5, 5.5 -11, 2.6 -15.5 L 0 -13 L -2.6 -15.5 C -5.5 -11, -4 -5, 0 -1.5 Z";
  return (
    <g transform={`translate(${x} ${y})`}>
      <g className="blossom-node" style={{ animationDelay: `${delay}s` }}>
        {[0, 72, 144, 216, 288].map((a) => (
          <path key={a} d={petal} transform={`rotate(${a})`} fill="url(#petal-fill)" />
        ))}
        <circle r="2.6" fill="#B9A0DD" />
      </g>
    </g>
  );
}

export function VersionBranch() {
  return (
    <figure className="relative">
      <svg
        viewBox="0 10 960 180"
        className="h-auto w-full overflow-visible"
        role="img"
        aria-labelledby="branch-title branch-desc"
      >
        <title id="branch-title">Minecraft versions supported by Bloom</title>
        <desc id="branch-desc">
          Supported: 1.7.10 and 1.8.x on Bukkit or Spigot with Java 8; Paper 1.21.x with Java 21; Paper 26.x with
          Java 25. Versions 1.9 to 1.21.0 have no adapter yet.
        </desc>
        <defs>
          <linearGradient id="petal-fill" x1="0" y1="-16" x2="0" y2="0" gradientUnits="userSpaceOnUse">
            <stop offset="0" stopColor="#F9D6E8" />
            <stop offset="1" stopColor="#E58DBA" />
          </linearGradient>
          <linearGradient id="bark" x1="0" x2="1">
            <stop offset="0" style={{ stopColor: "var(--muted)", stopOpacity: 0.85 }} />
            <stop offset="1" style={{ stopColor: "var(--muted)", stopOpacity: 0.55 }} />
          </linearGradient>
        </defs>
        <path
          className="branch-path"
          pathLength={1}
          d="M10 150 C 120 130, 160 105, 240 96 S 420 120, 520 122 S 700 105, 780 94 S 900 70, 950 58"
          fill="none"
          stroke="url(#bark)"
          strokeWidth="3"
          strokeLinecap="round"
        />
        {/* twigs */}
        <path className="branch-path" pathLength={1} d="M92 136 Q 90 126, 92 118" fill="none" stroke="url(#bark)" strokeWidth="1.6" strokeLinecap="round" />
        <path className="branch-path" pathLength={1} d="M210 99 Q 212 96, 210 92" fill="none" stroke="url(#bark)" strokeWidth="1.6" strokeLinecap="round" />
        <path className="branch-path" pathLength={1} d="M742 103 Q 744 100, 742 98" fill="none" stroke="url(#bark)" strokeWidth="1.6" strokeLinecap="round" />
        <path className="branch-path" pathLength={1} d="M440 117 Q 452 98, 470 92" fill="none" stroke="url(#bark)" strokeWidth="1.4" strokeLinecap="round" />
        <path className="branch-path" pathLength={1} d="M600 118 Q 590 136, 574 142" fill="none" stroke="url(#bark)" strokeWidth="1.4" strokeLinecap="round" />
        {blossoms.map((b) => (
          <Flower key={b.label} x={b.x} y={b.y} delay={b.delay} />
        ))}
        <g className="fill-ink font-sans">
          {blossoms.map((b) => (
            <g key={b.label} transform={`translate(${b.x} ${b.y - 46})`} textAnchor="middle">
              <text className="font-display" fontWeight={700} fontSize="17">{b.label}</text>
              <text y="19" fontSize="12.5" className="fill-muted">{b.note}</text>
            </g>
          ))}
          <text x="520" y="160" textAnchor="middle" fontSize="12.5" className="fill-muted">
            1.9 to 1.21.0: no adapter yet
          </text>
        </g>
      </svg>
    </figure>
  );
}
