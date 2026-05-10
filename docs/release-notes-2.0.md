# RamEssentials 2.0 Release Notes

RamEssentials 2.0 targets RamCore 2.0.0 and Paper 26.1.2.

Breaking storage changes:

- `config.conf` is replaced by `config.yml`.
- `accounts.conf` is replaced by one JSON file per account.
- `warps.conf` is replaced by one JSON file per warp.
- `playerdata/*.conf` is replaced by JSON player data files.

Migration:

- Legacy data is migrated automatically once.
- A full backup is written to `legacy-backup-<timestamp>`.
- Old files are archived as `.legacy`.
- `migration-2.0.done` prevents repeated migration.

New or expanded features:

- RamCore 2.0 command module registration.
- RamCore typed config and file repositories.
- Dirty-only JSON persistence.
- Permission-based home limits.
- Configurable command cooldowns and teleport warmups.
- Safe teleport target search.
- Back stack support.
- Warp metadata: creator, creation time, permission, category, and icon.
- `/warps`, `/warp info`, `/warp setpermission`, `/warp category`, `/warp icon`.
- `/home rename`.
- `/tpahere`, request expiry, and clickable accept/deny actions.
- `/eco give`, `/eco take`, `/eco set`, `/eco reset`, `/eco top`.
- `/balancetop` pagination.
- Date-based economy transaction logs.

Operational checks after upgrade:

- Start the server once and inspect console output.
- Run `/re migrate status`.
- Check `/re diagnostics`.
- Verify homes, warps, spawn, back, TPA, and Vault economy behavior.
