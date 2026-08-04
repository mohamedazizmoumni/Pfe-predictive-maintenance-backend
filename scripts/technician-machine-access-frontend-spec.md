# Backend change: Technician machine-access restriction

## Summary

Technicians can no longer see every machine. A MANAGER, ADMIN, or SUPER_ADMIN must explicitly assign a machine to a technician before that technician can see it. MANAGER/ADMIN/SUPER_ADMIN accounts are unaffected — they still see everything.

---

## 1. Existing endpoints — behavior change (no shape/path change)

### `GET /api/v1/machines`
- **MANAGER / ADMIN / SUPER_ADMIN**: returns all machines, unchanged.
- **TECHNICIAN-only accounts** (no MANAGER/ADMIN/SUPER_ADMIN role): now returns **only the machines assigned to them**. Could be an empty array if nothing has been assigned yet.
- Any other role (FINANCE_MANAGER, STOCK_MANAGER, DATA_SCIENTIST): unrestricted, unchanged.
- Response shape (`MachineDTO[]`) is unchanged.

### `GET /api/v1/machines/{id}`
- Same role rules as above.
- **New possible response for technicians**: `403 Forbidden` if the machine exists but isn't assigned to that technician. Body:
```json
{
  "timestamp": "2026-07-03T10:15:00Z",
  "status": 403,
  "error": "Forbidden",
  "message": "You are not assigned to this machine",
  "path": "/api/v1/machines/42"
}
```
  Frontend should treat this distinctly from a `404` — the machine exists, the user just isn't authorized to view it. Show something like "You don't have access to this machine" rather than a generic not-found page.

---

## 2. New endpoints — machine ↔ technician assignment (MANAGER / ADMIN / SUPER_ADMIN only)

Any of these called by a plain TECHNICIAN (or unauthenticated) returns `403 Forbidden`.

### Assign a technician to a machine
```
POST /api/v1/machines/{machineId}/technicians
Content-Type: application/json

{ "technicianId": 5 }
```
**200 OK** response body (`MachineTechnicianDTO`):
```json
{
  "id": 1,
  "machineId": 12,
  "technicianId": 5,
  "technicianUsername": "jdoe",
  "technicianDisplayName": "John Doe",
  "assignedById": 2,
  "assignedByUsername": "manager1",
  "assignedAt": "2026-07-03T10:15:00"
}
```
Error cases:
- `404 Not Found` — `machineId` or `technicianId` doesn't exist.
- `400 Bad Request` — target user doesn't have the TECHNICIAN role, or is already assigned to this machine. Message explains which.

### Unassign a technician from a machine
```
DELETE /api/v1/machines/{machineId}/technicians/{technicianId}
```
- **204 No Content** on success.
- **400 Bad Request** if that technician wasn't assigned to that machine.

### List technicians assigned to a machine
```
GET /api/v1/machines/{machineId}/technicians
```
**200 OK** → `MachineTechnicianDTO[]` (same shape as above; empty array if none assigned).
- `404 Not Found` if the machine doesn't exist.

---

## 3. Picking a technician to assign (no new endpoint — reuse existing)

To populate a "choose a technician" dropdown for the assignment UI, use the existing:
```
GET /api/v1/users?role=TECHNICIAN
```
This already exists and returns `UserDTO[]` filtered to users holding the TECHNICIAN role.

---

## 4. Not changed (out of scope for now)

The real-time WebSocket telemetry stream (`/topic/machines`, `/topic/alerts`) still broadcasts **every** machine's data to every connected client regardless of role — it is **not** filtered by assignment yet. If a technician's frontend relies on the WebSocket feed for the machine list/telemetry (not just the REST endpoints above), it will still show all machines there until a follow-up change adds per-user filtering to the socket. For now, treat the REST endpoints above as the source of truth for "which machines can this technician see."

---

## 5. Quick reference table

| Method | Path | Who | Purpose |
|---|---|---|---|
| GET | `/api/v1/machines` | any authenticated | list (filtered for technicians) |
| GET | `/api/v1/machines/{id}` | any authenticated | detail (403 for unassigned technicians) |
| POST | `/api/v1/machines/{machineId}/technicians` | MANAGER/ADMIN/SUPER_ADMIN | assign technician |
| DELETE | `/api/v1/machines/{machineId}/technicians/{technicianId}` | MANAGER/ADMIN/SUPER_ADMIN | unassign technician |
| GET | `/api/v1/machines/{machineId}/technicians` | MANAGER/ADMIN/SUPER_ADMIN | list assigned technicians |
| GET | `/api/v1/users?role=TECHNICIAN` | any authenticated | list technicians (for picker) — pre-existing |
