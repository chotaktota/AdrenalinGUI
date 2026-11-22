"use strict";

const article = document.getElementById("article");
const navigation = document.getElementById("navigation");
const contents = document.getElementById("table-of-contents");
const searchDialog = document.getElementById("search-dialog");
const searchInput = document.getElementById("search-input");
const searchResults = document.getElementById("search-results");
const searchStatus = document.getElementById("search-status");
const themeButton = document.getElementById("theme-toggle");
let documentation = null;
let currentPage = "";
let headingObserver = null;

function escapeHtml(value) {
  return String(value).replace(/[&<>"']/g, character => ({
    "&": "&amp;", "<": "&lt;", ">": "&gt;", '"': "&quot;", "'": "&#39;"
  })[character]);
}

function safeLink(value) {
  if (value.startsWith("#/") || value.startsWith("https://")) return value;
  return "#/introduction";
}

function inlineMarkdown(text) {
  const pattern = /(\x60[^\x60]+\x60|\*\*[^*]+\*\*|\[[^\]]+\]\([^)]+\))/g;
  let output = "";
  let offset = 0;
  for (const match of text.matchAll(pattern)) {
    output += escapeHtml(text.slice(offset, match.index));
    const token = match[0];
    if (token.startsWith("\x60")) {
      output += "<code>" + escapeHtml(token.slice(1, -1)) + "</code>";
    } else if (token.startsWith("**")) {
      output += "<strong>" + escapeHtml(token.slice(2, -2)) + "</strong>";
    } else {
      const link = token.match(/^\[([^\]]+)\]\(([^)]+)\)$/);
      const external = link[2].startsWith("https://");
      output += '<a href="' + escapeHtml(safeLink(link[2])) + '"' +
        (external ? ' target="_blank" rel="noopener noreferrer"' : "") + ">" +
        escapeHtml(link[1]) + "</a>";
    }
    offset = match.index + token.length;
  }
  return output + escapeHtml(text.slice(offset));
}

function highlight(code, language) {
  if (!["kotlin", "yaml", "bash", "powershell"].includes(language)) return escapeHtml(code);
  const tokens = /("(?:\\.|[^"\\])*"|'(?:\\.|[^'\\])*'|\/\/[^\n]*|\b(?:class|fun|val|var|if|else|return|override|private|public|protected|internal|import|package|true|false|null|in|is|as|when|object|this|for|while|try|catch|finally)\b|\b[A-Z][A-Za-z0-9_]*\b|\b\d+(?:\.\d+)?[fL]?\b)/g;
  let output = "";
  let offset = 0;
  for (const match of code.matchAll(tokens)) {
    const value = match[0];
    let role = "keyword";
    if (value.startsWith('"') || value.startsWith("'")) role = "string";
    else if (value.startsWith("//")) role = "comment";
    else if (/^\d/.test(value)) role = "number";
    else if (/^[A-Z]/.test(value)) role = "type";
    output += escapeHtml(code.slice(offset, match.index));
    output += '<span class="code-' + role + '">' + escapeHtml(value) + "</span>";
    offset = match.index + value.length;
  }
  return output + escapeHtml(code.slice(offset));
}

function codeBlock(code, language = "kotlin", file = "") {
  return '<div class="code-block"><div class="code-header"><span class="code-file">' +
    escapeHtml(file || language) +
    '</span><button class="copy-button" type="button" aria-label="Скопировать код">Копировать</button></div><pre><code>' +
    highlight(code, language) + "</code></pre></div>";
}

function headingId(text) {
  return text.toLowerCase().replace(/[^a-zа-яё0-9]+/g, "-").replace(/^-|-$/g, "");
}

