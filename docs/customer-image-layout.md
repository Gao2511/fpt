# Customer-page image fitting

Built-in imagegen edited the two former 1518×1036 banners into 1672×941 landscape assets (approximately 16:9). The generated assets replace the existing canonical Hero2/Hero3 filenames; prior originals remain recoverable from Git and were also backed up locally. home.jsp references them with v=fit-20261010 to refresh browser caches.

- `web/assets/images/Hero2.png`
- `web/assets/images/Hero3.png`

## Generation prompts

WiFi banner: Recompose the original orange FPT Telecom WiFi 6 artwork into a landscape 16:9 carousel, with white FPT logo, realistic modem/router on the left, large typography on the right and safe margins. Exact copy: “LẮP ĐẶT INTERNET”, “WIFI 6”, “KẾT NỐI CHO GIA ĐÌNH”. No prices, phone numbers, unlimited-coverage or four-times-speed claims.

Family Internet banner: Recompose the original orange FPT Telecom campaign into landscape 16:9, retaining the logo, router and camera appearance. Exact copy: “MẠNG NHANH”, “GIÁ TỐT”, “FPT TELECOM GIA LAI”, “fpt.gialai.vn”. Keep products and text inside the frame; no invented prices, promotions or specifications.

## Code changes and verification

`home.css` now uses a responsive 16:9 hero frame and `object-fit: contain`, replacing fixed breakpoint heights and stretched `fill`. Square device illustrations use square frames and no hover enlargement. Carousel arrows and indicators sit below the artwork, keeping embedded text unobstructed. `home.jsp` references the new banners and groups those controls below the slideshow.

Chrome verification used actual image sections extracted from home.jsp with home.css, theme-toggle.css and performance-optimize.css; all assets were served locally without a database. At widths 360, 390, 768, 1440 and 1920px, all 11 images loaded, were contained inside their frames, the banner retained 16:9, and the page had no horizontal overflow. Desktop/mobile carousel screenshots were visually inspected: title, brand and website text remain visible.

No prices, admin dashboard, package data or chatbot logic changed. Generated product imagery is illustrative, based on the supplied existing artwork. Release requires the usual approved deployment. Rollback restores prior home.jsp/home.css and the prior WAR; prior originals are recoverable from Git.

Build verification: build.bat dist succeeded; Jasper compiled view/home.jsp with 0 errors (existing PostgreSQL JAR-name scan warnings only); git diff --check found no whitespace errors.

Filename correction: deployed /assets/images/responsive/hero-wifi6.png returned HTTP 404 while /assets/images/Hero2.png returned HTTP 200 with Content-Type image/png. Both generated files have a valid PNG signature. Preserve canonical Hero2.png/Hero3.png paths and deploy the assets together with the JSP/WAR; no extension-only rename or format conversion is needed.

Local port 8084 follow-up: all 11 images returned HTTP 200 and decoded successfully in fresh Chrome. Added a 56.25% intrinsic spacer so absolute-positioned slides cannot collapse on browsers without aspect-ratio, and versioned home.css to refresh cached styles. Verified on the actual local server at widths 390/1440 with aspect-ratio explicitly disabled: frame heights 196.875/459px and all 11 images loaded. The normal WAR was locked by the running server; built dist/fpt-sale-images.war separately instead. The reported failure in the user's existing browser profile was not reproduced; root cause remains unconfirmed.
