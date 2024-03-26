package uz.khodirjob.openbudjet.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.DeleteMessage;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import uz.khodirjob.openbudjet.payload.KeyWords;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Service
@RequiredArgsConstructor
public class MyBot extends TelegramLongPollingBot {
    private Long chatId;
    private final UserService userService;

    @Override
    public void onUpdateReceived(Update update) {
        if (update.hasMessage()) {
            Message message = update.getMessage();
            this.chatId = message.getChatId();
            if (message.hasText()) {
                text(message);
            } else if (message.hasContact()) {
                sendMessage(userService.addUser(message));
            }
        } else if (update.hasCallbackQuery()) {
            CallbackQuery callbackQuery = update.getCallbackQuery();
            this.chatId = callbackQuery.getFrom().getId();
            String data = callbackQuery.getData();
            if (data.equals("confirm")) {
                sendMessage(userService.confirm(chatId));
            } else if (data.startsWith("confirm/admin/")) {
                sendMessage(userService.confirmVote(chatId, data));
            } else if (data.startsWith("not/confirm/admin/")) {
                sendMessage(userService.notConfirmVote(chatId, data));
            } else if (data.startsWith("bloc/voteId/")) {
                sendMessage(userService.blocUser(data, chatId));
            }


            try {
                execute(DeleteMessage.builder()
                        .messageId(callbackQuery.getMessage().getMessageId())
                        .chatId(chatId)
                        .build());
            } catch (Exception e) {
                System.out.println(e);
            }
        }
    }


    private void text(Message message) {
        String text = message.getText();
        switch (text) {
            case "/start" -> start();
            case KeyWords.VOTE_BUTTON_TEXT -> vote();
        }
    }

    private void vote() {
        SendMessage sendMessage = SendMessage.builder()
                .chatId(chatId)
                .text(KeyWords.VOTE_MESSAGE)
                .build();
        InlineKeyboardButton button = InlineKeyboardButton.builder()
                .text(KeyWords.VOTE_BUTTON_TG)
                .url(KeyWords.VOTE_BUTTON_TG_URL)
                .build();

        InlineKeyboardButton button1 = InlineKeyboardButton.builder()
                .text(KeyWords.VOTE_BUTTON_WEB)
                .url(KeyWords.VOTE_BUTTON_WEB_URL)
                .build();

        InlineKeyboardButton button2 = InlineKeyboardButton.builder()
                .text(KeyWords.VOTE_BUTTON_CONFIRM)
                .callbackData("confirm")
                .build();

        List<List<InlineKeyboardButton>> row = new ArrayList<>();
        row.add(Collections.singletonList(button));
        row.add(Collections.singletonList(button1));
        row.add(Collections.singletonList(button2));

        InlineKeyboardMarkup markup = InlineKeyboardMarkup.builder()
                .keyboard(row)
                .build();

        sendMessage.setReplyMarkup(markup);
        sendMessage(sendMessage);
    }


    private void start() {
        SendMessage sendMessage = new SendMessage();
        sendMessage.setChatId(chatId);
        sendMessage.setText(KeyWords.HELLO_MESSAGE);

        KeyboardButton button = KeyboardButton.builder()
                .text(KeyWords.TELEPHONE_BUTTON_TEXT)
                .requestContact(true)
                .build();

        KeyboardRow row = new KeyboardRow();
        row.add(button);

        ReplyKeyboardMarkup markup = ReplyKeyboardMarkup.builder()
                .keyboardRow(row)
                .resizeKeyboard(true)
                .selective(true)
                .build();

        sendMessage.setReplyMarkup(markup);
        sendMessage(sendMessage);
    }

    public void menu() {
        KeyboardButton button = new KeyboardButton("Ovoz berish");


    }

    @Override
    public String getBotUsername() {
        return "takrortakrorbot";
    }

    @Override
    public String getBotToken() {
        return "6896507973:AAF1NZYduQwZ-tJwlmvMLHIZFDSR7ieECUk";
    }

    public void sendMessage(SendMessage sendMessage) {
        try {
            execute(sendMessage);
        } catch (TelegramApiException e) {
            throw new RuntimeException(e);
        }
    }

    public void sendMessage(List<SendMessage> sendMessages) {
        sendMessages.forEach(sendMessage -> {
            try {
                execute(sendMessage);
            } catch (TelegramApiException e) {
                throw new RuntimeException(e);
            }
        });
    }
}
