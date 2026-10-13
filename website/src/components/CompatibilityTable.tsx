import { compatibility } from "../lib/site";

const RUNTIME_LABEL: Record<string, string> = {
  accepted: "Supported",
  unsupported: "Not supported",
  "opt-in": "Opt-in, untested",
};

/** Renders docs/compatibility.json. Verification shows only what has actually been done. */
export function CompatibilityTable() {
  return (
    <div className="not-prose my-6 overflow-x-auto">
      <table className="w-full min-w-[44rem] border-collapse text-left text-[0.92rem]">
        <caption className="sr-only">Supported Minecraft versions and how each was verified</caption>
        <thead>
          <tr className="border-b border-line text-muted">
            <th scope="col" className="py-2 pr-4 font-bold">Minecraft</th>
            <th scope="col" className="py-2 pr-4 font-bold">Server</th>
            <th scope="col" className="py-2 pr-4 font-bold">Java</th>
            <th scope="col" className="py-2 pr-4 font-bold">Bloom</th>
            <th scope="col" className="py-2 pr-4 font-bold">Verified by</th>
          </tr>
        </thead>
        <tbody>
          {compatibility.rows.map((row) => (
            <tr key={row.minecraft + row.servers} className="border-b border-line align-top">
              <th scope="row" className="py-3 pr-4 font-mono text-[0.86rem] font-normal">{row.minecraft}</th>
              <td className="py-3 pr-4">{row.servers}</td>
              <td className="py-3 pr-4 font-mono text-[0.86rem]">{row.java}</td>
              <td className="py-3 pr-4">
                <span className={row.runtime === "accepted" ? "text-accent" : "text-muted"}>
                  {RUNTIME_LABEL[row.runtime] ?? row.runtime}
                </span>
                {row.adapter !== "none" && <span className="block text-[0.82rem] text-muted">{row.adapter} adapter</span>}
              </td>
              <td className="py-3 pr-4 text-[0.86rem] text-muted">
                {row.verification.length ? row.verification.join(", ") : "—"}
                <span className="mt-1 block">{row.notes}</span>
              </td>
            </tr>
          ))}
        </tbody>
      </table>
      <p className="mt-3 text-[0.84rem] text-muted">
        Updated {compatibility.updated}. “Supported” means Bloom starts and selects an adapter; the last column lists
        how that version has actually been verified so far.
      </p>
    </div>
  );
}

export function CapabilityTable() {
  const { columns, rows } = compatibility.capabilities;
  return (
    <div className="not-prose my-6 overflow-x-auto">
      <table className="w-full min-w-[36rem] border-collapse text-left text-[0.9rem]">
        <caption className="sr-only">Platform capabilities by server family</caption>
        <thead>
          <tr className="border-b border-line text-muted">
            <th scope="col" className="py-2 pr-4 font-bold">Capability</th>
            {columns.map((c) => (
              <th key={c} scope="col" className="py-2 pr-4 font-bold">{c}</th>
            ))}
          </tr>
        </thead>
        <tbody>
          {rows.map((row) => (
            <tr key={row.capability} className="border-b border-line">
              <th scope="row" className="py-2 pr-4 font-mono text-[0.82rem] font-normal">{row.capability}</th>
              {row.values.map((v, i) => (
                <td key={i} className={`py-2 pr-4 ${v.startsWith("no") ? "text-muted" : ""}`}>{v}</td>
              ))}
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
}
