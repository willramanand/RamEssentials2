# RamEssentials Configuration

RamEssentials 2.0 uses RamCore typed YAML config at `plugins/RamEssentials2/config.yml`.

Important sections:

- `homes.default-limit` - default number of homes per player.
- `homes.permission-limits.<key>` - permission-based limits using `ramessentials.homes.limit.<key>`.
- `cooldowns.<command>` - cooldowns in seconds for commands like `home`, `spawn`, `warp`, `tpa`, `tpahere`, `back`, and `heal`.
- `teleport.warmup-seconds` - teleport warmup before movement.
- `teleport.safe-location-search` - adjusts target locations to avoid unsafe blocks.
- `teleport.load-target-chunk` - whether safe teleport search may load target chunks.
- `teleport.request-timeout-seconds` - `/tpa` and `/tpahere` expiry.
- `teleport.back-stack-size` - number of previous locations retained by `/back`.
- `economy.percent-lost-on-death` - percentage lost on death.
- `economy.transaction-logging` - writes economy audit logs to `transactions/yyyy-MM-dd.log`.
- `messages.*` - MiniMessage strings with placeholders.

Storage:

- Accounts: `accounts/<uuid>.json`
- Player data: `playerdata/<uuid>.json`
- Warps: `warps/<name>.json`
- Economy logs: `transactions/yyyy-MM-dd.log`

Legacy migration:

On first 2.0 startup, the plugin migrates old `config.conf`, `accounts.conf`, `warps.conf`, and `playerdata/*.conf` files. It creates `migration-2.0.done`, archives migrated files as `.legacy`, and creates a full `legacy-backup-<timestamp>` directory before rewriting data.

Use `/re migrate status` to inspect what was migrated.
