# Crosshair
### Assisted targeting with an ultramicrotome

## Installation
Crosshair is a Fiji plugin for targeted ultramicrotomy via 3D image data (e.g. micro-CT data).  

### Install imageJ, or update your existing version
If you haven't used Fiji/ImageJ before - you can download it from [the ImageJ website](https://imagej.net/Fiji).  
If you're using an existing Fiji installation, make sure it is up to date!  
Go to `Help > Update...` in the imagej menu, and select `Apply Changes`.  

### Add the Crosshair update site
Go to `Help > Update...` in the imagej menu, and select `Manage update sites`.  
In the new window that pops up, select `Add Unlisted Site`. This will create a new row in the table.  
Fill this row out so it looks like below:  

| Active | Name          | URL           | Host   | Directory on Host | Description |
| -------| ------------- | ------------- | ------ | ------            | ------      |
| &check;| `Crosshair`   | `https://sites.imagej.net/Crosshair/` | | |

Then click `Apply and Close`, followed by `Apply Changes`. 

## User guide

A full user guide is provided in [the Crosshair wiki](https://github.com/automated-ultramicrotomy/crosshair/wiki).

## Cite

If you use Crosshair in your work, please cite our [paper on eLife](https://elifesciences.org/articles/80899)

## Reporting issues

If you find a problem with Crosshair, please let us know!  
Go to the [issue tracker](https://github.com/automated-ultramicrotomy/crosshair/issues) and click the green `New issue` button at the top right. Write a short title and description for your issue, along with information about what type of computer you were using (Windows / mac etc.). Then click the `Submit new issue` button. 

## Developer docs

Some brief information about [how to release new versions](RELEASES.md) and [keep dependencies up to date](RENOVATE.md) is also provided in the repository.
