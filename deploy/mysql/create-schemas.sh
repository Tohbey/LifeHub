#!/usr/bin/env bash
# Creates one schema per LifeHub service, and two users per service (README section 10, ADR 0003):
#   <DB_USER>            runtime user: data changes only, on its own schema only
#   <DB_MIGRATION_USER>  Liquibase user: schema changes, on its own schema only
# Names and passwords come from each deploy/env/<service>.env, so they match what the service uses.
# Safe to run again: existing schemas and users are kept, grants are reapplied, passwords are updated.
#
# Usage: MYSQL_ROOT_PASSWORD=... deploy/mysql/create-schemas.sh
#   MYSQL_CONTAINER   the MySQL container's name (default: mysql)
set -euo pipefail

MYSQL_CONTAINER="${MYSQL_CONTAINER:-mysql}"
ENV_DIR="$(cd "$(dirname "$0")/../env" && pwd)"
: "${MYSQL_ROOT_PASSWORD:?Set MYSQL_ROOT_PASSWORD}"

# service:schema:runtime grants
SERVICES=(
    "identity-workspace:lifehub_identity:SELECT, INSERT, UPDATE, DELETE"
    "notification:lifehub_notification:SELECT, INSERT, UPDATE, DELETE"
    "finance:lifehub_finance:SELECT, INSERT, UPDATE, DELETE"
    "planner:lifehub_planner:SELECT, INSERT, UPDATE, DELETE"
    "shopping-meals:lifehub_shopping:SELECT, INSERT, UPDATE, DELETE"
    # The audit log is append-only: its runtime user can never change or delete history.
    "audit:lifehub_audit:SELECT, INSERT"
)
MIGRATION_GRANTS="SELECT, INSERT, UPDATE, DELETE, CREATE, ALTER, DROP, INDEX, REFERENCES"

# Reads one KEY=value from an env file.
value() {
    grep -E "^$2=" "$1" | tail -1 | cut -d= -f2-
}

# Refuses values that would break out of a SQL string.
safe() {
    if [[ -z "$1" || "$1" == *"'"* || "$1" == *"\\"* ]]; then
        echo "Missing value, or it contains a quote or backslash: $2" >&2
        exit 1
    fi
    printf '%s' "$1"
}

sql=""
for entry in "${SERVICES[@]}"; do
    IFS=: read -r service schema grants <<<"$entry"
    env_file="$ENV_DIR/$service.env"
    [[ -f "$env_file" ]] || { echo "Missing $env_file (copy it from $service.env.example)" >&2; exit 1; }
    app_user=$(safe "$(value "$env_file" DB_USER)" "$service DB_USER")
    app_password=$(safe "$(value "$env_file" DB_PASSWORD)" "$service DB_PASSWORD")
    migration_user=$(safe "$(value "$env_file" DB_MIGRATION_USER)" "$service DB_MIGRATION_USER")
    migration_password=$(safe "$(value "$env_file" DB_MIGRATION_PASSWORD)" "$service DB_MIGRATION_PASSWORD")

    sql+="CREATE DATABASE IF NOT EXISTS \`$schema\` CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;
CREATE USER IF NOT EXISTS '$app_user'@'%' IDENTIFIED BY '$app_password';
ALTER USER '$app_user'@'%' IDENTIFIED BY '$app_password';
CREATE USER IF NOT EXISTS '$migration_user'@'%' IDENTIFIED BY '$migration_password';
ALTER USER '$migration_user'@'%' IDENTIFIED BY '$migration_password';
GRANT $grants ON \`$schema\`.* TO '$app_user'@'%';
GRANT $MIGRATION_GRANTS ON \`$schema\`.* TO '$migration_user'@'%';
"
done

printf '%s' "$sql" | docker exec -i -e MYSQL_PWD="$MYSQL_ROOT_PASSWORD" "$MYSQL_CONTAINER" mysql -uroot
echo "Schemas and users are ready."
