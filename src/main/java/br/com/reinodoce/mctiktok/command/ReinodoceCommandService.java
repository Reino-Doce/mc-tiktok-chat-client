package br.com.reinodoce.mctiktok.command;

import java.util.List;

public interface ReinodoceCommandService {
    CommandResult connect(String username);

    CommandResult disconnect();

    List<String> statusLines();

    CommandResult setReconnectSeconds(int seconds);

    CommandResult setFollowerRule(boolean enabled);

    CommandResult setMinMemberLevelRule(int level);

    CommandResult setSynteticGift(int value);

    CommandResult setSynteticGiftComboMode(String mode);

    CommandResult setSynteticFollow(boolean enabled);

    CommandResult setSynteticJoin(boolean enabled);

    CommandResult setSynteticMemberLevel(boolean enabled);

    CommandResult setChatEmotesEnabled(boolean enabled);

    CommandResult reload();
}
