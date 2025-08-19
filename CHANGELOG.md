# Changelog

## [0.1.0-m1] - 2025-08-19
### Added
- `map-cli` with `discover`, `validate`, `stats`.
- HA HTTP client (states/services) with retries.
- Manifest builder + inference (climate/light/fan/cover/media_player/number).
- Lockfile with stable IDs, service sigs, enums cache.
- GraalVM native build support (Windows).

### Fixed/Improved
- Service field typing v0 (percent/°C/K/enums/transition).
- Number-domain capability (value + min/max/step/unit).
- New CLI `--demo` and `--verbose`.
