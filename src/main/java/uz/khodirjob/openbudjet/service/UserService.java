package uz.khodirjob.openbudjet.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;
import uz.khodirjob.openbudjet.entity.User;
import uz.khodirjob.openbudjet.entity.Vote;
import uz.khodirjob.openbudjet.payload.KeyWords;
import uz.khodirjob.openbudjet.repository.UserRepository;
import uz.khodirjob.openbudjet.repository.VoteRepository;

import javax.xml.crypto.dsig.spec.HMACParameterSpec;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class UserService {
    private final UserRepository userRepository;
    private final VoteRepository voteRepository;

    public SendMessage addUser(Message message) {
        Optional<User> byChatId = userRepository.findByChatId(message.getChatId());
        if (byChatId.isPresent())
            return null;

        User user = User.builder()
                .chatId(message.getChatId())
                .phoneNumber(message.getContact().getPhoneNumber())
                .userName(message.getFrom().getUserName())
                .firstName(message.getFrom().getFirstName())
                .build();
        if (message.getContact().getPhoneNumber().contains("950035369")) {
            user.setIsAdmin(true);
        }
        User save = userRepository.save(user);

        SendMessage sendMessage = SendMessage.builder()
                .chatId(message.getChatId())
                .text(KeyWords.SELECT_MESSAGE)
                .replyMarkup(menu())
                .build();
        return sendMessage;
    }

    public ReplyKeyboardMarkup menu() {
        KeyboardButton button = new KeyboardButton(KeyWords.VOTE_BUTTON_TEXT);
        KeyboardButton button1 = new KeyboardButton(KeyWords.CONTACT_TEXT);
        KeyboardRow row = new KeyboardRow();
        row.add(button);

        KeyboardRow row1 = new KeyboardRow();
        row1.add(button1);

        List<KeyboardRow> rowList = new ArrayList<>();
        rowList.add(row);
        rowList.add(row1);

        ReplyKeyboardMarkup markup = ReplyKeyboardMarkup.builder()
                .selective(true)
                .resizeKeyboard(true)
                .keyboard(rowList)
                .build();

        return markup;
    }

    public List<SendMessage> confirm(Long chatId) {
        User user = userRepository.findByIsAdmin(true).get();
        List<SendMessage> messageList = new ArrayList<>();
        messageList.add(SendMessage.builder()
                .text(KeyWords.VOTE_CHEK_MESSAGE)
                .chatId(chatId)
                .build());
        User user1 = userRepository.findByChatId(chatId).get();
        Vote vote = Vote.builder()
                .user(user)
                .confirmVote(LocalDateTime.now())
                .build();


        vote = voteRepository.save(vote);

        SendMessage sendMessage = SendMessage.builder()
                .chatId(user.getChatId())
                .text(user1.getPhoneNumber() + " raqam egasi ovoz berganiligi ma'lum qildi. Ilimos buni tekshiring. Ma'lum qilish vaqti: " + LocalDateTime.now())
                .replyMarkup(markup(vote.getId()))
                .build();

        messageList.add(sendMessage);
        return messageList;
    }


    public InlineKeyboardMarkup markup(Integer voteId) {
        InlineKeyboardButton button = InlineKeyboardButton.builder()
                .text(KeyWords.CONFIRM_BUTTON_ADMIN)
                .callbackData("confirm/admin/" + voteId)
                .build();
        InlineKeyboardButton button1 = InlineKeyboardButton.builder()
                .text(KeyWords.NOT_CONFIRM_BUTTON_ADMIN)
                .callbackData("not/confirm/admin/" + voteId)
                .build();

        InlineKeyboardButton button2 = InlineKeyboardButton.builder()
                .text(KeyWords.BLOC_USER)
                .callbackData("bloc/voteId/" + voteId)
                .build();


        List<List<InlineKeyboardButton>> buttons = new ArrayList<>();
        buttons.add(Collections.singletonList(button));
        buttons.add(Collections.singletonList(button1));
        buttons.add(Collections.singletonList(button2));

        InlineKeyboardMarkup markup = InlineKeyboardMarkup.builder()
                .keyboard(buttons)
                .build();

        return markup;
    }

    public List<SendMessage> confirmVote(Long chatId, String data) {
        Integer voteId = Integer.parseInt(data.substring(14));
        Vote vote = voteRepository.findById(voteId).get();
        User user = vote.getUser();
        user.setIsVoted(true);
        userRepository.save(user);
        vote.setIsSuccess(true);
        voteRepository.save(vote);

        List<SendMessage> messageList = new ArrayList<>();

        messageList.add(SendMessage.builder()
                .chatId(vote.getUser().getChatId())
                .text("Sizning ovozingiz qabul qilindi agar to'lov qilinmagan bo'lsa aloqaga chiqing")
                .build());
        messageList.add(SendMessage.builder()
                .text("To'lov qilish yodingizdan chiqmasin")
                .chatId(chatId)
                .build());

        return messageList;
    }

    public List<SendMessage> notConfirmVote(Long chatId, String data) {
        Integer voteId = Integer.parseInt(data.substring(18));
        Vote vote = voteRepository.findById(voteId).get();
        User user = vote.getUser();
        user.setIsVoted(false);
        userRepository.save(user);
        vote.setIsSuccess(false);
        vote.setConfirmVote(LocalDateTime.now());
        voteRepository.save(vote);

        List<SendMessage> messageList = new ArrayList<>();

        messageList.add(SendMessage.builder()
                .chatId(vote.getUser().getChatId())
                .text("Sizning ovozingiz qabul qilinmadi")
                .build());
        messageList.add(SendMessage.builder()
                .text("Ovoz qabul qilinmadi")
                .chatId(chatId)
                .build());

        return messageList;
    }

    public List<SendMessage> blocUser(String data, Long chatId) {
        Integer voteId = Integer.parseInt(data.substring(12));
        Vote vote = voteRepository.findById(voteId).get();
        User user = vote.getUser();
        if (!user.getIsAdmin())
            user.setIsBlocked(true);
        userRepository.save(user);

        List<SendMessage> messageList = new ArrayList<>();

        messageList.add(SendMessage.builder()
                .chatId(vote.getUser().getChatId())
                .text("Siz bloklandiggiz")
                .build());

        messageList.add(SendMessage.builder()
                .text(user.getPhoneNumber() + " raqam egasi bloklandi")
                .chatId(chatId)
                .build());

        return messageList;

    }
}
