# Fix JSP HTTP 500 / fn:escapeXml — 2026-10-12

Scope: urgent hotfix while Phase 5 is paused. Supabase configuration, credentials and database remain unchanged. No production connection, Gemini request, commit, push or deployment was performed.

## Confirmed cause

The deployed-input WAR contained both `WEB-INF/lib/jstl-1.2.jar` and `WEB-INF/lib/standard-1.0.6.jar`. Both define `org.apache.taglibs.standard.tag.common.core.Util`. The old implementation lacks `escapeXml(String)`, while the JSTL 1.2 `Functions.escapeXml` calls it. Loading the old Util first reproduces the reported `NoSuchMethodError` without any database or network operation.

`javap` confirms the method exists in the 1.2 Util and is absent from the 1.0.6 Util. This explains the HTTP 500 at JSP rendering; it is not evidence of a Supabase authentication or connectivity failure. The failure can depend on classloader/JAR order.

Previous Jasper compilation and parent-delegated Home tests missed this runtime conflict because their classpath loaded the compatible implementation first. A new test now loads the actual WAR libraries with an isolated classloader, counts class providers and checks both JAR orders.

## Patch

- `tools/dependencies.lock`: stop downloading legacy Standard 1.0.6. JSTL 1.2 already bundles the implementation used by this application.
- `nbproject/project.xml`, `nbproject/project.properties`: remove the old dependency and exclude legacy Standard JARs from the archive.
- `build.xml`: before NetBeans packaging, remove only generated `standard-*.jar` copies from the configured build web library directory.
- `build-portable.xml`: exclude legacy Standard from compilation/copy/archive; clear stale generated copies on an incremental WAR build.
- `nbproject/build-impl.xml`: corresponding entries updated in the ignored generated local build script; NetBeans can regenerate it from project.xml.
- `test/cms/JstlRuntimeTest.java`, `test/test-jstl-runtime.ps1`: offline reproduction, duplicate class detection and runtime validation of the actual WAR.
- `dist/fpt-sale.war`: rebuilt with the portable build for the existing Render Dockerfile. Supabase remains the application's database.

Original `lib/runtime/standard-1.0.6.jar` is retained locally as a negative-test fixture and is no longer packaged. No historical evidence, execution locks, business data or original libraries were deleted. Existing JSP escaping remains enabled.

## Executed results

| Check | Measured result |
| --- | --- |
| Old class order | Exact NoSuchMethodError reproduced |
| Portable WAR runtime test | 6/6 PASS; exactly one Util and one Functions provider |
| NetBeans WAR runtime test | 6/6 PASS; reversed library order also works |
| Real Home JSP/includes with synthetic data on loopback Tomcat | 9/9 PASS |
| Rendered inline JavaScript | 12 scripts parse; 2/2 ripple and 16/16 CMS order assertions PASS |
| Jasper | All 32 view JSPs compile, zero errors |
| Portable regression | 14 suites completed, BUILD SUCCESSFUL; 255 numbered checks plus aggregate EditorLayoutTest |
| Portable WAR / NetBeans dist builds | Both BUILD SUCCESSFUL |
| git diff --check | PASS |

Local Tomcat was 9.0.118 on JDK17. The user's reported production version is 9.0.122; no live production verification is claimed. The isolated Java classloader tests validate the incompatible-method condition independently of a Tomcat patch version.

Evidence retained:

- `.build/jstl-runtime-498d83c189314a48a12b817823c7edc4/` (portable WAR)
- `.build/jstl-runtime-3deeb3b3b9a844689170d824f0e050c2/` (NetBeans WAR)
- `.build/home-view-test-4d790fdf93454e49b5626c816b0a9745/`
- `.build/jstl-fix-regression-19be4ae7409344438ff54e21f2ed0034.log`

## Release boundary

The local patch and WAR are ready. Per the new Phase 5 instruction, pushing/production deployment requires separate authorization. Pushing this hotfix to Render's `main` would trigger the configured automatic deployment. No database migration is required for this hotfix; do not change Supabase or encryption keys to resolve it. Keep RAG and live indexing disabled.

After an authorized update, verify Home and normal escaped CMS text render without this exception. If the error persists, inspect whether any server-wide Tomcat library still supplies the old Util; do not remove `fn:escapeXml` or HTML escaping as a workaround. Roll back this hotfix with a reviewed forward revert/build if necessary; the previous WAR itself contains the known JSTL conflict, so reverting blindly can restore the 500.
