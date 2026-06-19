package io.wdsj.asw.common.template;

public class PluginVersionTemplate {
    public static final String VERSION = "${pluginVersion}";
    public static final String VERSION_CHANNEL = "${versionChannel}";
    public static final String COMMIT_HASH_SHORT = "${gitCommitShort}";
    public static final String COMMIT_HASH = "${gitCommitFull}";
    public static final String COMMIT_BRANCH = "${gitBranch}";
}
