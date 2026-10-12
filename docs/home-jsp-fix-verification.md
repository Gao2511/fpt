# home.jsp verification — 2026-10-12

Scope: user reports red `$` markers in `web/view/home.jsp`. Offline/local checks only; no commit, push, deployment, Gemini request or production database connection.

## Findings and changes

- JSP EL `${...}` is valid. Jasper/Tomcat 9 compiled all 32 view JSPs with zero errors before the patch. An IDE marker alone does not establish a JSP compilation failure.
- A real rendering defect existed at the ripple animation: JavaScript template substitutions `${x}` and `${y}` were evaluated as server JSP expressions. The pre-patch generated servlet splits the string and evaluates these expressions, which normally renders `left: px; top: px;`. Set `ripple.style.left/top` from JavaScript variables instead.
- Escape the welcome name and prefilled name, phone and email in `home.jsp` so markup and quotes cannot alter those elements. This is scoped to this view; it is not a claim that all website output has been security audited.
- Remove the obsolete Cloudinary reference from `nbproject/project.xml`. The corresponding two generated entries were also removed from the ignored local `nbproject/build-impl.xml`. No source uses Cloudinary; the actual runtime dependencies remain unchanged.
- Add authoritative `lib/runtime` and `lib/compile-only` paths to the ignored local `.vscode/settings.json`, retaining existing references.
- Expand `test/compile-cms-jsps.ps1` from a fixed page list to all JSPs under the staged `view` directory, including dynamic include targets.
- Add a Tomcat loopback rendering harness with synthetic servlet data and a Node VM check for the JavaScript actually rendered by JSP. The Tomcat harness is excluded from the portable unit test compilation like the existing browser harness; its runner supplies Tomcat dependencies explicitly.

## Files changed in this fix

- `web/view/home.jsp`
- `nbproject/project.xml`
- `nbproject/build-impl.xml` (ignored, generated local file)
- `.vscode/settings.json` (ignored local IDE configuration)
- `test/compile-cms-jsps.ps1`
- `build-portable.xml`
- `test/cms/HomeViewTest.java` (new)
- `test/cms/home-view-scripts.test.cjs` (new)
- `test/run-home-view-tests.ps1` (new)
- This report.

Existing uncommitted Phase 1–4 changes were preserved.

## Executed verification

- `ant -f build-portable.xml war`: BUILD SUCCESSFUL (build only, no deployment).
- `ant -f build.xml compile "-Dj2ee.deploy.on.save=false"`: BUILD SUCCESSFUL after removing stale generated Cloudinary entries. Quote the property argument in PowerShell.
- Jasper compilation: **32 JSPs, zero errors**.
- HomeViewTest: **8/8 passed**, anonymous/customer/empty-catalogue rendering with the real JSP and includes; context path links, chat and theme elements, escaped prefilled values and welcome name checked.
- HomeViewScriptsTest: **12 rendered inline scripts parse**, **2/2 click-coordinate assertions passed** for a synthetic click at `(42,65)` with a button offset `(10,20)`.
- Portable regression: **14 suites completed, BUILD SUCCESSFUL** (255 explicitly numbered tests/checks plus the aggregate EditorLayoutTest).
- `git diff --check` for the changed tracked view/config/compiler files: passed.

Evidence:

- Pre-patch JSP generated source/log: `.build/home-jsp-check-7796de58678743d8b714fac81ebc5bc4/`.
- Final rendered fixtures and JavaScript/render logs: `.build/home-view-test-c7450a349b8e43eca22a646f27ee66ca/`.
- Regression log: `.build/home-fix-regression.log`.
- Initial rendering harness passed 8/8, but its Node mock lacked `document.addEventListener`; the mock was corrected and the combined runner then passed. Earlier evidence remains present.

## Local reproduction

Use Ant/JDK17 and PowerShell 7. Build the portable WAR, then run:

```powershell
pwsh -NoProfile -File test/compile-cms-jsps.ps1 -TomcatHome '<Tomcat9 directory>' -AntJar '<Ant directory>/lib/ant.jar'
pwsh -NoProfile -File test/run-home-view-tests.ps1 -TomcatHome '<Tomcat9 directory>'
```

The rendering harness binds only to `127.0.0.1`, uses synthetic request attributes, ignores application annotations and registers only its fixture servlet. It does not run HomeServlet, DAO, AI or production filters. Its temporary server stops after verification. `RAG_ENABLED=false` is set only for the runner process; production settings are untouched.

Reopen/reload the IDE project to refresh dependency indexing. If valid EL is still red, inspect the IDE language mode/JSP support and exact diagnostic. This task verified compilation and rendered behavior; it did not visually inspect the user's IDE or redeploy their running local website.

## Follow-up: dynamic CSS order markers

The administrator identified `style="order:${cms['benefit1.order']};"` specifically. This server expression compiles correctly, but a CSS parser looking at the JSP source sees `${...}` instead of an integer. The previous compilation checks did not address that editor diagnostic.

- Updated all eight affected elements: `benefit1`–`benefit4` and `device1`–`device4` in `home.jsp`. The view now renders escaped `data-cms-order` HTML attributes instead of dynamic inline CSS.
- Added `web/js/home-order.js`, loaded after the cards. It applies the configured integer as `element.style.order`; malformed or out-of-range values are ignored. CMS settings and business data remain unchanged.
- No inline `style` attribute containing JSP EL remains in `home.jsp`. This does not assert that the IDE itself has been visually inspected or that other included JSPs lack similar markers.
- Updated the rendering fixture to reverse the first/fourth benefit and device priorities. Executed tests: **9/9 rendering checks**, **16/16 ordering assertions** (eight rendered values, six malformed values, two reordered groups), the existing **2/2 ripple assertions**, and parsing of **12 rendered scripts**. All **32 JSPs** compiled with zero errors; portable WAR build passed.
- New evidence: `.build/home-view-test-d8a57826afa44393b0bd517c7ace542b/`. Previous evidence remains present. Full backend regression from the earlier verification was not repeated for this view-only follow-up.
- Ordering now requires the page's JavaScript, like its existing interactions. If JavaScript is disabled or the new asset cannot load, the cards retain their original markup order. No production deployment was performed.

## Follow-up: VS Code `build.bat dist` failure

The IDE process had neither `ANT_HOME` nor an Ant executable on `PATH`, although the existing NetBeans Ant installation was present. Updated `build.bat` to find Ant via a valid `ANT_HOME`, then `PATH`, then that existing local installation. It uses `call` and propagates Ant's exit code; no system environment variables were changed and no downloads were needed.

Executed the user's exact command `./build.bat dist`: **BUILD SUCCESSFUL**, exit 0, producing `dist/fpt-sale.war`. Confirmed the archive contains `view/home.jsp` and `js/home-order.js`. Additional smoke checks passed for explicit ANT_HOME, PATH discovery, invalid ANT_HOME fallback, and propagation of an intentional missing-target failure as exit 1. This was a local build only; no server start, deploy, database or AI call occurred.
