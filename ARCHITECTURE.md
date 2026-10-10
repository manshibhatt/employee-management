# Employee and leave management

The existing layered Spring Boot layout is retained: REST controllers call services, services work through repositories, and DTOs keep persistence relationships out of employee API responses.

## Domain relationships

```text
Department 1 ── * Employee 1 ── 1 EmployeeInfo
                            └── * LeaveRequest * ── 1 LeaveType
User 0..1 ── 0..1 Employee
```

`EmployeeInfo` contains the employee's job title, phone, address, date of birth, hire date, and emergency contact. New employees receive an empty info row; opening a legacy employee profile creates its missing info row. Leave requests belong to an employee and one seeded leave type. The application seeds Annual, Sick, and Personal leave types when the leave type table is empty.

## Roles

There are two roles:

- `ROLE_USER`: can see the employee record linked to their login, view their employee info, and submit or track their own leave requests.
- `ROLE_LEAVE_APPROVER`: can manage departments and employees, link user logins to employee records, view employee details and all leave requests, and approve or reject pending requests.

Public registration always creates `ROLE_USER`; a request body cannot grant approver access. Provision the approver role through the database, using your own username:

```sql
UPDATE users SET role = 'ROLE_LEAVE_APPROVER' WHERE username = 'your-approver-username';
```

For an existing database, migrate the old administrator role if needed:

```sql
UPDATE users SET role = 'ROLE_LEAVE_APPROVER' WHERE role = 'ROLE_ADMIN';
```

Restart Spring Boot after changing roles. Employee accounts are linked by the approver from **People → Add/Edit employee → Employee login**. A login can be linked to one employee only. Without this link, that user cannot submit leave requests.

## Main API routes

- `GET /api/employees/mine` — current user's linked employee.
- `GET|PUT /api/employees/{id}/info` — view profile (owner or approver), edit profile (approver).
- `GET /api/leaves/types` — leave types.
- `GET /api/leaves` — own requests for a user; all requests for an approver. Approvers can filter with `?employeeId=...`.
- `POST /api/leaves` — submit a request for the authenticated user's linked employee.
- `PATCH /api/leaves/{id}/decision` — approver approves or rejects a pending request.

Hibernate's existing `ddl-auto=update` setting creates the new tables and columns on application startup. Keep database backups before deploying schema changes to a database with important data.
