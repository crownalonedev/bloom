// Bloom's own code themes. Colours come from the brand palette and were checked for >= 4.5:1 contrast against
// their background (comments included).
import type { ThemeRegistrationRaw } from "shiki";

type Palette = {
  name: string;
  type: "dark" | "light";
  bg: string;
  fg: string;
  comment: string;
  keyword: string;
  type_: string;
  string: string;
  number: string;
  fn: string;
  annotation: string;
  punctuation: string;
};

function theme(p: Palette): ThemeRegistrationRaw {
  return {
    name: p.name,
    type: p.type,
    colors: { "editor.background": p.bg, "editor.foreground": p.fg },
    settings: [
      { settings: { foreground: p.fg, background: p.bg } },
      { scope: ["comment", "punctuation.definition.comment"], settings: { foreground: p.comment, fontStyle: "italic" } },
      {
        scope: ["keyword", "storage", "storage.type", "storage.modifier", "keyword.control", "keyword.operator.new"],
        settings: { foreground: p.keyword },
      },
      {
        scope: ["entity.name.type", "entity.name.class", "support.class", "support.type", "storage.type.java", "entity.other.inherited-class"],
        settings: { foreground: p.type_ },
      },
      { scope: ["string", "string.quoted", "markup.inline.raw"], settings: { foreground: p.string } },
      { scope: ["constant.numeric", "constant.language", "constant.other", "variable.other.enummember"], settings: { foreground: p.number } },
      { scope: ["entity.name.function", "support.function", "meta.method-call entity.name.function"], settings: { foreground: p.fn } },
      { scope: ["storage.type.annotation", "punctuation.definition.annotation", "meta.declaration.annotation"], settings: { foreground: p.annotation } },
      { scope: ["punctuation", "meta.brace", "keyword.operator"], settings: { foreground: p.punctuation } },
      { scope: ["entity.name.tag", "support.type.property-name", "entity.name.tag.yaml"], settings: { foreground: p.keyword } },
      { scope: ["variable.parameter"], settings: { foreground: p.fg } },
    ],
  };
}

export const bloomDark = theme({
  name: "bloom-dark",
  type: "dark",
  bg: "#1b1421",
  fg: "#efe4f1",
  comment: "#a293ac",
  keyword: "#f2a7cc",
  type_: "#c6b2ea",
  string: "#a9dcc4",
  number: "#f5c89a",
  fn: "#e3d6fb",
  annotation: "#e58dba",
  punctuation: "#b9a9c3",
});

export const bloomLight = theme({
  name: "bloom-light",
  type: "light",
  bg: "#fbf0f6",
  fg: "#2a1b30",
  comment: "#6f5f78",
  keyword: "#a8336f",
  type_: "#5f4597",
  string: "#24704f",
  number: "#94541b",
  fn: "#4f3484",
  annotation: "#a8336f",
  punctuation: "#5e4f66",
});
