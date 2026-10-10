<%@page contentType="text/html" pageEncoding="UTF-8"%>
<%@taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>
<%@taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions"%>
<!doctype html>
<html lang="vi"><head><meta charset="UTF-8"><meta name="viewport" content="width=device-width,initial-scale=1">
<title>${business ? 'Thông tin kinh doanh' : 'Nội dung website'} - FPT Sale Manager</title>
<link rel="icon" href="${pageContext.request.contextPath}/assets/images/favicon.png">
<link rel="stylesheet" href="${pageContext.request.contextPath}/assets/admin/admin.css">
<link rel="stylesheet" href="${pageContext.request.contextPath}/assets/admin/admin-customer.css">
<link rel="stylesheet" href="${pageContext.request.contextPath}/assets/admin/admin-content.css">
<script src="${pageContext.request.contextPath}/js/admin-content.js" defer></script></head><body>
<jsp:include page="/view/admin/admin-header.jsp"><jsp:param name="activeTab" value="${business ? 'business' : 'content'}"/></jsp:include>
<main class="admin-main cms-editor">
<div class="page-header-flex"><div><span class="cms-eyebrow">QUẢN TRỊ WEBSITE</span>
<h1>${business ? 'Thông tin kinh doanh' : 'Nội dung website'}</h1>
<p>${business ? 'Quản lý thông tin liên hệ, phí dịch vụ và nội dung ưu đãi.' : 'Chỉnh nội dung và ảnh theo từng khu vực của trang khách hàng.'}</p></div>
<a class="cms-secondary" href="${pageContext.request.contextPath}/home" target="_blank" rel="noopener">Xem trang khách hàng ↗</a></div>
<c:if test="${not empty cmsError}"><div class="alert alert-error" role="alert"><c:out value="${cmsError}"/></div></c:if>
<c:if test="${not empty sessionScope.cmsMessage}"><div class="alert alert-success" role="status"><c:out value="${sessionScope.cmsMessage}"/></div><c:remove var="cmsMessage" scope="session"/></c:if>
<div class="cms-overview"><span class="cms-dot"></span><strong>${fn:length(cmsSections)} khu vực quản lý</strong><span class="cms-overview-note">Thay đổi chỉ hiển thị sau khi bấm “Lưu và xuất bản”.</span></div>
<form id="cms-form" method="post" action="${pageContext.request.contextPath}/admin/${business ? 'business':'content'}" data-context="${pageContext.request.contextPath}" data-demo="${demoMode}">
<input type="hidden" name="csrf_token" value="${fn:escapeXml(csrfToken)}">
<div class="cms-layout"><aside class="cms-sidebar"><nav aria-label="Khu vực chỉnh sửa"><p>ĐI ĐẾN KHU VỰC</p>
<c:forEach items="${cmsSections}" var="section" varStatus="index"><a href="#cms-${section.id}" ${index.first ? 'aria-current="location"' : ''}><span class="cms-nav-number">${index.count}</span><c:out value="${section.title}"/></a></c:forEach>
</nav><div class="cms-sidebar-tip">${business ? 'Giá gói và tốc độ Internet được quản lý riêng tại mục Gói cước.' : 'Chọn khu vực để mở nội dung. Tải ảnh ngay tại ô ảnh cần thay.'}</div></aside>
<div class="cms-sections">
<c:forEach items="${cmsSections}" var="section" varStatus="index">
<details id="cms-${section.id}" class="cms-section" ${index.first ? 'open' : ''}>
<summary><span class="cms-section-number">${index.count}</span><span><strong><c:out value="${section.title}"/></strong><small><c:out value="${section.description}"/></small></span><span class="cms-chevron" aria-hidden="true">⌄</span></summary>
<div class="cms-section-body">
<c:forEach items="${section.groups}" var="group"><fieldset class="cms-field-group">
<c:if test="${not empty group.title}"><legend><c:out value="${group.title}"/></legend></c:if>
<div class="cms-field-grid">
<c:forEach items="${group.fields}" var="field"><c:set var="value" value="${cmsValues[field.key]}"/>
<c:choose><c:when test="${field.type == 'image'}">
<div class="cms-image-field" data-image-field><span class="cms-field-label" id="label-${field.key}"><c:choose><c:when test="${field.key == 'hero.title'}">Tiêu đề chính</c:when><c:when test="${field.key == 'hero.subtitle'}">Tiêu đề phụ</c:when><c:when test="${field.key == 'hero.description'}">Mô tả ngắn</c:when><c:when test="${field.key == 'hero.visible'}">Hiển thị phần đầu trang</c:when><c:when test="${field.key == 'hero.label'}">Nhãn nổi bật (tùy chọn)</c:when><c:otherwise><c:out value="${field.label}"/></c:otherwise></c:choose></span>
<input type="hidden" name="${field.key}" value="${fn:escapeXml(value)}" data-image-value>
<div class="cms-image-preview" ${empty value ? 'data-empty="true"' : ''}>
<c:choose><c:when test="${fn:startsWith(value, '/')}" ><img src="${pageContext.request.contextPath}${fn:escapeXml(value)}" alt="${fn:escapeXml(field.label)}" loading="lazy"></c:when><c:otherwise><img ${not empty value ? 'src="' : ''}${not empty value ? fn:escapeXml(value) : ''}${not empty value ? '"' : ''} alt="${fn:escapeXml(field.label)}" loading="lazy"></c:otherwise></c:choose>
<span class="cms-image-empty">Chưa có ảnh</span></div>
<div class="cms-image-actions"><label class="cms-upload-button">Tải ảnh lên<input type="file" accept="image/png,image/jpeg" aria-labelledby="label-${field.key}" data-image-upload ${demoMode ? 'disabled' : ''}></label>
<button type="button" class="cms-image-clear" data-image-clear ${demoMode ? 'disabled' : ''}>Bỏ ảnh</button></div>
<small>PNG/JPG · tối đa 2 MB · tối đa 4096px</small><span class="cms-upload-status" role="status" aria-live="polite"></span></div>
</c:when><c:otherwise>
<div class="cms-field ${fn:contains(field.key, 'escription') ? 'cms-field-wide' : ''}"><label for="field-${field.key}"><c:choose><c:when test="${field.key == 'hero.title'}">Tiêu đề chính</c:when><c:when test="${field.key == 'hero.subtitle'}">Tiêu đề phụ</c:when><c:when test="${field.key == 'hero.description'}">Mô tả ngắn</c:when><c:when test="${field.key == 'hero.visible'}">Hiển thị phần đầu trang</c:when><c:when test="${field.key == 'hero.label'}">Nhãn nổi bật (tùy chọn)</c:when><c:otherwise><c:out value="${field.label}"/></c:otherwise></c:choose></label>
<c:choose>
<c:when test="${field.type == 'boolean'}"><select id="field-${field.key}" name="${field.key}"><option value="true" ${value=='true'?'selected':''}>Hiển thị / Bật</option><option value="false" ${value=='false'?'selected':''}>Ẩn / Tắt</option></select></c:when>
<c:when test="${fn:contains(field.key, 'escription')}"><textarea id="field-${field.key}" name="${field.key}" maxlength="2000" rows="3"><c:out value="${value}"/></textarea></c:when>
<c:otherwise><input id="field-${field.key}" name="${field.key}" maxlength="${field.key == 'hero.title' || field.key == 'hero.subtitle' ? '30' : '2000'}" type="${field.type=='number'?'number':(field.type=='date'?'date':(field.type=='email'?'email':'text'))}" ${field.type=='number'?'min="0" max="100000000" step="1"':''} value="${fn:escapeXml(value)}"></c:otherwise>
</c:choose>
<c:if test="${field.key == 'business.standardFee'}"><small>VNĐ / lần lắp đặt. Không cộng vào cước hàng tháng.</small></c:if>
<c:if test="${field.key == 'business.promotionPackageIds'}"><small>ID gói, cách nhau bằng dấu phẩy (ví dụ: 1,2,3). Xem ID tại mục Gói cước.</small></c:if>
<c:if test="${fn:endsWith(field.key, 'Monthly')}"><small>VNĐ / tháng · phụ thu riêng cho Mesh WiFi.</small></c:if>
<c:if test="${fn:endsWith(field.key, 'Fee') && field.key != 'business.standardFee'}"><small>VNĐ / lần lắp đặt Mesh.</small></c:if>
<c:if test="${fn:endsWith(field.key, '.order')}"><small>Số nhỏ hơn hiển thị trước.</small></c:if>
</div></c:otherwise></c:choose>
</c:forEach></div></fieldset></c:forEach></div></details></c:forEach>
<div class="cms-save-bar"><div><strong>Lưu thay đổi của anh/chị</strong><span id="cms-save-hint" role="status">${demoMode ? 'Tài khoản demo chỉ được xem nội dung.' : 'Nội dung sẽ được cập nhật trên trang khách hàng.'}</span></div>
<div class="cms-save-actions"><a class="cms-secondary" href="${pageContext.request.contextPath}/admin/dashboard">Quay lại</a><button class="btn-add-customer" type="submit" ${demoMode ? 'disabled' : ''}>Lưu và xuất bản</button></div></div>
</div></div></form>
<c:if test="${!demoMode}"><details class="cms-history cms-section"><summary><span class="cms-section-number">↶</span><span><strong>Lịch sử xuất bản</strong><small>Xem các lần lưu và khôi phục nội dung trước đó.</small></span><span class="cms-chevron" aria-hidden="true">⌄</span></summary>
<div class="cms-section-body"><p class="cms-history-note">Chỉ khôi phục bản gần nhất khi chưa có lần xuất bản khác ghi đè.</p>
<c:choose><c:when test="${empty revisions}"><div class="cms-history-empty">Chưa có lần xuất bản nào.</div></c:when><c:otherwise><div class="cms-history-list">
<c:forEach items="${revisions}" var="revision" varStatus="index"><div class="cms-history-row"><div><strong>Bản #<c:out value="${revision.id}"/></strong><span><c:out value="${revision.time}"/> · Admin #<c:out value="${revision.actor}"/></span></div>
<form method="post"><input type="hidden" name="csrf_token" value="${fn:escapeXml(csrfToken)}"><input type="hidden" name="action" value="rollback"><input type="hidden" name="revisionId" value="${revision.id}"><button class="cms-secondary" type="submit" ${index.first ? '' : 'disabled'}>Khôi phục</button></form></div></c:forEach></div></c:otherwise></c:choose>
</div></details></c:if>
</main></body></html>
