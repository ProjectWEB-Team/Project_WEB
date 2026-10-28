from pathlib import Path
root=Path('.')
for name in ['HubRoutingServiceImplTest','ContainerHandoverServiceImplTest']:
 p=root/f'src/test/java/vn/iotstar/service/{name}.java'
 s=p.read_text(encoding='utf-8')
 import re
 s=re.sub(r'(shipment:101:hub:\d+)(")',r'\1:FORWARD\2',s)
 if name=='HubRoutingServiceImplTest':
  s=s.replace('discrepancy.completeDestinationReceipt(true);','discrepancy.depart();\n        discrepancy.completeDestinationReceipt(true);').replace('isEqualTo("DISCREPANCY")','isEqualTo("COMPLETED_WITH_DISCREPANCY")')
 p.write_text(s,encoding='utf-8')
p=root/'src/main/java/vn/iotstar/controller/LastMileDeliveryController.java'
s=p.read_text(encoding='utf-8').replace('return "redirect:/hub/delivery-assignments";', 'return "redirect:/hub/inbound";')
s=s.replace('@PostMapping({"/user/return-collection", "/hub/return-collection"})','@PostMapping("/hub/return-collection")')
s=s.replace('return "redirect:/user/return-collection";', 'return "redirect:/hub/return-collection";')
s=s.replace('@GetMapping({"/user/return-collection", "/hub/return-collection"})','@GetMapping("/hub/return-collection")')
p.write_text(s,encoding='utf-8')
p=root/'src/main/webapp/WEB-INF/views/hub/delivery-assignments.jsp';s=p.read_text(encoding='utf-8')
start=s.index('<h2>');end=s.index('<a href=',start);form=s[start:end];s=s[:start]+s[end:];p.write_text(s,encoding='utf-8')
p=root/'src/main/webapp/WEB-INF/views/hub/inbound-queue.jsp';s=p.read_text(encoding='utf-8').replace('</body>',form+'</body>');p.write_text(s,encoding='utf-8')
p=root/'src/main/webapp/WEB-INF/views/shipment/detail.jsp';s=p.read_text(encoding='utf-8');start=s.index('\n<p><a href=',s.index('</html>'));link=s[start:].strip();s=s[:start].replace('</main>',link+'\n</main>');p.write_text(s,encoding='utf-8')
