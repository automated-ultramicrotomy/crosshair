# Guidelines for making new releases (on fiji update site + github)

## Testing inside IntelliJ

- Run through the Crosshair workflow, using `src/test/java/develop/openexamples/OpenExampleBlock`. This will open a downsampled micro-CT volume that is included inside the project.

- When you're happy with the changes, increase the version number inside `pom.xml`.

## Testing inside Fiji

- Download a fresh Fiji and fully update it.

- Make sure the Crosshair update site has been added with your ImageJ username in the host column, as [specified in the Fiji docs](https://imagej.net/update-sites/setup#start-the-updater-and-check-your-update-site).

- Then run `mvn clean install "-Dscijava.app.directory=/path/to/Fiji.app"` to place the relevant `Crosshair` jars into that Fiji under `/jars`.

- Test the Crosshair workflow briefly. You can use `Target Bdv File` and navigate to the test block `.xml` under `src/test/resources/exampleBlock.xml`.

## Uploading to update site

- Go to `Help > Update`. Right click on any changes highlighted here and select `keep-as-is`. 

- Click on `Advanced Mode`, then select `View locally modified files only` from the `View Options` dropdown.

- Right click on `crosshair` and `imagej-utils` and select `Upload to Crosshair`. Any other changes can be ignored - they should come with Fiji by default.

- Click `Apply Changes (Upload)` in the bottom right. Read any of the complaints that appear and either upload those jars, or break the dependency if you're sure it isn't required (for example, if it's provided on another update site that users can enable).

- After upload, you can check the results in another Fiji that has the `Crosshair` update site enabled.

## Making a release on github

- Once all the relevant changes are on `master`, go the the `releases` tab on github.

- Click `Draft a new release`, then under `choose a tag` type in the same version you put in the `pom.xml`. Also set the release title as this version.

- Click `generate release notes` and edit the results to give an overall summary of changes.

- Then select `Publish Release`.