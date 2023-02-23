# Maven Notes

## reference articles
[personal group id](https://central.sonatype.org/publish/requirements/#supported-code-hosting-services-for-personal-groupid)

## deploy maven central repository

### deploy mode
```bash
mvn -Drevision=_ versions:set -DnewVersion=1.10-SNAPSHOT && mvn clean deploy -Prelease -DskipTests -Dcheckstyle.skip=true -U
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
