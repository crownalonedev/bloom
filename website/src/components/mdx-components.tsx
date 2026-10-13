import type { MDXComponents } from "mdx/types";
import type { ComponentProps } from "react";
import { Link } from "wouter";
import { Callout } from "./Callout";
import { CodeBlock } from "./CodeBlock";
import { CapabilityTable, CompatibilityTable } from "./CompatibilityTable";
import { Tab, Tabs } from "./Tabs";

function MdxLink({ href = "", ...rest }: ComponentProps<"a">) {
  if (href.startsWith("/") && !href.startsWith("//") && !/\.(txt|json|xml)$/.test(href)) {
    return <Link href={href} {...rest} />;
  }
  return <a href={href} {...rest} />;
}

const components: MDXComponents = {
  pre: CodeBlock as MDXComponents["pre"],
  a: MdxLink,
  Callout,
  Tabs,
  Tab,
  CompatibilityTable,
  CapabilityTable,
};

export function useMDXComponents(): MDXComponents {
  return components;
}
