package io.wdsj.asw.common.template;

public class PluginVersionTemplate {
    public static final String VERSION = "${project.version}";
    public static final String VERSION_CHANNEL = "${version.channel}";
    public static final String COMMIT_HASH_SHORT = "${git.commit.id.abbrev}";
    public static final String COMMIT_HASH = "${git.commit.id}";
    public static final String COMMIT_BRANCH = "${git.branch}";
}
