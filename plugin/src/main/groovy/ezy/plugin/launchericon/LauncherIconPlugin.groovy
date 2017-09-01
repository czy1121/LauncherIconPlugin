package ezy.plugin.launchericon

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.file.FileCollection

import java.text.SimpleDateFormat

class LauncherIconPlugin implements Plugin<Project> {

    public static class LauncherIconExtension {
        String branch = null
        String user = null
        boolean mipmap = true
        String ic_launcher = "ic_launcher.png"

        void useBranch(Project project) {
            def cmd = "git --git-dir=${project.rootDir}/.git --work-tree=${project.rootDir}/ rev-parse --abbrev-ref HEAD";
            branch = cmd.execute().text.trim()
        }
    }

    void apply(Project project) {
        project.extensions.create('launcherIcon', LauncherIconExtension)

        project.afterEvaluate {
            LauncherIconExtension config = project.launcherIcon

            def variants
            if (project.android.hasProperty('applicationVariants')) {
                variants = project.android.applicationVariants
            } else if (project.android.hasProperty('libraryVariants')) {
                variants = project.android.libraryVariants
            } else {
                throw new IllegalStateException('Android project must have applicationVariants or libraryVariants!')
            }

            def version = project.android.defaultConfig.versionName;
            def user = config.user ? config.user : (project.hasProperty("launcherIconUser") ? project.property("launcherIconUser") : null);
            def datetime = nowMMDDhh();
            variants.all { variant ->

                def build = variant.buildType.name;
                if (build == "release") {
                    return;
                }

                def flavor = variant.flavorName;
                def dst = project.file("$project.buildDir/generated/res/$flavor/$build/ic_launchers/")
                variant.sourceSets.get(variant.sourceSets.size() - 1).res.srcDirs += dst

                FileCollection files = project.files()
                variant.sourceSets.each { sourceSet ->
                    List<File> icons = new ArrayList<>();
                    for (File file : sourceSet.res.srcDirs) {
                        searchIcons(config, icons, file)
                    }
                    files = files + project.files(icons)
                }

                if (files.empty) {
                    String source = config.mipmap ? "mipmap" : "drawable"
                    println("WARNING: launcher file not found: $config.ic_launcher in $source folders");
                    return;
                }

                Task task = project.task("prepareLauncherIconFor${variant.name.capitalize()}", type: LauncherIconTask) {
                    sources = files
                    target = dst
                    text = formatText(config.branch, version, flavor, user, datetime)
                    launcher = config.ic_launcher
                }

                variant.registerResGeneratingTask(task, new File(dst, "_dummy"))
            }
        }

    }

    String formatText(String branch, String version, String flavor,  String user, String datetime) {
        StringBuilder builder = new StringBuilder();
        if (branch) {
            builder.append(branch)
            builder.append("\n");
        }
        builder.append("V$version")
        if (!flavor && !user) {
            builder.append("-");
        } else {
            if (flavor) {
                builder.append("-");
                builder.append(flavor.toUpperCase());
            }
            builder.append("\n");
            if (user) {
                builder.append(user.toUpperCase());
                builder.append("@");
            }
        }
        builder.append(datetime)
        return builder.toString();

    }

    String nowMMDDhh() {
        SimpleDateFormat formatter = new SimpleDateFormat()
        formatter.setTimeZone(TimeZone.getTimeZone("GMT+8"));
        formatter.applyPattern("MMddHH");
        return formatter.format(new Date());
    }

    void searchIcons(LauncherIconExtension config, List<File> temp, File dir) {
        if (!dir.exists()) {
            return;
        }
        if (dir.isFile()) {
            boolean isNameMatched = dir.absolutePath.endsWith(config.ic_launcher);
            boolean isTypeMatched = dir.absolutePath.contains(config.mipmap ? "mipmap" : "drawable")
            if (isNameMatched && isTypeMatched) {
                temp.add(dir);
            }
            return;
        }
        List<File> files = dir.listFiles();
        if (files == null) {
            return;
        }
        for (File f : files) {
            searchIcons(config, temp, f);
        }
    }
}