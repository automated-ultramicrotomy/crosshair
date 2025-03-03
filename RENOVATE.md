# Guidelines for keeping dependencies up to date

## Renovate

This repository uses [Renovate](https://github.com/renovatebot/renovate) to automatically detect updates to Crosshair's dependencies. Renovate will use the settings from `renovate.json` to detect updates and automatically open PRs which can then be reviewed / edited / merged.

## Dependencies

The main dependency Renovate watches is `pom-scijava` - the parent pom which should list all of the dependencies / versions released with the latest Fiji. Note that sometimes there can be differences between the dependencies stated here and those shipped with Fiji - see the [responses to this forum post](https://forum.image.sc/t/how-to-find-out-the-pom-scijava-version-of-the-currently-used-fiji-version/45889/3?u=kimberly_meechan).

If you want to check which `pom-scijava` is being used from inside a copy of Fiji, type the following into the Fiji search bar and press enter:
```
!ui.showDialog(app.getApp("Fiji").getPOM().getParentVersion()) 
```

