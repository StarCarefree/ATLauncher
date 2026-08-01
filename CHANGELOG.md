# Changelog

This changelog only contains the changes that are unreleased. For changes for individual releases, please visit the
[releases](https://github.com/ATLauncher/ATLauncher/releases) page on GitHub.

## 3.4.41.2

### New Features

- Check every mod in an instance for updates in one pass, and update them all from a single list
- Search and filter the mod list by name, type, platform and whether an update is waiting
- Show each mod's type, version and platform in the mod list, and badge the ones with an update
- Install a mod's required dependencies, and theirs in turn, with one button
- Mod browser cards now show the author, download count and category, and match the rest of the launcher
- Mod browser searches as you type, and says which page of how many results you are on

### Fixes

- Checking for updates no longer opens a pair of dialogs per mod, or claim to have checked when cancelled
- Removing a mod from its right click menu now asks first, as removing several already did
- Refreshing metadata for a disabled mod looked up the wrong path and matched nothing
- A mod dropped onto the disabled column was never matched on CurseForge
- Fabric mods are offered for updates on a Forge instance running Sinytra Connector, as they are when browsing

### Misc

- The mod fingerprinting code existed in four copies; it is now one
- The mod compatibility rules existed in three copies that had drifted apart; they are now one
