package com.jtz.worldcomp;

import com.github.theholywaffle.teamspeak3.TS3Config;
import com.github.theholywaffle.teamspeak3.TS3Query;
import com.github.theholywaffle.teamspeak3.TS3Api;
import com.github.theholywaffle.teamspeak3.api.TextMessageTargetMode;
import com.github.theholywaffle.teamspeak3.api.wrapper.ServerQueryInfo;
import com.github.theholywaffle.teamspeak3.api.event.TS3EventType;
import com.github.theholywaffle.teamspeak3.api.event.TS3EventAdapter;
import com.github.theholywaffle.teamspeak3.api.event.TextMessageEvent;
import com.github.theholywaffle.teamspeak3.api.reconnect.ConnectionHandler;
import com.github.theholywaffle.teamspeak3.api.reconnect.ReconnectStrategy;

import java.util.Collections;
import java.util.Arrays;
import java.util.Set;
import java.util.HashSet;
import java.util.HashMap;
import java.util.Map;
import java.util.List;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.Properties;
import java.util.stream.Collectors;
import java.util.regex.Pattern;
import java.util.regex.Matcher;
import java.io.FileInputStream;
import java.io.IOException;

public class Main {

    private static final Pattern WORLD_PATTERN = Pattern.compile("^\\D*(\\d+) *(dwf|elm|rdi).*$");

    private static final Integer[] _VALID_WORLDS = {
        1, 2, 4, 5, 6, 9, 10, 12, 14, 15, 16, 21, 22, 23, 24,
        25, 26, 27, 28, 30, 31, 32, 35, 36, 37, 39, 40, 42, 44,
        45, 46, 48, 49, 50, 51, 53, 54, 56, 58, 59, 60, 62, 63, 64,
        65, 66, 67, 68, 69, 70, 71, 72, 73, 74, 76, 77, 78, 79,
        82, 83, 84, 85, 86, 87, 88, 89, 91, 92, 96, 98, 99,
        100, 103, 104, 105, 106, 114, 116, 117, 119, 123, 124,
        134, 138, 139, 140
    };

    private static final Set<Integer> VALID_WORLDS = new HashSet<Integer>(Arrays.asList(_VALID_WORLDS));

    private static Map<String, List<Integer>> locations = new HashMap<>();

    public static boolean isValidWorld(int world) {
        return VALID_WORLDS.contains(world);
    }

    public static String getWorldsAsMessage() {
        StringBuilder sb = new StringBuilder();
        for (String l : locations.keySet()) {
            List<Integer> worlds = locations.get(l);
            sb.append(l.toUpperCase());
            sb.append(": ");
            sb.append(worlds.stream().map(n -> n.toString()).collect(Collectors.joining(", ")));
            sb.append("\n");
        }
        String s = sb.toString();
        return s.length() > 0 ? "\n" + s : "No worlds.";
    }

    public static void startBot() throws Exception {
        FileInputStream fis = new FileInputStream("config.properties");
        Properties props = new Properties();
        props.load(fis);

        String host = props.getProperty("host");
        int port = Integer.parseInt(props.getProperty("port"));
        int vserver = Integer.parseInt(props.getProperty("vserver"));
        String queryUser = props.getProperty("queryUser");
        String queryPass = props.getProperty("queryPass");
        String nickname = props.getProperty("nickname");
        String ownerID = props.getProperty("ownerID");
        int channelID = Integer.parseInt(props.getProperty("channelID"));

        if (host == null || queryUser == null || queryPass == null ||
            nickname == null || ownerID == null) {
            throw new Exception("One or more properties was not set.");
        }

        final TS3Config config = new TS3Config();
        config.setHost(host);
        config.setQueryPort(port);
        config.setDebugLevel(Level.ALL);

        config.setReconnectStrategy(ReconnectStrategy.exponentialBackoff());
        config.setConnectionHandler(new ConnectionHandler() {
            @Override
            public void onConnect(TS3Query query) {
                final TS3Api api = query.getApi();
                api.login(queryUser, queryPass);
                api.selectVirtualServerById(vserver);
                api.setNickname(nickname);
                api.sendChannelMessage(nickname + " is back.");

                // the bot's own info.
                ServerQueryInfo self = api.whoAmI();
                // System.out.println(nickname + "'s ID: " + self.getId());

                // join whatever channel.
                api.moveClient(self.getId(), channelID);
                // api.sendChannelMessage("No worlds ATM sorry.");

                api.registerEvent(TS3EventType.TEXT_CHANNEL, self.getChannelId());
            }

            @Override
            public void onDisconnect(TS3Query query) {
                // ...
            }
        });

        final TS3Query query = new TS3Query(config);
        query.connect();

        final TS3Api api = query.getApi();
        ServerQueryInfo self = api.whoAmI();

        api.addTS3Listeners(new TS3EventAdapter() {
            @Override
            public void onTextMessage(TextMessageEvent e) {
                if (e.getTargetMode() == TextMessageTargetMode.CHANNEL &&
                    e.getInvokerId() != self.getId()) {

                    try {
                        String msg = e.getMessage().toLowerCase();
                        handleMessage(api, msg);
                    } catch (Exception ex) {
                        ex.printStackTrace();
                    }
                }
            }
        });
    }

    public static void handleMessage(TS3Api api, String msg) {
        // api.sendChannelMessage(e.getInvokerName() + " is bad.");

        if (msg.equals("!worlds")) {
            api.sendChannelMessage(getWorldsAsMessage());
        }

        if (msg.equals("!reset") || msg.equals("!clear")) {
            locations.clear();
            api.sendChannelMessage("Reset world list.");
        }

        Matcher m = WORLD_PATTERN.matcher(msg);
        if (!m.matches()) {
            return;
        }

        int w = Integer.parseInt(m.group(1));
        String loc = m.group(2);
        if (!isValidWorld(w)) {
            return;
        }

        // initialize list if empty.
        if (!locations.containsKey(loc)) {
            locations.put(loc, new ArrayList<Integer>());
        }

        for (String l : locations.keySet()) {
            List<Integer> worlds = locations.get(l);
            if (worlds.contains(w)) {
                worlds.remove((Object) w);
            }
        }
        locations.get(loc).add(w);
        Collections.sort(locations.get(loc));
    }

    public static void main(String[] args) {
        try {
            startBot();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
