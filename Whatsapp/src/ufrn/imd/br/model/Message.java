package ufrn.imd.br.model;

import java.util.concurrent.ConcurrentHashMap;

public class Message {
    int sender;
    int receiver;
    String text;

    private ConcurrentHashMap<Integer, String> messages = new ConcurrentHashMap<>();

	public void sendMessage(int receiver, String text) {
		messages.put(receiver, text);
	}
}