<%@page contentType="text/html" pageEncoding="UTF-8"%>
<%@taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>
<c:if test="${kbTab == 'documents' || kbTab == 'verify' || kbTab == 'advanced'}">
 <section class="kb-panel"><h3>Tri thức từ file Git · FILE_MANAGED</h3>
 <p>Sửa TXT/Markdown trong knowledge/, tăng version rồi deploy. Đồng bộ chỉ nhập file của deployment hiện tại và xếp hàng; index dùng ngân sách worker hiện có. Không tự bật RAG.</p>
 <p>Runtime: <c:out value="${fileSyncRuntimeStatus}"/> <c:out value="${fileSyncNotice}"/></p>
 <form method="post" action="${pageContext.request.contextPath}/admin/knowledge-base">
  <input type="hidden" name="csrf_token" value="<c:out value='${csrfToken}'/>"><input type="hidden" name="action" value="syncRepositoryFiles"><input type="hidden" name="tab" value="documents">
  <button type="submit" class="kb-btn">Đồng bộ file của deployment · 0 API trong thao tác đồng bộ</button>
 </form>
 <p>Worker riêng có thể index khi đã được cấp phép. Đồng bộ thành công chưa chứng minh chatbot dùng RAG. “Đủ điều kiện” là kiểm tra metadata/index; retrieval còn kiểm tra xung đột nguồn và gate customer RAG.</p>
 <div class="kb-table-wrap"><table class="kb-table"><thead><tr><th>ID / File / Phiên bản</th><th>SHA-256</th><th>Sync / Index / Chunks</th><th>Provider / Đủ điều kiện</th><th>Chunk được gửi Gemini gần đây</th></tr></thead><tbody>
 <c:forEach var="f" items="${fileSources}"><tr><td><a href="${pageContext.request.contextPath}/admin/knowledge-base?tab=documents&amp;docId=${f.id}"><c:out value="${f.file_id}"/></a><br><c:out value="${f.file_path}"/> · v<c:out value="${f.file_version}"/></td><td><code style="word-break:break-all"><c:out value="${f.file_sha}"/></code></td><td><c:out value="${f.state}"/> / <c:out value="${f.indexing_status}"/> / <c:out value="${f.indexed_chunks}"/></td><td><c:out value="${f.embedding_provider}"/> / <c:out value="${f.eligible}"/></td><td><c:out value="${f.recentPassedToGemini}"/></td></tr></c:forEach>
 <c:if test="${empty fileSources}"><tr><td colspan="5">Chưa có nguồn file đã đồng bộ hoặc migration chưa sẵn sàng.</td></tr></c:if>
 </tbody></table></div>
 </section>
</c:if>
