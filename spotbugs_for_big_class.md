*  Download and install [Gradle](https://gradle.org/install).

*  Checkout/fork the target/latest version from github: [https://github.com/spotbugs/spotbugs](https://github.com/spotbugs/spotbugs) or [https://github.com/landawn/spotbugs](https://github.com/landawn/spotbugs)

*  Modify all `java/jdk versions` in `./spotbugs/spotbugs/build.gradle` and `./spotbugs/spotbugs-annotations/build.gradle` to `11`.

*  Modify below lines in class `edu.umd.cs.findbugs.ba.AnalysisContext`:

```java
   public boolean isTooBig(final ClassDescriptor desc) {
        final IAnalysisCache analysisCache = Global.getAnalysisCache();

        try {
            final ClassContext classContext = analysisCache.getClassAnalysis(ClassContext.class, desc);
            final ClassData classData = analysisCache.getClassAnalysis(ClassData.class, desc);
            if (classData.getData().length > 10000000) { // increase the number to 10000000
                return true;
            }
            try {
                final JavaClass javaClass = classContext.getJavaClass();
                if (javaClass.getMethods().length > 10000) { // increase the number to 10000
                    return true;
                }
            } catch (final RuntimeException e) {
            .....
    }

```

*  Run `gradlew build` under root folder: `.\spotbugs\`

* For `Eclipse`, copy all `jar` files under folder: `.\spotbugs\eclipsePlugin\lib\` to Eclipse plugin folder: `ECLIPSE_ROOT\plugins\com.github.spotbugs.plugin.eclipse_4.8.6.r202406180231-6cf7b2c\lib`. Depends on the SpotBugs plugin version installed in Eclipse, `com.github.spotbugs.plugin.eclipse_4.8.6.r202406180231-6cf7b2c` could be different.

* For `Intellij`, copy all `jar` files under folder: `.\spotbugs\eclipsePlugin\lib\` to Intellij plugin folder: `Users\<user>\AppData\Roaming\JetBrains\IdeaIC2024.3\plugins\spotbugs-idea\lib`. `IdeaIC2024.3` is the current installed Intellij version.


