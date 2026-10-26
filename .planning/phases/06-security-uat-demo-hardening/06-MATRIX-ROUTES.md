# Phase 06 — Route permission matrix (HARD-01, plan 06-01)

Source branch: `feature/phase-06-hardening`. Tests: `PermissionMatrixRouteTest` (web chain, 1002 cells), `PermissionMatrixApiChainTest` (`/api/**` JWT chain, 56 cells).

Routes covered: 104

## Legend

- **Allowed roles**: the documented policy; every other role must get 403. Anonymous callers are redirected to `/login` on the web chain and get `401 {"error":"Authentication required"}` on the API chain. "public" means every principal, anonymous included, passes the chain.
- **Kind**: `controller` = a real handler (checked by the completeness guard `everyControllerRouteHasAMatrixRow`, which fails the build on a missing or stale row); `filter` = handled by Spring Security filters, covered by the cited existing tests; `probe` = a path with no handler that proves a prefix rule or default deny.
- **CSRF**: every web POST row is also sent without a token by each principal and must get 403 (`everyWebPostWithoutACsrfTokenIsForbidden`). API POST rows pass without a token for allowed roles (D-03).
- **Existing coverage**: older `*SecurityTest` classes that already reference the path ("—" = covered only by this matrix).

| Method | Route | Allowed roles | Kind | Matrix test | Existing coverage |
|---|---|---|---|---|---|
| GET | `/` | public | controller | PermissionMatrixRouteTest | — |
| GET | `/home` | public | controller | PermissionMatrixRouteTest | — |
| GET | `/login` | public | controller | PermissionMatrixRouteTest | GoogleOAuthRouteSecurityTest, LoginRouteSecurityTest |
| GET | `/register` | public | controller | PermissionMatrixRouteTest | — |
| POST | `/register` | public | controller | PermissionMatrixRouteTest | — |
| GET | `/auth/phone-login` | public | controller | PermissionMatrixRouteTest | — |
| POST | `/auth/phone-login` | public | controller | PermissionMatrixRouteTest | — |
| GET | `/auth/verify-phone` | public | controller | PermissionMatrixRouteTest | — |
| POST | `/auth/verify-phone` | public | controller | PermissionMatrixRouteTest | — |
| POST | `/auth/verify-phone/resend` | public | controller | PermissionMatrixRouteTest | — |
| GET | `/auth/google/complete` | public | controller | PermissionMatrixRouteTest | — |
| POST | `/auth/google/complete` | public | controller | PermissionMatrixRouteTest | — |
| GET | `/tracking` | public | controller | PermissionMatrixRouteTest | IntakeRouteSecurityTest |
| GET | `/user/shipments` | all 6 roles | controller | PermissionMatrixRouteTest | LoginRouteSecurityTest, ShipmentRouteSecurityTest |
| GET | `/user/shipments/create` | all 6 roles | controller | PermissionMatrixRouteTest | LoginRouteSecurityTest, ShipmentRouteSecurityTest |
| GET | `/user/shipments/address-catalog` | all 6 roles | controller | PermissionMatrixRouteTest | — |
| POST | `/user/shipments/quote` | all 6 roles | controller | PermissionMatrixRouteTest | ShipmentRouteSecurityTest |
| POST | `/user/shipments` | all 6 roles | controller | PermissionMatrixRouteTest | LoginRouteSecurityTest, ShipmentRouteSecurityTest |
| GET | `/user/shipments/{shipmentId}` | all 6 roles | controller | PermissionMatrixRouteTest | LoginRouteSecurityTest, ShipmentRouteSecurityTest |
| POST | `/user/shipments/{shipmentId}/confirm-payment` | all 6 roles | controller | PermissionMatrixRouteTest | LoginRouteSecurityTest, ShipmentRouteSecurityTest |
| POST | `/user/shipments/{shipmentId}/cancel` | all 6 roles | controller | PermissionMatrixRouteTest | LoginRouteSecurityTest, ShipmentRouteSecurityTest |
| GET | `/user/incoming` | all 6 roles | controller | PermissionMatrixRouteTest | IntakeRouteSecurityTest |
| GET | `/user/incoming/{shipmentId}` | all 6 roles | controller | PermissionMatrixRouteTest | IntakeRouteSecurityTest |
| POST | `/user/incoming/{shipmentId}/unlock` | all 6 roles | controller | PermissionMatrixRouteTest | IntakeRouteSecurityTest |
| GET | `/user/shipment-fees/{shipmentId}` | all 6 roles | controller | PermissionMatrixRouteTest | ShipmentFeeControllerSecurityTest |
| POST | `/user/shipment-fees/{shipmentId}/wallet` | all 6 roles | controller | PermissionMatrixRouteTest | ShipmentFeeControllerSecurityTest |
| POST | `/user/shipment-fees/{shipmentId}/demo-vnpay` | all 6 roles | controller | PermissionMatrixRouteTest | ShipmentFeeControllerSecurityTest |
| GET | `/user/wallet` | all 6 roles | controller | PermissionMatrixRouteTest | WalletAndWithdrawalControllerSecurityTest |
| POST | `/user/wallet/withdraw` | all 6 roles | controller | PermissionMatrixRouteTest | WalletAndWithdrawalControllerSecurityTest |
| GET | `/admin/withdrawals` | ADMIN | controller | PermissionMatrixRouteTest | WalletAndWithdrawalControllerSecurityTest |
| POST | `/admin/withdrawals/{requestId}/approve` | ADMIN | controller | PermissionMatrixRouteTest | WalletAndWithdrawalControllerSecurityTest |
| POST | `/admin/withdrawals/{requestId}/reject` | ADMIN | controller | PermissionMatrixRouteTest | WalletAndWithdrawalControllerSecurityTest |
| GET | `/admin/overdue-returns` | ADMIN | controller | PermissionMatrixRouteTest | — |
| POST | `/admin/overdue-returns` | ADMIN | controller | PermissionMatrixRouteTest | — |
| GET | `/manager/operations` | ADMIN, MANAGER | controller | PermissionMatrixRouteTest | ManagerOperationsControllerSecurityTest |
| GET | `/manager/operations/work/{kind}/{id}` | ADMIN, MANAGER | controller | PermissionMatrixRouteTest | ManagerOperationsControllerSecurityTest |
| GET | `/manager/reconciliation` | ADMIN, MANAGER | controller | PermissionMatrixRouteTest | FinanceReconciliationControllerSecurityTest |
| GET | `/manager/reconciliation/detail` | ADMIN, MANAGER | controller | PermissionMatrixRouteTest | — |
| POST | `/manager/reconciliation/refund` | ADMIN, MANAGER | controller | PermissionMatrixRouteTest | FinanceReconciliationControllerSecurityTest |
| POST | `/manager/reconciliation/compensate` | ADMIN, MANAGER | controller | PermissionMatrixRouteTest | FinanceReconciliationControllerSecurityTest |
| POST | `/manager/reconciliation/submit` | ADMIN, MANAGER | controller | PermissionMatrixRouteTest | FinanceReconciliationControllerSecurityTest |
| POST | `/manager/reconciliation/payout` | ADMIN, MANAGER | controller | PermissionMatrixRouteTest | — |
| GET | `/manager/incidents/missing` | ADMIN, MANAGER | controller | PermissionMatrixRouteTest | MissingParcelControllerSecurityTest |
| GET | `/manager/incidents/missing/{incidentId}` | ADMIN, MANAGER | controller | PermissionMatrixRouteTest | MissingParcelControllerSecurityTest |
| POST | `/manager/incidents/missing/resolve` | ADMIN, MANAGER | controller | PermissionMatrixRouteTest | MissingParcelControllerSecurityTest |
| GET | `/manager/audit/shipments/{shipmentId}` | ADMIN, MANAGER | controller | PermissionMatrixRouteTest | OperationalAuditControllerSecurityTest |
| GET | `/manager/overdue-returns` | ADMIN, MANAGER | controller | PermissionMatrixRouteTest | — |
| POST | `/manager/overdue-returns` | ADMIN, MANAGER | controller | PermissionMatrixRouteTest | — |
| GET | `/hub/sorter` | ADMIN, HUB_STAFF | controller | PermissionMatrixRouteTest | HubRoutingControllerSecurityTest |
| POST | `/hub/inbound` | ADMIN, HUB_STAFF | controller | PermissionMatrixRouteTest | DestinationReceiptControllerSecurityTest, HubRoutingControllerSecurityTest, LastMileReturnControllerSecurityTest |
| POST | `/hub/sorter` | ADMIN, HUB_STAFF | controller | PermissionMatrixRouteTest | HubRoutingControllerSecurityTest |
| GET | `/hub/pickups` | ADMIN, HUB_STAFF | controller | PermissionMatrixRouteTest | IntakeRouteSecurityTest |
| POST | `/hub/pickups/{shipmentId}/assign` | ADMIN, HUB_STAFF | controller | PermissionMatrixRouteTest | IntakeRouteSecurityTest |
| POST | `/hub/pickups/{shipmentId}/service-failure` | ADMIN, HUB_STAFF | controller | PermissionMatrixRouteTest | IntakeRouteSecurityTest |
| GET | `/hub/inbound` | ADMIN, HUB_STAFF | controller | PermissionMatrixRouteTest | DestinationReceiptControllerSecurityTest, HubRoutingControllerSecurityTest, LastMileReturnControllerSecurityTest |
| POST | `/hub/inbound/receive` | ADMIN, HUB_STAFF | controller | PermissionMatrixRouteTest | DestinationReceiptControllerSecurityTest |
| POST | `/hub/inbound/reconcile` | ADMIN, HUB_STAFF | controller | PermissionMatrixRouteTest | DestinationReceiptControllerSecurityTest |
| POST | `/hub/inbound/reconcile-empty` | ADMIN, HUB_STAFF | controller | PermissionMatrixRouteTest | DestinationReceiptControllerSecurityTest |
| GET | `/hub/delivery-assignments` | ADMIN, HUB_STAFF | controller | PermissionMatrixRouteTest | LastMileAssignmentControllerSecurityTest |
| POST | `/hub/delivery-assignments` | ADMIN, HUB_STAFF | controller | PermissionMatrixRouteTest | LastMileAssignmentControllerSecurityTest |
| POST | `/hub/delivery-returns/receive` | ADMIN, HUB_STAFF | controller | PermissionMatrixRouteTest | LastMileReturnControllerSecurityTest |
| GET | `/hub/return-collection` | ADMIN, HUB_STAFF | controller | PermissionMatrixRouteTest | LastMileReturnControllerSecurityTest, ShipmentFeeControllerSecurityTest |
| POST | `/hub/return-collection` | ADMIN, HUB_STAFF | controller | PermissionMatrixRouteTest | LastMileReturnControllerSecurityTest, ShipmentFeeControllerSecurityTest |
| POST | `/hub/return-collection/cash` | ADMIN, HUB_STAFF | controller | PermissionMatrixRouteTest | ShipmentFeeControllerSecurityTest |
| GET | `/hub/outbound` | ADMIN, HUB_STAFF | controller | PermissionMatrixRouteTest | HandoverControllerSecurityTest |
| POST | `/hub/outbound/containers` | ADMIN, HUB_STAFF | controller | PermissionMatrixRouteTest | — |
| POST | `/hub/outbound/load` | ADMIN, HUB_STAFF | controller | PermissionMatrixRouteTest | HandoverControllerSecurityTest |
| POST | `/hub/outbound/handovers` | ADMIN, HUB_STAFF | controller | PermissionMatrixRouteTest | HandoverControllerSecurityTest |
| POST | `/hub/outbound/handovers/correct` | ADMIN, HUB_STAFF | controller | PermissionMatrixRouteTest | HandoverControllerSecurityTest |
| GET | `/hub/audit/shipments/{shipmentId}` | ADMIN, HUB_STAFF | controller | PermissionMatrixRouteTest | OperationalAuditControllerSecurityTest |
| GET | `/driver` | ADMIN, LINEHAUL_DRIVER | controller | PermissionMatrixRouteTest | HandoverControllerSecurityTest, LinehaulTripRouteSecurityTest |
| POST | `/driver/handovers/accept` | ADMIN, LINEHAUL_DRIVER | controller | PermissionMatrixRouteTest | LinehaulTripRouteSecurityTest |
| POST | `/driver/handovers/scan` | ADMIN, LINEHAUL_DRIVER | controller | PermissionMatrixRouteTest | HandoverControllerSecurityTest |
| POST | `/driver/trips/start` | ADMIN, LINEHAUL_DRIVER | controller | PermissionMatrixRouteTest | LinehaulTripRouteSecurityTest |
| POST | `/driver/trips/arrive` | ADMIN, LINEHAUL_DRIVER | controller | PermissionMatrixRouteTest | — |
| POST | `/driver/trips/delay` | ADMIN, LINEHAUL_DRIVER | controller | PermissionMatrixRouteTest | — |
| GET | `/driver/audit/shipments/{shipmentId}` | ADMIN, LINEHAUL_DRIVER | controller | PermissionMatrixRouteTest | — |
| GET | `/shipper/tasks` | ADMIN, SHIPPER | controller | PermissionMatrixRouteTest | IntakeRouteSecurityTest |
| POST | `/shipper/tasks/{assignmentId}/attempt` | ADMIN, SHIPPER | controller | PermissionMatrixRouteTest | IntakeRouteSecurityTest |
| POST | `/shipper/tasks/{assignmentId}/complete` | ADMIN, SHIPPER | controller | PermissionMatrixRouteTest | IntakeRouteSecurityTest |
| GET | `/shipper/delivery-tasks` | ADMIN, SHIPPER | controller | PermissionMatrixRouteTest | LastMileDeliveryControllerSecurityTest, LastMileReturnControllerSecurityTest |
| POST | `/shipper/delivery-tasks/scan` | ADMIN, SHIPPER | controller | PermissionMatrixRouteTest | LastMileDeliveryControllerSecurityTest |
| POST | `/shipper/delivery-tasks/collect` | ADMIN, SHIPPER | controller | PermissionMatrixRouteTest | LastMileDeliveryControllerSecurityTest |
| POST | `/shipper/delivery-tasks/confirm` | ADMIN, SHIPPER | controller | PermissionMatrixRouteTest | LastMileDeliveryControllerSecurityTest |
| POST | `/shipper/delivery-tasks/failure` | ADMIN, SHIPPER | controller | PermissionMatrixRouteTest | LastMileReturnControllerSecurityTest |
| GET | `/shipper/return-tasks` | ADMIN, SHIPPER | controller | PermissionMatrixRouteTest | LastMileReturnControllerSecurityTest |
| POST | `/shipper/return-tasks/scan` | ADMIN, SHIPPER | controller | PermissionMatrixRouteTest | LastMileReturnControllerSecurityTest |
| POST | `/shipper/return-tasks/confirm` | ADMIN, SHIPPER | controller | PermissionMatrixRouteTest | LastMileReturnControllerSecurityTest |
| POST | `/shipper/return-tasks/cash` | ADMIN, SHIPPER | controller | PermissionMatrixRouteTest | — |
| POST | `/login` | public | filter | n/a (filter-handled) | GoogleOAuthRouteSecurityTest, LoginRouteSecurityTest |
| POST | `/logout` | public | filter | n/a (filter-handled) | LoginRouteSecurityTest |
| GET | `/ws` | all 6 roles | filter | n/a (filter-handled) | — |
| GET | `/oauth2/authorization/google` | public | filter | n/a (filter-handled) | GoogleOAuthRouteSecurityTest |
| GET | `/login/oauth2/code/google` | public | filter | n/a (filter-handled) | GoogleOAuthRouteSecurityTest |
| GET | `/internal/unlisted` | all 6 roles | probe | PermissionMatrixRouteTest | — |
| POST | `/internal/unlisted` | all 6 roles | probe | PermissionMatrixRouteTest | — |
| POST | `/api/driver/trips/{tripId}/delay` | ADMIN, LINEHAUL_DRIVER | controller | PermissionMatrixApiChainTest | LinehaulTripRouteSecurityTest, OperationalIncidentApiSecurityTest |
| POST | `/api/auth/login` | public | controller | PermissionMatrixApiChainTest | — |
| GET | `/api/security/actor` | all 6 roles | controller | PermissionMatrixApiChainTest | — |
| GET | `/api/tracking/{trackingNumber}` | public | controller | PermissionMatrixApiChainTest | IntakeRouteSecurityTest |
| GET | `/api/shipper/probe` | ADMIN, SHIPPER | probe | PermissionMatrixApiChainTest | — |
| GET | `/api/hub/probe` | ADMIN, HUB_STAFF | probe | PermissionMatrixApiChainTest | — |
| GET | `/api/calculator/probe` | public | probe | PermissionMatrixApiChainTest | — |
| GET | `/api/unlisted` | all 6 roles | probe | PermissionMatrixApiChainTest | — |

