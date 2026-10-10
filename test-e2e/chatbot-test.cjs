'use strict';
const assert = require('node:assert/strict');
const fs = require('node:fs');
const vm = require('node:vm');
const chat = require('../web/js/ai-chat.js');
let passed = 0;
function test(name, run) { run(); passed++; console.log('PASS ' + name); }
function documentStub() {
    const doc = {createElement(tag) {
        return {tag, ownerDocument: doc, children: [], style: {}, textContent: '', classList: {toggle() {}},
            appendChild(child) { this.children.push(child); }, remove() { this.removed = true; }, focus() {}};
    }};
    return doc;
}
test('frontend renders only message text, never the structured envelope or metadata', () => {
    const doc = documentStub(), body = doc.createElement('div');
    const bubble = chat.appendReply(body, {reply: 'Em chào anh/chị.', action: 'none', state: {secret: 'do not show'}, intent: 'consultation'}, '/fpt-sale');
    assert.equal(bubble.textContent, 'Em chào anh/chị.'); assert.equal(bubble.children.length, 0);
    assert.equal(body.children.length, 1);
});
test('frontend rejects object/empty messages and unknown actions', () => {
    for (const data of [{reply: {} ,action: 'none'}, {reply: '', action: 'none'}, {reply: 'Chào anh/chị.', action: 'submit'}])
        assert.throws(() => chat.validatedReply(data));
});
test('registration link is local and allowlisted; chat never submits a form', () => {
    const doc = documentStub(), body = doc.createElement('div');
    const bubble = chat.appendReply(body, {reply: 'Anh/chị vui lòng mở biểu mẫu.', action: 'open_registration', registrationPath: '/package-detail?id=3#registration'}, '/fpt-sale');
    assert.equal(bubble.children[0].href, '/fpt-sale/package-detail?id=3#registration');
    for (const path of ['javascript:alert(1)', 'https://example.com/', '//example.com/', '/ContactServlet'])
        assert.throws(() => chat.validatedReply({reply: 'Chào anh/chị.', action: 'open_registration', registrationPath: path}));
});
test('response markup remains literal customer text rather than executable HTML', () => {
    const doc = documentStub(), body = doc.createElement('div');
    const bubble = chat.appendReply(body, {reply: '<img src=x onerror=alert(1)>', action: 'none'}, '');
    assert.equal(bubble.textContent, '<img src=x onerror=alert(1)>'); assert.equal(bubble.children.length, 0);
});
(async () => {
    const doc = documentStub(), body = doc.createElement('div'), input = doc.createElement('input');
    doc.getElementById = id => id === 'aiChatInput' ? input : body;
    let requests = 0, finish;
    const window = {document: doc, setTimeout, clearTimeout, fetch: async () => {
        requests++; await new Promise(resolve => {finish = resolve;});
        return {json: async () => ({reply: 'Em chào anh/chị.', action: 'none'})};
    }};
    vm.runInNewContext(fs.readFileSync(require.resolve('../web/js/ai-chat.js'), 'utf8'), {window, AbortController, URLSearchParams});
    window.FptChat.init('/fpt-sale');input.value = 'Chào bạn!';const first = window.sendAIMessage();
    input.value = 'Câu hỏi tiếp theo';await window.sendAIMessage();assert.equal(requests, 1);assert.equal(input.disabled, true);
    finish();await first;assert.equal(input.disabled, false);assert.equal(body.children.at(-1).textContent, 'Em chào anh/chị.');
    passed++;console.log('PASS in-flight guard prevents reordered concurrent UI requests');
    for (const failure of ['network', 'malformed']) {
        window.fetch = async () => { if (failure === 'network') throw new Error('offline'); return {json: async () => ({reply: {state: 'SECRET'}, action: 'none'})}; };
        input.value = 'Có truyền hình và camera.'; await window.sendAIMessage();
        assert.match(body.children.at(-1).textContent, /0932 079 469/);
        assert(!body.children.at(-1).textContent.includes('SECRET'));
        assert(!body.children.at(-1).textContent.includes('ngoài phạm vi'));
        assert.equal(input.disabled, false);
    }
    passed++;console.log('PASS network and malformed failures offer human contact and allow retry');
    console.log('Chat frontend: ' + passed + ' passed');
})().catch(error => {console.error(error);process.exitCode = 1;});
