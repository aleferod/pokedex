# Assumptions (as listed by the model after the code)

1. The JWT `sub` claim carries the username; tokens are issued by another service sharing the HS256 secret.
2. Users are provisioned elsewhere; the `users` table only needs `id` and `username` here.
3. "Today" for the due-date rule is evaluated in UTC, via the injected `Clock`.
4. `status` defaults to `TODO` when omitted on create; on `PUT` it is required because PUT replaces the task.
5. `description` is optional; an empty string is stored as given.
6. The list is ordered by `dueDate` ascending with nulls last, then `createdAt` ascending.
7. Deleting a user deletes their tasks (`ON DELETE CASCADE`).
