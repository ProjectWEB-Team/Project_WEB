# Phase 06 — STOMP matrix (HARD-01 + HARD-02, plan 06-03)

Source branch: `feature/phase-06-hardening`. D-04 (multi-session in Maven), D-07 (no WebSocket client, broker or browser framework): sessions are distinct principals and the per-user `SimpMessagingTemplate.convertAndSendToUser` call is the delivery boundary.

Short names: **WSC** = `WebSocketConfigTest`, **SMS** = `StompMultiSessionDeliveryTest`, **WSE** = `WebSocketEndpointSecurityTest`.

## Handshake and interceptor matrix

| Command | Destination class | Principal | Expected | Evidence |
|---|---|---|---|---|
| HTTP handshake `/ws` | — | anonymous | 302 to `/login` | WSE#unauthenticatedWebSocketHandshakeIsRedirectedToLogin |
| HTTP handshake `/ws` | — | authenticated | reaches the endpoint | WSE#authenticatedHandshakeReachesTheWebSocketEndpoint |
| CONNECT | — | anonymous | denied | WSC#rejectsUnauthenticatedStompConnections |
| CONNECT | — | authenticated | allowed | WSC#authenticatedConnectIsAllowed |
| SUBSCRIBE | `/user/queue/shipments`, `/user/queue/operations`, `/user/queue/notifications` | authenticated | allowed | WSC#eachPrivateUserQueueIsSubscribableOnlyWhenAuthenticated (3 cases), SMS#everyPublisherDestinationIsSubscribableOnlyThroughTheUserPrefix |
| SUBSCRIBE | same three | anonymous | denied | WSC#eachPrivateUserQueueIsSubscribableOnlyWhenAuthenticated |
| SUBSCRIBE | raw broker queues `/queue/shipments`, `/queue/notifications`, `/queue/operations` | authenticated | denied | WSC#authenticatedSubscriptionOutsideTheAllowListIsDenied, SMS#everyPublisherDestinationIsSubscribableOnlyThroughTheUserPrefix |
| SUBSCRIBE | another user's queue `/user/other-user/queue/shipments` | authenticated | denied | WSC#authenticatedSubscriptionOutsideTheAllowListIsDenied, SMS#bothSessionsMaySubscribeToTheirOwnUserQueueButNotToARawOrForeignQueue |
| SUBSCRIBE | `/topic/*`, `/app/*`, `/user/queue/other`, suffixed `/user/queue/shipments/extra` | authenticated | denied | WSC#authenticatedSubscriptionOutsideTheAllowListIsDenied |
| SUBSCRIBE | missing destination | authenticated | denied | WSC#subscriptionWithoutADestinationIsDenied |
| SEND | `/app/shipments`, `/user/queue/shipments`, `/queue/notifications` | authenticated and anonymous | denied | WSC#everyClientSendIsDeniedWithOrWithoutAuthentication, WSC#deniesClientInitiatedStompSendsInTheWalkingSkeleton |
| DISCONNECT, UNSUBSCRIBE | — | any | pass through (no data) | WSC#disconnectAndUnsubscribePassThroughWithoutData |

Wiring: exactly one client-inbound interceptor of type `AuthenticatedShipmentStompInterceptor` (WSC#theInterceptorIsTheOnlyClientInboundInterceptor); only `/ws` is registered with no allowed-origin call, so the same-origin default holds (WSC#onlyTheSameOriginWsEndpointIsRegistered); broker uses only `/queue`, `/app`, `/user` (WSC#brokerUsesOnlyQueueAppAndUserPrefixes).

## Delivery matrix (publisher × recipient × transaction outcome)

| Publisher / type | Intended recipient | Excluded | Before commit | Rollback | After commit | Payload | Evidence |
|---|---|---|---|---|---|---|---|
| `StompShipmentEventPublisher` | owner | stranger session | nothing | nothing | once to owner | `trackingNumber, currentStatus, eventType, occurredAt` | SMS#shipmentEventReachesOnlyTheOwnerAndOnlyAfterCommit, #shipmentEventIsNeverSentOnRollback, #shipmentPayloadCarriesOnlyTheFourPublicComponents |
| `StompNotificationEventPublisher` | each named user | the other user's payload | nothing | nothing (same synchronization pattern) | once per recipient | `eventType, reference, message` (phone, amount, status dropped) | SMS#twoNotificationRecipientsEachReceiveOnlyTheirOwnPayload, #payloadsSerializeToTheMinimalFieldSets, #blankNotificationRecipientIsRejectedBeforeAnySynchronization |
| HANDOVER_ACCEPTED | OUTBOUND/GENERAL_STAFF of the hub + receiving driver | other hub's staff, locked account | — | — | once each | `type, reference` | SMS#handoverAcceptedReachesOnlyOutboundStaffOfThatHubAndTheReceivingDriver |
| DESTINATION_ARRIVAL | INBOUND/GENERAL_STAFF of the destination + trip driver | other hub's staff | nothing (real Spring listener) | nothing | once each | `type, reference` | SMS#destinationArrivalAndInboundReadyReachOnlyInboundStaffOfTheDestinationAndTheTripDriver, #destinationArrivalThroughTheSpringListenerWaitsForCommitAndIsSilentOnRollback |
| INBOUND_READY_CONTAINER | same as DESTINATION_ARRIVAL | other hub's staff | — | — | once each | `type, reference` | SMS#destinationArrivalAndInboundReadyReachOnlyInboundStaffOfTheDestinationAndTheTripDriver |
| NEW_INCIDENT | origin/destination supervisors, destination inbound staff, assigned driver | unrelated managers, locked, wrong-role driver | nothing | nothing | once each | `type, reference` | MissingParcelRealtimePublisherTest (existing, not duplicated) |
| BLOCKED_ROUTE | supervisors of the hub subtree | others | — | — | once each | `type, reference` | OperationalRealtimePublisherTest (existing, not duplicated) |

Principal addressing: `UsernamePasswordAuthenticationToken(User).getName()` equals `User.getUsername()`, the key every publisher passes (SMS#theSessionPrincipalNameIsTheUsernamePublishersAddress).

## Automated versus browser-only

`05-UAT.md` item 3 bundles several claims. This plan proves the **technical** STOMP parts in Maven: right recipient, unrelated sessions excluded, minimal private payload, nothing emitted on rollback, subscription allow-list. It does **not** prove, and `05-UAT.md` is not edited to claim:

- that a real browser holding two logged-in sessions shows the toast/list update on the right screen only;
- the JSP pages, forms and CSRF behaviour observed in a browser;
- the populated manager/finance/return screens being usable.

Those remain NOT_RUN in the manual checklist of `06-UAT-RESULTS.md` (plan 06-07) and in TV3's Phase 05.1 guide.

## Findings

| ID | Severity | Surface | Description | Disposition | Evidence |
|---|---|---|---|---|---|
| F-06-S-01 | LOW | Interceptor | DISCONNECT and UNSUBSCRIBE frames pass through without a principal check. They carry no data and cannot open a subscription. | RECORDED | WSC#disconnectAndUnsubscribePassThroughWithoutData |
| F-06-S-02 | LOW | `OperationalRealtimePublisher` | HANDOVER_ACCEPTED / DESTINATION_ARRIVAL add the persisted handover/trip driver without re-checking `ROLE_LINEHAUL_DRIVER` (NEW_INCIDENT does). The driver was assigned by role-checked services and inactive users are still removed. | RECORDED | `OperationalRealtimePublisher.recipients` |

No CRITICAL or HIGH STOMP finding; `WebSocketConfig.java` and the publishers were not changed.
