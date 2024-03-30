package uz.khodirjob.openbudjet.service;

import com.sun.jdi.event.ExceptionEvent;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFCell;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.methods.send.SendDocument;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.InputFile;
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
import java.io.File;
import java.io.FileOutputStream;
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
        if (!message.getChatId().equals(message.getContact().getUserId()))
            return null;

        User user = null;
        if (byChatId.isPresent())
            user = byChatId.get();
        else
            user = User.builder()
                    .chatId(message.getChatId())
                    .phoneNumber(message.getContact().getPhoneNumber())
                    .userName(message.getFrom().getUserName())
                    .firstName(message.getFrom().getFirstName())
                    .build();
        if (message.getContact().getPhoneNumber().contains("934198255")) {
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
        List<SendMessage> messageList = new ArrayList<>();
        User user = userRepository.findByIsAdmin(true).get();
        for (User user1 : userRepository.findAllByIsAdmin(true)) {

        }

        messageList.add(SendMessage.builder()
                .text(KeyWords.VOTE_CHEK_MESSAGE)
                .chatId(chatId)
                .build());

        User user1 = userRepository.findByChatId(chatId).get();
        Vote vote = Vote.builder()
                .user(user1)
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
        if (user.getIsAdmin() == null)
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

    public boolean chekUser(Long chatId) {
        Optional<User> byChatId = userRepository.findByChatId(chatId);
        Boolean isBlocked = null;
        if (byChatId.isPresent())
            isBlocked = byChatId.get().getIsBlocked();

        return isBlocked == null ? false : isBlocked;
    }

    public boolean chekVote(Long chatId) {
        Optional<User> byChatId = userRepository.findByChatId(chatId);
        Boolean isVoted = null;
        if (byChatId.isPresent())
            isVoted = byChatId.get().getIsVoted();

        return isVoted == null ? false : isVoted;
    }

    public SendDocument getVotes(Long chatId) {
        try {
            Optional<User> byChatId = userRepository.findByChatId(chatId);
            if (byChatId.isEmpty() || byChatId.get().getIsAdmin() == null)
                return null;
            SendDocument sendDocument = new SendDocument();
            XSSFWorkbook workbook = new XSSFWorkbook();
            XSSFSheet sheet = workbook.createSheet("Berilgan ovozlar");
            XSSFRow row;
            int rowId = 0;
            row = sheet.createRow(rowId++);
            XSSFCell voteId = row.createCell(1);
            voteId.setCellValue("ID");
            XSSFCell phoneNumber = row.createCell(2);
            phoneNumber.setCellValue("Telefon raqam");
            XSSFCell confirmTime = row.createCell(3);
            confirmTime.setCellValue("Ovoz berilgan vaqti");
            XSSFCell status = row.createCell(4);
            status.setCellValue("Xolati");


            for (Vote vote : voteRepository.findAll()) {
                try {
                    row = sheet.createRow(rowId++);
                    CellStyle redStyle = workbook.createCellStyle();
                    redStyle.setFillForegroundColor(IndexedColors.RED.getIndex());
                    redStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
                    CellStyle yellowStyle = workbook.createCellStyle();
                    yellowStyle.setFillForegroundColor(IndexedColors.YELLOW.getIndex());
                    yellowStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
                    CellStyle greenStyle = workbook.createCellStyle();
                    greenStyle.setFillForegroundColor(IndexedColors.GREEN.getIndex());
                    greenStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);


                    XSSFCell cell0 = row.createCell(1);
                    cell0.setCellValue(vote.getId());

                    XSSFCell cell1 = row.createCell(2);
                    cell1.setCellValue("'" + vote.getUser().getPhoneNumber());

                    XSSFCell cell2 = row.createCell(3);
                    cell2.setCellValue(vote.getConfirmVote().toString());

                    XSSFCell cell = row.createCell(4);
                    if (vote.getIsSuccess() == null) {
                        row.setRowStyle(yellowStyle);
                        cell.setCellValue("Tekshirilmadi");
                        cell.setCellStyle(yellowStyle);
                        cell1.setCellStyle(yellowStyle);
                        cell2.setCellStyle(yellowStyle);
                        cell0.setCellStyle(yellowStyle);
                    } else if (vote.getIsSuccess()) {
                        cell.setCellValue("Qabul qilindi");
                        cell.setCellStyle(greenStyle);
                        cell1.setCellStyle(greenStyle);
                        cell2.setCellStyle(greenStyle);
                        cell0.setCellStyle(greenStyle);
                        row.setRowStyle(greenStyle);
                    } else {
                        cell.setCellValue("Qabul qilinmagdi");
                        cell.setCellStyle(redStyle);
                        cell1.setCellStyle(redStyle);
                        cell2.setCellStyle(redStyle);
                        cell0.setCellStyle(redStyle);
                        row.setRowStyle(redStyle);
                    }
                } catch (Exception e) {
                    System.out.println(e);
                }

            }
            FileOutputStream out = new FileOutputStream(
                    new File("src/main/resources/Ovozlar.xlsx"));
            workbook.write(out);

            File file = new File("src/main/resources/Ovozlar.xlsx");

            InputFile inputFile = new InputFile(file);
            sendDocument.setDocument(inputFile);
            sendDocument.setChatId(chatId);
            sendDocument.setCaption("Berilgan ovozlar ro'yxati");
            return sendDocument;
        } catch (Exception e) {
            System.out.println(e);
        }
        return null;
    }
}
