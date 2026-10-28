from pathlib import Path
p=Path('src/main/webapp/WEB-INF/views/layout/sidebar.jsp')
s=p.read_text(encoding='utf-8')
start=s.index('\n<a href=',s.rfind('</div>'))
s=s[:start]
hub='''<li class="nav-item"><a class="nav-link py-1 text-ute-dark" href="${pageContext.request.contextPath}/hub/delivery-assignments">Phân công giao / hoàn hàng</a></li>
<li class="nav-item"><a class="nav-link py-1 text-ute-dark" href="${pageContext.request.contextPath}/hub/return-collection">Bàn giao hàng hoàn tại hub</a></li>
'''
manager='''<li class="nav-item"><a class="nav-link py-1 text-ute-dark" href="${pageContext.request.contextPath}/manager/overdue-returns">Hàng hoàn quá hạn</a></li>
'''
first=s.index('</ul>');s=s[:first]+hub+s[first:]
last=s.rindex('</ul>');s=s[:last]+manager+s[last:]
p.write_text(s,encoding='utf-8')
