#!/usr/bin/env node

const fs = require('fs').promises;
const path = require('path');

module.exports = async function (context) {
    const PACKAGE_NAME_PLACEHOLDER = "<%PACKAGE_NAME%>";
    let packageName, activityTargetPath;

    try {
        const projectRoot = context.opts.projectRoot;
        const platformRoot = path.join(projectRoot, 'android');        
        const configPath = path.join(projectRoot, 'capacitor.config.json');
        const configData = await fs.readFile(configPath, 'utf8');
        const config = JSON.parse(configData);
        packageName = config.appId;
        
        const activitySourcePath = path.join(context.opts.plugin.pluginInfo.dir, 'src/android/MainActivity.java');
        activityTargetPath = path.join(platformRoot, 'app/src/main/java', packageName.replace(/\./g, '/'), 'MainActivity.java');
        
        let activitySrc = await fs.readFile(activitySourcePath, 'utf8');
        activitySrc = activitySrc.replace(PACKAGE_NAME_PLACEHOLDER, packageName);
        await fs.writeFile(activityTargetPath, activitySrc, 'utf8');
    } catch (err) {
        console.error('Error while configuring MainActivity.java:', err);
        throw err;
    }
};
