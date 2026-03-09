package br.com.reinodoce.mctiktok.chat;

public interface ChatEventSink {
    void sendLiveComment(String prefix, String username, String message);

    void sendLiveComment(String prefix, RichLiveMessage message);

    void sendStarComment(String prefix, String username, String message);

    void sendStarComment(String prefix, RichLiveMessage message);

    void sendSyntheticGift(String prefix, String username, String giftName, int count);

    void sendSyntheticGift(String prefix, RichLiveMessage message);

    void sendSyntheticFollow(String prefix, String username);

    void sendSyntheticFollow(String prefix, RichLiveMessage message);

    void sendSyntheticJoin(String prefix, String username);

    void sendSyntheticJoin(String prefix, RichLiveMessage message);

    void sendSyntheticMemberLevel(String prefix, String username, int memberLevel);

    void sendSyntheticMemberLevel(String prefix, RichLiveMessage message, int memberLevel);

    void sendSystem(String message, boolean success);

    static ChatEventSink noop() {
        return new ChatEventSink() {
            @Override
            public void sendLiveComment(String prefix, String username, String message) {
            }

            @Override
            public void sendLiveComment(String prefix, RichLiveMessage message) {
            }

            @Override
            public void sendStarComment(String prefix, String username, String message) {
            }

            @Override
            public void sendStarComment(String prefix, RichLiveMessage message) {
            }

            @Override
            public void sendSyntheticGift(String prefix, String username, String giftName, int count) {
            }

            @Override
            public void sendSyntheticGift(String prefix, RichLiveMessage message) {
            }

            @Override
            public void sendSyntheticFollow(String prefix, String username) {
            }

            @Override
            public void sendSyntheticFollow(String prefix, RichLiveMessage message) {
            }

            @Override
            public void sendSyntheticJoin(String prefix, String username) {
            }

            @Override
            public void sendSyntheticJoin(String prefix, RichLiveMessage message) {
            }

            @Override
            public void sendSyntheticMemberLevel(String prefix, String username, int memberLevel) {
            }

            @Override
            public void sendSyntheticMemberLevel(String prefix, RichLiveMessage message, int memberLevel) {
            }

            @Override
            public void sendSystem(String message, boolean success) {
            }
        };
    }
}
