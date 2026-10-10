<%@ tag pageEncoding="UTF-8" %>
<%@ attribute name="plan" required="true" type="dto.PackageDTO" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<c:if test="${plan.standardInstallationFee > 0}">
    <fmt:formatNumber value="${plan.standardInstallationFee}" pattern="#,###" var="feeAmount"/>
    <p class="standard-installation-fee" style="font-size:13px;line-height:1.6;margin:10px 0;overflow-wrap:anywhere;">
        Phí lắp đặt thông thường: <strong>${fn:replace(feeAmount, ',', '.')}đ/lần</strong>
    </p>
</c:if>
