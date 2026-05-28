package com.sparta.logistics.slack.sender;

public record SlackSendResult(
        String slackTs,
        String slackChannelId
) {
}
