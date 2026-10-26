# Independent reviews during Phase 05 execution

These are source-level review results, not substitutes for SQL Server, concurrency or browser verification. The baseline checkout is `ca2bbf8`; repairs remain uncommitted by design. The final reviewer must assess the current working tree and new source files, not an empty commit range.

| Review finding | Corrective ownership / evidence |
|---|---|
| Self-dropoff weight surcharge accepted only CASH and rejected an existing paid source | 05-03 added authorized assessment, sender wallet/demo-VNPay settlement and matching paid-source consumption. Reviewed source; focused tests passed. 05-04 extends cancellation refunds to the surcharge. |
| Tracking order undefined for events sharing DATETIME2(0) timestamp | 05-02 query orders by createdAt then id; a real SQL regression exists but execution was environment-skipped. |
| Missing-item work list linked to shipment audit scoped to the wrong physical hub | 05-02 adds incident-scoped investigation route and fixes link without broadening global shipment audit. |
| Departed driver lost audit access when currentHub became null | 05-03 authorizes assigned driver from active custody before physical-hub guard; unrelated driver remains denied. |
| Custody migration renamed statuses but did not reconcile legacy manifested state/location/vehicle facts | 05-05 must repair with evidence or fail closed. Legacy fixture needs actual STARTED/partially received ARRIVED journeys and repeat application, not empty metadata only. |
| Record DTO properties failed actual Tomcat EL 10.1.30 | Read-only EL probe produced PropertyNotFoundException for OperationalWorkItemResponse.summary, MissingParcelCaseResponse.incidentId, VehicleOptionResponse.plateNumber. 05-05 owns JavaBean/accessor fixes and populated Jasper tests, including older dashboard/sorting/driver/audit records. |
| Failed delivery fabricated immediate physical hub receipt | 05-03 adds real inbound return scan; assignment/routing require physical inventory. |
| Retry ignored next-day or explicit rescheduled date | 05-03 shared date guard filters candidates and rechecks assignment and outbound scan using Clock; source rechecked independently. |
| Overdue-return JSP had an unclosed c:out | Fixed in 05-03; populated Jasper proof belongs to 05-05. |
| Automatic refund events with null actor vanished from audit | 05-04 left-joins actor and maps SYSTEM; focused regression passed. |
| Valid compensation reason overflowed event description after prefix | 05-04 bounds persisted event summary while retaining investigation evidence. |
| Legitimate zero-payable loss could never close | 05-04 closes the confirmed loss without creating a zero-value ledger entry; preserves evidence/scope/replay checks. |
| Reconciliation and retained finance JSPs used invalid JSTL taglib URLs | 05-04 fixes rewritten reconciliation page. 05-05 fixes remaining wallet/withdrawal and reachable pages using jakarta.tags.* and actual Jasper tests. |
| Accepted electronic COD had no continuation to payout | Canonical Workflow §95 allows CASH/VNPAY/TRANSFER. 05-04 adds method-specific reconciliation with scoped manager-recorded settlement evidence, then separate payout; mixed batches rejected. This is manual/demo confirmation, not real provider verification. |
| Collection form submitted no actual amounts, and one method conflated COD with receiver shipping fee | 05-04 adds explicit amounts and separate methods. COD accepts CASH/VNPAY/TRANSFER; receiver fee accepts CASH/VNPAY. Both are validated before mutation. |

The last independent review of 05-04 reported no remaining source blocker in these deltas. It did not run Maven or SQL and did not verify Jasper/browser execution. The final integration executor and review/verification gates must report their own evidence and any additional defects.

User decision: fixed default return fee **25,000 VND**, configurable. User confirmed a dedicated SQL Server instance exists and would configure test environment variables; presence and actual test execution must still be checked without printing secrets.
