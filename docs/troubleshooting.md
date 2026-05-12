# Troubleshooting and limitations

## Limitations

- The TikTok integration uses an **unofficial** API.
- Changes on TikTok may break events or the connection without notice.
- Member level may vary per region or LIVE payload.

## Troubleshooting

**Gradle reports a Java error.**
Use JDK 17 for the 1.20.1 build; point `JAVA_HOME` at a Java 17
installation. On Windows, `gradlew.bat` will try to auto-discover a
compatible local JDK (`jdk-22`, `jdk-21`, `jdk-17`, or `jre1.8`) before
falling back to the system `java.exe`.

**Launcher fails to load the new mod build.**
Remove the old mod jar from the `mods/` folder before copying the new
artifact. Some launchers cache classloaders per-jar and will refuse to
load two jars with overlapping mod ids.

**No messages appear in game.**
- Check that the chat HUD is enabled.
- Run `/reinodoce status` to inspect the connection state and the last
  error.
- Verify that `chatPrefix` is not empty (see
  [configuration.md](configuration.md)).

**Invalid username.**
TikTok usernames must be 2–30 characters from `A-Z`, `a-z`, `0-9`, `.`,
and `_`. The leading `@` is optional and is stripped before validation.

**LIVE offline / keeps reconnecting.**
Tune the reconnect interval with
`/reinodoce settings reconnect <seconds>`. `0` disables reconnect; the
default is `5`.

**Comments are filtered out.**
Check `/reinodoce status` for active filters, then relax `rule follower`
or `rule min-member-level`. See [commands.md](commands.md).
