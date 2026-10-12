// Execute the JavaScript rendered by JSP; no browser, external calls or dependencies.
const fs = require('node:fs');
const vm = require('node:vm');
const assert = require('node:assert/strict');
const path = require('node:path');
const html = fs.readFileSync(process.argv[2], 'utf8');
const scripts = [...html.matchAll(/<script\b[^>]*>([\s\S]*?)<\/script>/gi)].map(match => match[1]).filter(source => source.trim());
for (const source of scripts) new vm.Script(source); // Includes must also have valid JS.
const source = scripts.find(source => source.includes('CLICK RIPPLE'));
assert.ok(source, 'rendered effects script exists');
let click, ripple;
const button = {
  style: {}, getBoundingClientRect: () => ({left: 10, top: 20}),
  addEventListener: (_, callback) => { click = callback; },
  appendChild: element => { ripple = element; }
};
const context = {
  document: {
    addEventListener() {},
    querySelectorAll: selector => selector.startsWith('.btn-pkg') ? [button] : [],
    querySelector: () => ({style: {}}),
    createElement: () => ({style: {}, remove() {}}),
    head: {appendChild() {}}
  },
  window: {addEventListener() {}},
  IntersectionObserver: class {observe() {}}, setTimeout() {}
};
vm.runInNewContext(source, context);
click.call(button, {clientX: 42, clientY: 65});
assert.equal(ripple.style.left, '32px');
assert.equal(ripple.style.top, '45px');
console.log(`HomeViewScriptsTest: ${scripts.length} rendered scripts parsed; 2/2 click coordinate assertions passed`);

// Execute the actual shipped ordering script against CMS values rendered by JSP.
const ordering = fs.readFileSync(path.resolve(__dirname, '../../web/js/home-order.js'), 'utf8');
const cards = [...html.matchAll(/<div class="(feature|device)" data-cms-order="([^"]*)">/g)]
  .map(match => ({group: match[1], value: match[2], style: {}, getAttribute() {return this.value;}}));
assert.equal(cards.length, 8, 'all four benefits and four devices are rendered');
const invalid = ['', 'not-a-number', '2;display:none', '1.5', '2147483648', '9007199254740993']
  .map(value => ({value, style: {order: '7'}, getAttribute() {return this.value;}}));
const elements = cards.concat(invalid);
vm.runInNewContext(ordering, {document: {querySelectorAll: () => elements}});
for (const card of cards) assert.equal(card.style.order, card.value);
for (const element of invalid) assert.equal(element.style.order, '7');
for (const group of ['feature', 'device']) {
  const sorted = cards.filter(card => card.group === group).sort((a, b) => Number(a.style.order) - Number(b.style.order));
  assert.equal(sorted[0], cards.filter(card => card.group === group)[3], 'CMS moves the fourth card first');
}
console.log('HomeOrderTest: 16/16 assertions passed (8 rendered orders, 6 invalid values, 2 reordered groups)');
