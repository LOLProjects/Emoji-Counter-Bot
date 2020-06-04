import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.security.auth.login.LoginException;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.Emote;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

public class Main extends ListenerAdapter
{
	String prefix = "!";

	public Main() throws LoginException, InterruptedException
	{
		new JDABuilder("token").addEventListeners(this).build();
	}

	@Override
	public void onMessageReceived(MessageReceivedEvent e)
	{
		if (e.getMember().getUser().isBot())
			return;
		if (e.getMessage().getContentRaw().contentEquals(prefix + "count"))
		{
			e.getChannel().sendMessage("Counting").queue();
			EmojiCount ec = new EmojiCount(e.getJDA());
			long firstMessageId = e.getChannel().getHistoryFromBeginning(1).complete().getRetrievedHistory().get(0).getIdLong();
			long lastMessageId = 0;
			while (firstMessageId != lastMessageId)
			{
				List<Message> history = e.getChannel().getHistoryBefore(lastMessageId, 100).complete().getRetrievedHistory();
				lastMessageId = history.get(history.size() - 1).getIdLong();
				history.forEach((m) -> processMessage(ec, m));
				System.out.println("Processed one set");
			}
			e.getMessage().getChannel().sendMessage(ec.getMembers()).queue();
		}
	}

	public void processMessage(EmojiCount ec, Message message)
	{
		if (message == null) return;
		if (message.getAuthor().isBot())
			return;
		message.getEmotes().forEach((emote) -> ec.addOne(message.getAuthor(), emote));
	}

	public static void main(String[] args) throws LoginException, InterruptedException
	{
		new Main();
	}
}

class EmojiCount
{
	HashMap<Long, HashMap<Long, Integer>> memberToCount = new HashMap<>();
	JDA jda;

	public EmojiCount(JDA j)
	{
		jda = j;
	}

	public void addOne(User m, Emote emote)
	{
		long mId = m.getIdLong();
		long eId = emote.getIdLong();
		HashMap<Long, Integer> count = new HashMap<>();
		count.put(eId, 1);
		memberToCount.compute(mId, (memberId, nameToCount) -> {
			if (memberId == null || nameToCount == null)
				return count;

			Integer numOfEmoji = nameToCount.get(eId);

			if (numOfEmoji == null)
				numOfEmoji = 0;
			numOfEmoji++;

			nameToCount.put(eId, numOfEmoji);

			return nameToCount;
		});
	}

	public String getMembers()
	{
		String info = "";
		for (Map.Entry<Long, HashMap<Long, Integer>> entry : memberToCount.entrySet())
		{
			info += getMember(entry.getKey());
		}
		if (info.contentEquals(""))
			return "No emojis";
		return info;
	}

	public String getMember(long m)
	{
		HashMap<Long, Integer> count = memberToCount.get(m);
		String info = String.format("===<@%s>===\n", m);
		for (Map.Entry<Long, Integer> entry : count.entrySet())
		{
			Emote e = jda.getEmoteById(entry.getKey());
			if (e == null) continue;
			info += String.format("%s : %d\n", e.getAsMention(), entry.getValue());
		}
		return info;
	}

}
