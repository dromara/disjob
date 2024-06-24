# Maven Notes

## Reference articles
[personal group id](https://central.sonatype.org/publish/requirements/#supported-code-hosting-services-for-personal-groupid)

## Deploy to maven central
> maven phase: `clean -> validate -> compile -> test -> package -> integration -> verify -> install -> deploy`

### deploy
```xml
<plugin>
  <groupId>org.apache.maven.plugins</groupId>
  <artifactId>maven-deploy-plugin</artifactId>
  <version>2.8.2</version>
</plugin>
```

- flatten-maven-plugin
```xml
<!-- https://www.mojohaus.org/flatten-maven-plugin/usage.html -->
<!-- ./mvnw clean install && ./mvnw deploy -Prelease -Dmaven.test.skip=true -->
<plugin>
  <groupId>org.codehaus.mojo</groupId>
  <artifactId>flatten-maven-plugin</artifactId>
  <version>1.6.0</version>
  <configuration>
    <updatePomFile>true</updatePomFile>
    <flattenMode>resolveCiFriendliesOnly</flattenMode>
    <outputDirectory>target</outputDirectory>
  </configuration>
  <executions>
    <execution>
      <id>flatten.process-resources</id>
      <phase>process-resources</phase>
      <goals>
        <goal>flatten</goal>
      </goals>
    </execution>
    <execution>
      <id>flatten.clean</id>
      <phase>clean</phase>
      <goals>
        <goal>clean</goal>
      </goals>
    </execution>
  </executions>
</plugin>
```

- versions-maven-plugin
```xml
<!-- ./mvnw -Drevision=_ versions:set -DnewVersion=1.10-SNAPSHOT && ./mvnw clean deploy -Prelease -DskipTests -->
<plugin>
  <groupId>org.codehaus.mojo</groupId>
  <artifactId>versions-maven-plugin</artifactId>
  <version>2.13.0</version>
  <configuration>
    <generateBackupPoms>false</generateBackupPoms>
  </configuration>
</plugin>
```

### maven-release-plugin
```xml
<plugin>
  <groupId>org.apache.maven.plugins</groupId>
  <artifactId>maven-release-plugin</artifactId>
  <version>3.0.1</version>
  <configuration>
    <autoVersionSubmodules>true</autoVersionSubmodules>
    <useReleaseProfile>false</useReleaseProfile>
    <releaseProfiles>release</releaseProfiles>
    <generateReleasePoms>false</generateReleasePoms>
    <tagNameFormat>v@{project.version}</tagNameFormat>
    <arguments>-DskipTests</arguments>
    <preparationGoals>clean verify</preparationGoals>
    <goals>deploy</goals>
    <checkModificationExcludes>
      <checkModificationExclude>**/pom.xml</checkModificationExclude>
    </checkModificationExcludes>
  </configuration>
</plugin>
```

- interactive
```bash
mvn release:update-versions \
  -B \
  -\-batch-mode \
  -DdevelopmentVersion=1.10-SNAPSHOT \
&& \
mvn release:prepare release:perform \
  -Prelease \
  -Darguments="-Dcheckstyle.skip=true"
```

- non-interactive
```bash
mvn release:update-versions \
  -B \
  -\-batch-mode \
  -DdevelopmentVersion=1.10-SNAPSHOT \
&& \
mvn release:prepare release:perform \
  -Prelease \
  -Darguments="-Dcheckstyle.skip=true" \
  -DreleaseVersion=1.10 \
  -Dtag=v1.10 \
  -DdevelopmentVersion=1.10-SNAPSHOT
```

## Others
- 使用“mvnw”命令需要确认“~/.m2”目录下是否有settings.xml文件且正确配置<server>元素
- release版本deploy完后还需要在“s01.oss.sonatype.org”页面上手动操作：“Staging Repositories” -> “Close” -> “Release”
- deploy本地jar包到中央仓库：
  - deploy jar包的`-DpomFile`配置参数必须放在最后且值为`pom.xml`
  - javadoc/sources校验不通过，需要上传一个虚假(dummy)的javadoc/sources来通过验证
```bash
mvn gpg:sign-and-deploy-file \
  -Dfile=jdk-tools-1.8.0_371-javadoc.jar \
  -Dclassifier=javadoc \
  -DgroupId=cn.ponfee \
  -DartifactId=jdk-tools \
  -Dversion=1.8.0_371 \
  -Dpackaging=jar \
  -DrepositoryId=ossrh \
  -Durl=https://s01.oss.sonatype.org/service/local/staging/deploy/maven2/ \
&& \
mvn gpg:sign-and-deploy-file \
  -Dfile=jdk-tools-1.8.0_371-sources.jar \
  -Dclassifier=sources \
  -DgroupId=cn.ponfee \
  -DartifactId=jdk-tools \
  -Dversion=1.8.0_371 \
  -Dpackaging=jar \
  -DrepositoryId=ossrh \
  -Durl=https://s01.oss.sonatype.org/service/local/staging/deploy/maven2/ \
&& \
mvn gpg:sign-and-deploy-file \
  -Dfile=jdk-tools-1.8.0_371.jar \
  -DgroupId=cn.ponfee \
  -DartifactId=jdk-tools \
  -Dversion=1.8.0_371 \
  -Dpackaging=jar \
  -DpomFile=pom.xml \
  -DrepositoryId=ossrh \
  -Durl=https://s01.oss.sonatype.org/service/local/staging/deploy/maven2/ 
```
