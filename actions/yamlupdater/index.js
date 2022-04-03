const core = require('@actions/core');
// const github = require('@actions/github');
const yaml = require('js-yaml');

/*

release:
- version: 2.0.0
  major: 2
  minor: 0
  patch: 0
  timestamp:
prerelease:

 */

try {
    // Get the YAML file we've been passed by GH Actions
    let doc = yaml.safeLoad(core.getInput("inputyml"));
    const version = core.getInput("version");
    const semver = version.split("-", 2)[0];
    const versplit = semver.split(".");
    let isPreRelease = version.includes("BETA") || version.includes("ALPHA") || version.includes("RC") || version.includes("SNAPSHOT");
    let newVer = {
        version: version,
        major: versplit[0],
        minor: versplit[1],
        patch: versplit[2],
        timestamp: Date.now()
    }
    if (isPreRelease) {
        doc.release.push(newVer)
    } else {
        doc.prerelease.push(newVer)
    }

    core.setOutput('outputyml', yaml.safeDump(doc))
} catch (e) {
    core.setFailed(e.message);
}