# worldcomp ☠
TeamSpeak 3 world aggregator for Wilderness Warbands using the TS3 ServerQuery interface.

Build with Gradle and Java 8. The TS3 library by TheHolyWaffle is provided in ./lib/ because the only repo version on jcenter is an outdated fork.

Config
------

Create "config.properties" and edit accordingly.

```
#worldcomp config
#Sat Nov 19 19:48:10 CST 2016
host=127.0.0.1
port=10011
vserver=1
queryUser=serveradmin
queryPass=hunter2
nickname=worldb0t
ownerID=...
channelID=1
```

Get ownerID from Settings > Security > Identity Manager -> Unique ID. But actually it's not used yet, so...

Todo
----

- Auto clear worlds before every wave
- Move to a more flexible multi-channel, multi-network approach