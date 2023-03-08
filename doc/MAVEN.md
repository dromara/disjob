# Maven Notes

## reference articles
[personal group id](https://central.sonatype.org/publish/requirements/#supported-code-hosting-services-for-personal-groupid)

## deploy maven central repository

### deploy mode
- versions-maven-plugin
```xml
<!-- mvn -Drevision=_ versions:set -DnewVersion=1.10-SNAPSHOT && mvn clean deploy -Prelease -DskipTests -Dcheckstyle.skip=true -U -->
<plugin>
  <groupId>org.codehaus.mojo</groupId>
  <artifactId>versions-maven-plugin</artifactId>
  <version>2.13.0</version>
  <configuration>
    <generateBackupPoms>false</generateBackupPoms>
  </configuration>
</plugin>
```

- flatten-maven-plugin
```xml
<!-- https://www.mojohaus.org/flatten-maven-plugin/usage.html -->
<!-- mvn clean deploy -Prelease -DskipTests -Dcheckstyle.skip=true -U -->
<plugin>
  <groupId>org.codehaus.mojo</groupId>
  <artifactId>flatten-maven-plugin</artifactId>
  <version>1.3.0</version>
  <configuration>
    <updatePomFile>true</updatePomFile>
    <flattenMode>resolveCiFriendliesOnly</flattenMode>
    <outputDirectory>target</outputDirectory>
  </configuration>
  <executions>
    <execution>
      <id>flatten</id>
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

### release interactive mode
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

### release non-interactive mode
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

## notes
- 使用“mvnw”命令需要确认“~/.m2”目录下是否有settings.xml文件且正确配置<server>元素
- release版本deploy完后还需要在“s01.oss.sonatype.org”页面上手动操作：“Staging Repositories” -> “Close” -> “Release”