## Findings

| ID | Severity | Surface | Description | Disposition | Evidence |
|---|---|---|---|---|---|
| F-06-R-01 | MEDIUM | `/uploads/**` (resource handler, not a controller route) | Delivery evidence files are served to any authenticated principal, and `FileSystemStorageServiceImpl` accepts any extension (for example `.html` or `.svg`), so a shipper could store active content served from the application origin. No current page links to an upload; file names carry an 8-hex random part. Same-origin content plus any-authenticated read is a defense-in-depth gap, not a demonstrated IDOR. | RECORDED (owner chose to fix later) | `WebConfig.addResourceHandlers`, `FileSystemStorageServiceImpl.buildStorageFilename` |
| F-06-R-02 | LOW | `formLogin` | Every role lands on `/user/shipments/create` after login (`defaultSuccessUrl(..., true)`), including staff roles. No data exposure. | RECORDED | `SecurityConfig.webSecurityFilterChain` |
| F-06-R-03 | LOW | `/api/calculator/**` | Permitted publicly but no handler exists; dead rule. | RECORDED | `PermissionMatrixApiChainTest` probe row |
| F-06-R-04 | LOW | API JWT filter | `JwtAuthenticationFilter` checks `isEnabled()` but not `isAccountNonLocked()`, unlike the session sign-in path. | RECORDED | `JwtAuthenticationFilter.doFilterInternal`, `AccountSessionAuthenticator.signIn` |

No observed matrix cell differed from the documented policy, so no CRITICAL or HIGH route-level finding exists and `SecurityConfig.java` was not changed.

## Observations

- `@EnableMethodSecurity` is declared on `SecurityConfig`, but no `@PreAuthorize` annotation exists in `src/main/java`. Per D-02 this is verified, not migrated: route prefixes plus service-layer contextual checks (ADR-015) carry authorization.
- `/user/**` admits every authenticated role. Ownership (sender, receiver, wallet owner) is decided in the service layer; plan 06-02 records those cells.
- The web chain keeps CSRF and the `/api/**` chain keeps it disabled (D-03), both shown executably above.
- An unlisted path (`/internal/unlisted`) requires authentication only: anonymous callers are redirected, every role passes the chain. Default deny holds for anonymous callers; role restriction exists only on the listed prefixes.
