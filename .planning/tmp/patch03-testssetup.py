from pathlib import Path
root=Path('src')
# Adapt existing forward regressions to physical outbound custody and active persisted employee data.
p=root/'test/java/vn/iotstar/service/LastMileDeliveryServiceImplTest.java';s=p.read_text(encoding='utf-8');s=s.replace('import vn.iotstar.service.impl.LastMileDeliveryServiceImpl;', 'import vn.iotstar.service.impl.LastMileDeliveryServiceImpl;\nimport vn.iotstar.service.impl.LastMileAccess;\nimport vn.iotstar.service.impl.ShipmentFeeService;\nimport vn.iotstar.entity.Hub;\nimport vn.iotstar.entity.EmployeeProfile;\nimport vn.iotstar.repository.EmployeeProfileRepository;\nimport org.springframework.test.util.ReflectionTestUtils;\nimport java.time.LocalDateTime;')
s=s.replace('    private final LogisticsProperties', '    @Mock private EmployeeProfileRepository profiles;\n    private final LogisticsProperties')
s=s.replace('                storageService\n', '                storageService, new LastMileAccess(profiles), new ShipmentFeeService(shipmentRepository,paymentRepository,shipmentAssignmentRepository,shipmentEventRepository,userRepository,walletLedgerService,new LastMileAccess(profiles))\n')
s=s.replace('        assignment = ShipmentAssignment.create(shipment, shipper, "DELIVERY");','''        assignment = ShipmentAssignment.create(shipment, shipper, "DELIVERY");
        Hub hub=Hub.active("H100","Hub",1,"DELIVERY_HUB","SOUTH","P");
        ReflectionTestUtils.setField(hub,"id",100L);
        lenient().when(profiles.findFirstByUserId(10L)).thenReturn(Optional.of(EmployeeProfile.active(shipper,hub,"SHIPPER")));''')
s=s.replace('shipment.transitionTo(ShipmentStatus.OUT_FOR_DELIVERY);','shipment.transitionTo(ShipmentStatus.OUT_FOR_DELIVERY);\n        shipment.setCurrentHubId(null); assignment.setStatus("IN_PROGRESS"); assignment.setAcceptedAt(LocalDateTime.now());')
s=s.replace('.hasMessageContaining("not at the scanning hub")','.hasMessageContaining("Wrong scan hub")')
s=s.replace('new BigDecimal("30000.00"), "CASH", "PAID");','new BigDecimal("30000.00"), "CASH", "UNPAID");\n        feePayment.collect("CASH",10L,"TEST-DELIVERY");')
p.write_text(s,encoding='utf-8')
# UI corrections: no uninstalled security tag library, separate payment/release actions.
p=root/'main/webapp/WEB-INF/views/layout/sidebar.jsp';s=p.read_text(encoding='utf-8');s=s.replace('<sec:authorize access="hasRole(\'HUB_STAFF\')">','').replace('</sec:authorize>','');s += '\n<a href="${pageContext.request.contextPath}/manager/overdue-returns">Hàng hoàn quá hạn</a>\n';p.write_text(s,encoding='utf-8')
p=root/'main/webapp/WEB-INF/views/user/return-collection.jsp';s=p.read_text(encoding='utf-8');s=s.replace('/user/return-collection" method="post"','/hub/return-collection" method="post"');start=s.index('                <div class="mb-3">\n                    <label for="settledReturnFee"');end=s.index('                <button type="submit"',start);s=s[:start]+s[end:];s=s.replace('</main>', '''<h2>Thu phí hoàn bằng tiền mặt tại hub</h2><form action="${pageContext.request.contextPath}/hub/return-collection/cash" method="post">
<input type="hidden" name="${_csrf.parameterName}" value="${_csrf.token}"><label>Mã ID vận đơn<input name="shipmentId" type="number" required></label>
<button>Xác nhận đã thu đủ nghĩa vụ phí được hệ thống tính</button></form>
<p>Người gửi thanh toán bằng ví/VNPay demo tại trang phí vận đơn, nhân viên xác minh danh tính và bàn giao sau khi đã trả đủ phí.</p></main>''');p.write_text(s,encoding='utf-8')
p=root/'main/webapp/WEB-INF/views/shipper/return-tasks.jsp';s=p.read_text(encoding='utf-8');anchor='                                        <form action="${pageContext.request.contextPath}/shipper/return-tasks/confirm"';s=s.replace(anchor,'''                                        <form method="post" action="${pageContext.request.contextPath}/shipper/return-tasks/scan"><input type="hidden" name="${_csrf.parameterName}" value="${_csrf.token}"><input name="shipmentId" type="hidden" value="${task.shipmentId}"><button>Quét xuất kho</button></form>
                                        <form method="post" action="${pageContext.request.contextPath}/shipper/return-tasks/cash"><input type="hidden" name="${_csrf.parameterName}" value="${_csrf.token}"><input name="shipmentId" type="hidden" value="${task.shipmentId}"><button>Xác nhận thu đủ phí tiền mặt</button></form>
'''+anchor);p.write_text(s,encoding='utf-8')
# Shipment details already scoped to owner; provide navigation to actual fee settlement.
p=root/'main/webapp/WEB-INF/views/shipment/detail.jsp'
if not p.exists():
 matches=list((root/'main/webapp/WEB-INF/views').rglob('*detail.jsp'));p=next((x for x in matches if 'shipment' in str(x)),None)
if p:
 s=p.read_text(encoding='utf-8');s += '\n<p><a href="${pageContext.request.contextPath}/user/shipment-fees/${shipment.id}">Thanh toán phụ thu / phí hoàn</a></p>\n';p.write_text(s,encoding='utf-8')