function renderMarkdown(markdown, pageId) {
  const lines = markdown.replace(/\r/g, "").split("\n");
  const headings = [];
  const usedIds = new Set();
  let html = "";
  let index = 0;
  while (index < lines.length) {
    const line = lines[index];
    if (!line.trim() || /^# /.test(line)) {
      index++;
      continue;
    }
    const fence = line.match(/^(~~~|\x60\x60\x60)([a-z]*)\s*(.*)$/);
    if (fence) {
      const code = [];
      index++;
      while (index < lines.length && !lines[index].startsWith(fence[1])) {
        code.push(lines[index++]);
      }
      html += codeBlock(code.join("\n"), fence[2] || "text", fence[3]);
      index++;
      continue;
    }
    const heading = line.match(/^(#{2,3}) (.+)$/);
    if (heading) {
      const base = headingId(heading[2]);
      let id = base;
      let suffix = 2;
      while (usedIds.has(id)) id = base + "-" + suffix++;
      usedIds.add(id);
      const level = heading[1].length;
      headings.push({ id, text: heading[2], level });
      html += "<h" + level + ' id="' + escapeHtml(id) + '">' + inlineMarkdown(heading[2]) + "</h" + level + ">";
      index++;
      continue;
    }
    if (line.startsWith("|") && /^\|[ :|-]+\|$/.test(lines[index + 1] || "")) {
      const cells = value => value.trim().replace(/^\||\|$/g, "").split("|").map(cell => cell.trim());
      const headers = cells(line);
      html += '<div class="table-wrap"><table><thead><tr>' +
        headers.map(value => "<th scope=\"col\">" + inlineMarkdown(value) + "</th>").join("") +
        "</tr></thead><tbody>";
      index += 2;
      while (index < lines.length && lines[index].startsWith("|")) {
        html += "<tr>" + cells(lines[index]).map(value => "<td>" + inlineMarkdown(value) + "</td>").join("") + "</tr>";
        index++;
      }
      html += "</tbody></table></div>";
      continue;
    }
    const list = line.match(/^(- |\d+\. )(.+)$/);
    if (list) {
      const ordered = /^\d/.test(list[1]);
      const tag = ordered ? "ol" : "ul";
      html += "<" + tag + ">";
      const expression = ordered ? /^\d+\. (.+)$/ : /^- (.+)$/;
      while (index < lines.length) {
        const item = lines[index].match(expression);
        if (!item) break;
        html += "<li>" + inlineMarkdown(item[1]) + "</li>";
        index++;
      }
      html += "</" + tag + ">";
      continue;
    }
    const paragraph = [];
    while (index < lines.length && lines[index].trim() && !/^(#{1,3} |~~~|\x60\x60\x60|\||- |\d+\. )/.test(lines[index])) {
      paragraph.push(lines[index++]);
    }
    if (paragraph.length) html += "<p>" + inlineMarkdown(paragraph.join(" ")) + "</p>";
    else index++;
  }
  return { html, headings };
}

function renderNavigation() {
  const groups = new Map();
  for (const page of documentation.pages) {
    if (!groups.has(page.group)) groups.set(page.group, []);
    groups.get(page.group).push(page);
  }
  navigation.innerHTML = Array.from(groups, ([group, pages]) =>
    '<div class="nav-group"><p class="nav-label">' + escapeHtml(group) + "</p>" +
    pages.map(page => '<a class="nav-link" href="#/' + page.id + '" data-page="' + page.id + '">' + escapeHtml(page.title) + "</a>").join("") +
    "</div>"
  ).join("");
}

const demoItems = {
  P: { slot: 11, material: "PAPER", title: "Профиль", lines: ['name("<white>Профиль")', 'loreLine("<gray>%player_name%")', 'onClick { close() }'] },
  H: { slot: 13, material: "BOOK", title: "Помощь", lines: ['name("<red>Помощь")', 'loreLine("<gray>Категории сервера")', 'onClick { openPage("help") }'] },
  C: { slot: 15, material: "BARRIER", title: "Закрыть", lines: ['name("<gray>Закрыть")', 'onClick { close() }'] }
};

function renderDemo() {
  let slots = "";
  for (let slot = 0; slot < 27; slot++) {
    const entry = Object.entries(demoItems).find(([, item]) => item.slot === slot);
    if (entry) {
      const [symbol, item] = entry;
      slots += '<button class="inventory-cell" type="button" data-demo-symbol="' + symbol +
        '" aria-pressed="false" aria-label="Слот ' + slot + ', ' + item.title + '">' + symbol + "</button>";
    } else {
      slots += '<span class="inventory-cell" aria-hidden="true"></span>';
    }
  }
  return '<section class="demo" aria-label="Интерактивный пример шаблона">' +
    '<div class="demo-heading">От символа к предмету<span>Выберите P, H или C</span></div>' +
    '<div class="demo-body"><div class="inventory-preview"><div class="inventory-title">Меню сервера · 3 строки</div>' +
    '<div class="inventory-grid">' + slots + '</div><div class="pattern-preview" id="demo-pattern"></div></div>' +
    '<div class="demo-code" id="demo-code"></div></div>' +
    '<p class="demo-caption">Позиции и код одного шаблона. Окно открывается игроку внутри Minecraft.</p></section>';
}

function selectDemo(symbol) {
  const item = demoItems[symbol];
  document.querySelectorAll("[data-demo-symbol]").forEach(button => {
    button.setAttribute("aria-pressed", String(button.dataset.demoSymbol === symbol));
  });
  document.getElementById("demo-pattern").innerHTML = ".........\n" +
    "..P.H.C..".split("").map(character => character === symbol ? '<span class="pattern-selected">' + character + "</span>" : character).join("") +
    "\n.........";
  const code = "iconAt(pos('" + symbol + "')) {\n    type(Material." + item.material + ")\n    " + item.lines.join("\n    ") + "\n}";
  document.getElementById("demo-code").innerHTML = codeBlock(code, "kotlin", "Слот " + item.slot + " · " + item.title);
}

function closeNavigation() {
  delete document.body.dataset.nav;
  document.getElementById("nav-toggle").setAttribute("aria-expanded", "false");
}

document.querySelector(".skip-link").addEventListener("click", event => {
  event.preventDefault();
  article.focus({ preventScroll: true });
  article.scrollIntoView({ block: "start" });
});

navigation.addEventListener("click", event => {
  if (event.target.closest("a")) closeNavigation();
});

function renderRoute() {
  if (!documentation) return;
  const parts = location.hash.replace(/^#\/?/, "").split("/");
  const pageId = parts[0] || "introduction";
  let section = parts[1] || "";
  try { section = decodeURIComponent(section); } catch { section = ""; }
  const page = documentation.pages.find(candidate => candidate.id === pageId);
  if (!page) {
    currentPage = "";
    contents.replaceChildren();
    document.getElementById("page-navigation").replaceChildren();
    article.innerHTML = '<h1>Статья не найдена</h1><p>Выберите раздел в навигации или вернитесь к началу.</p><p><a href="#/introduction">Открыть введение</a></p>';
    article.setAttribute("aria-busy", "false");
    return;
  }
  const changedPage = currentPage !== page.id;
  if (changedPage) {
    currentPage = page.id;
    article.dataset.page = page.id;
    const withoutTitle = page.markdown.replace(/^# [^\n]*\n+/, "");
    const leadEnd = withoutTitle.indexOf("\n\n");
    const lead = leadEnd >= 0 ? withoutTitle.slice(0, leadEnd) : page.description;
    const body = leadEnd >= 0 ? withoutTitle.slice(leadEnd + 2) : withoutTitle;
    const rendered = renderMarkdown(body, page.id);
    let leadVisual = "";
    if (page.id === "introduction") {
      leadVisual = '<img class="wordmark" src="assets/wordmark.png" alt="AdrenalinGUI — красная металлическая надпись" width="2172" height="724">' +
        '<div class="intro-links"><a class="primary-link" href="#/quick-start">Создать первое меню</a><a class="secondary-link" href="#/examples">Примеры меню</a></div>' + renderDemo();
    }
    article.innerHTML = '<div class="article-heading"><p class="breadcrumb">' +
      escapeHtml(page.group) + ' / <span>AdrenalinGUI 2.0.1</span></p><h1 tabindex="-1">' +
      escapeHtml(page.id === "introduction" ? "Меню для Minecraft-плагинов на Kotlin" : page.title) +
      '</h1><p class="lead">' + inlineMarkdown(lead) + "</p></div>" + leadVisual + rendered.html;
    article.setAttribute("aria-busy", "false");
    document.title = page.title + " — AdrenalinGUI";
    document.querySelector('meta[name="description"]').content = page.description;
    document.querySelectorAll(".nav-link").forEach(link => {
      if (link.dataset.page === page.id) link.setAttribute("aria-current", "page");
      else link.removeAttribute("aria-current");
    });
    contents.innerHTML = rendered.headings.filter(heading => heading.level === 2).map(heading =>
      '<a class="toc-link" href="#/' + page.id + "/" + encodeURIComponent(heading.id) + '">' + escapeHtml(heading.text) + "</a>"
    ).join("");
    if (headingObserver) headingObserver.disconnect();
    headingObserver = new IntersectionObserver(entries => {
      for (const entry of entries) {
        if (!entry.isIntersecting) continue;
        contents.querySelectorAll("a").forEach(link => {
          if (decodeURIComponent(link.hash.split("/").at(-1)) === entry.target.id) link.setAttribute("aria-current", "location");
          else link.removeAttribute("aria-current");
        });
      }
    }, { rootMargin: "-80px 0px -65% 0px" });
    article.querySelectorAll("h2").forEach(heading => headingObserver.observe(heading));
    const index = documentation.pages.indexOf(page);
    const previous = documentation.pages[index - 1];
    const next = documentation.pages[index + 1];
    document.getElementById("page-navigation").innerHTML =
      (previous ? '<a class="page-link" href="#/' + previous.id + '"><span>Предыдущая статья</span><strong>' + escapeHtml(previous.title) + "</strong></a>" : "<span></span>") +
      (next ? '<a class="page-link" href="#/' + next.id + '"><span>Следующая статья</span><strong>' + escapeHtml(next.title) + "</strong></a>" : "<span></span>");
    document.getElementById("source-link").href = documentation.repository + "/blob/main/docs/pages/" + page.id + ".md";
    if (page.id === "introduction") selectDemo("H");
    closeNavigation();
  }
  if (section) {
    const target = document.getElementById(section);
    if (target) target.scrollIntoView({ block: "start" });
  } else if (changedPage) {
    window.scrollTo(0, 0);
    article.querySelector("h1").focus({ preventScroll: true });
  }
}

function updateSearch() {
  if (!documentation) return;
  const query = searchInput.value.trim().toLocaleLowerCase("ru");
  if (!query) {
    searchStatus.textContent = "Введите название возможности или метода";
    searchResults.replaceChildren();
    return;
  }
  const words = query.split(/\s+/);
  const matches = documentation.pages.map(page => {
    const text = (page.title + " " + page.description + " " + page.markdown).toLocaleLowerCase("ru");
    if (!words.every(word => text.includes(word))) return null;
    const score = (page.title.toLocaleLowerCase("ru").includes(query) ? 10 : 0) +
      (page.description.toLocaleLowerCase("ru").includes(query) ? 5 : 0);
    return { page, score };
  }).filter(Boolean).sort((a, b) => b.score - a.score).slice(0, 12);
  searchStatus.textContent = matches.length ? "Найдено статей: " + matches.length : "Статьи не найдены. Попробуйте другое слово.";
  searchResults.innerHTML = matches.map(({ page }) =>
    '<a class="search-result" href="#/' + page.id + '"><span class="search-group">' +
    escapeHtml(page.group) + "</span><strong>" + escapeHtml(page.title) +
    "</strong><small>" + escapeHtml(page.description) + "</small></a>"
  ).join("");
}

function openSearch() {
  if (!searchDialog.open) searchDialog.showModal();
  searchInput.focus();
}

function updateThemeLabel() {
  const dark = document.documentElement.dataset.theme !== "light";
  themeButton.textContent = dark ? "Светлая тема" : "Тёмная тема";
  themeButton.setAttribute("aria-label", dark ? "Включить светлую тему" : "Включить тёмную тему");
}

themeButton.addEventListener("click", () => {
  const theme = document.documentElement.dataset.theme === "light" ? "dark" : "light";
  document.documentElement.dataset.theme = theme;
  try { localStorage.setItem("agui-docs-theme", theme); } catch {}
  updateThemeLabel();
});
updateThemeLabel();
document.getElementById("search-open").addEventListener("click", openSearch);
document.getElementById("search-close").addEventListener("click", () => searchDialog.close());
searchInput.addEventListener("input", updateSearch);
searchResults.addEventListener("click", event => {
  if (event.target.closest("a")) searchDialog.close();
});
searchDialog.addEventListener("click", event => {
  if (event.target === searchDialog) {
    const bounds = searchDialog.getBoundingClientRect();
    if (event.clientX < bounds.left || event.clientX > bounds.right || event.clientY < bounds.top || event.clientY > bounds.bottom) searchDialog.close();
  }
});
searchDialog.addEventListener("keydown", event => {
  const results = Array.from(searchResults.querySelectorAll("a"));
  if (event.key === "ArrowDown" || event.key === "ArrowUp") {
    if (!results.length) return;
    event.preventDefault();
    const index = results.indexOf(document.activeElement);
    let next;
    if (index < 0) next = event.key === "ArrowDown" ? 0 : results.length - 1;
    else next = (index + (event.key === "ArrowDown" ? 1 : -1) + results.length) % results.length;
    results[next].focus();
  } else if (event.key === "Enter" && document.activeElement === searchInput && results.length) {
    event.preventDefault();
    results[0].click();
  }
});
document.addEventListener("keydown", event => {
  if ((event.ctrlKey || event.metaKey) && event.key.toLowerCase() === "k") {
    event.preventDefault();
    openSearch();
  }
});
document.getElementById("nav-toggle").addEventListener("click", () => {
  const open = document.body.dataset.nav !== "open";
  if (open) document.body.dataset.nav = "open";
  else delete document.body.dataset.nav;
  document.getElementById("nav-toggle").setAttribute("aria-expanded", String(open));
});
document.getElementById("nav-backdrop").addEventListener("click", closeNavigation);
document.addEventListener("keydown", event => {
  if (event.key === "Escape") closeNavigation();
});
article.addEventListener("click", async event => {
  const demoButton = event.target.closest("[data-demo-symbol]");
  if (demoButton) selectDemo(demoButton.dataset.demoSymbol);
  const copy = event.target.closest(".copy-button");
  if (!copy) return;
  const value = copy.closest(".code-block").querySelector("code").textContent;
  try {
    await navigator.clipboard.writeText(value);
    copy.textContent = "Скопировано";
    copy.setAttribute("aria-label", "Код скопирован");
    setTimeout(() => {
      copy.textContent = "Копировать";
      copy.setAttribute("aria-label", "Скопировать код");
    }, 1800);
  } catch {
    copy.textContent = "Не удалось скопировать";
    copy.setAttribute("aria-label", "Не удалось скопировать код");
  }
});
window.addEventListener("hashchange", renderRoute);

fetch("content.json").then(response => {
  if (!response.ok) throw new Error("HTTP " + response.status);
  return response.json();
}).then(data => {
  documentation = data;
  renderNavigation();
  renderRoute();
}).catch(error => {
  article.setAttribute("aria-busy", "false");
  article.innerHTML = '<h1>Документация временно недоступна</h1><p class="error-message">Не удалось загрузить статьи. Обновите страницу или откройте Markdown-версию на GitHub.</p><p><a href="https://github.com/chotaktota/AdrenalinGUI/tree/main/docs/pages">Статьи на GitHub</a></p>';
  console.error("Ошибка загрузки документации", error);
});
