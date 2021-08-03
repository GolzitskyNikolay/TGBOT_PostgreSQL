import database.MyDatabase;
import org.apache.log4j.Logger;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class CallBack {
    private final Logger logger = Logger.getLogger(CallBack.class);

    private AnimeBot animeBot;
    private SendAnime sendAnime;
    private StringBuilder userName;

    CallBack(AnimeBot animeBot, SendAnime sendAnime) {
        this.animeBot = animeBot;
        this.sendAnime = sendAnime;
    }

    void setUserName(StringBuilder userName){
        this.userName = userName;
    }

    void getGenreFromCallbackAndSendAnime(String text, SendMessage sendMessage, Update update, MyDatabase database) {
        text = text.split("genre = ")[1];
        try {
            animeBot.execute(sendMessage.setText("Жанр \"" + text + "\""));
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
        sendAnime.sendAnimeByGenre(update.getCallbackQuery().getMessage(), text, database);
    }

    void getListFromCallbackAndOpenIt(String text, SendMessage sendMessage, long chatId, MyDatabase database) {
        text = text.split("listNameToOpen = ")[1];

        List<String> animeNames = database.getAnimeByListNameAndUserId(text, chatId);

        if (animeNames.size() != 0) {
            Collections.sort(animeNames);

            StringBuilder builder = new StringBuilder("Список \"");
            builder.append(text).append("\":\n\n");
            for (int i = 1; i <= animeNames.size(); i++) {
                builder.append(i).append(") ").append(animeNames.get(i - 1)).append("\n");
            }
            sendMessage.setText(builder.toString());
        } else {
            logger.warn("user " + userName + " tried to open empty list " + text);
            sendMessage.setText("Список пустой");
        }

        try {
            animeBot.execute(sendMessage);
            logger.info("user " + userName + " opened list \"" + text + "\"");
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }

        if (animeNames.size() != 0) {
            sendMessage.setText("Вы можете удалить аниме из списка с помощью команды \"/deleteAnime\"");
            showDeleteAnimeButton(sendMessage);
            try {
                animeBot.execute(sendMessage);
            } catch (TelegramApiException e) {
                e.printStackTrace();
            }
            animeBot.setLastCommand(text);
        }
    }

    private synchronized void showDeleteAnimeButton(SendMessage sendMessage) {
        ReplyKeyboardMarkup replyKeyboardMarkup = new ReplyKeyboardMarkup();
        sendMessage.setReplyMarkup(replyKeyboardMarkup);

        replyKeyboardMarkup.setSelective(true);
        replyKeyboardMarkup.setResizeKeyboard(true);
        replyKeyboardMarkup.setOneTimeKeyboard(false);

        List<KeyboardRow> keyboard = new ArrayList<>();

        KeyboardRow keyboardRow1 = new KeyboardRow();
        keyboardRow1.add(new KeyboardButton("/deleteAnime"));
        keyboardRow1.add(new KeyboardButton("/lists"));

        keyboard.add(keyboardRow1);

        replyKeyboardMarkup.setKeyboard(keyboard);
    }

    void getListFromCallbackAndDeleteIt(String text, SendMessage sendMessage, long chatId, MyDatabase database) {
        text = text.split("listNameToDelete = ")[1];
        database.deletePersonalList(text, chatId);
        sendMessage.setText("Список \"" + text + "\" удалён");
        try {
            animeBot.execute(sendMessage);
            logger.info("user " + userName + " deleted list \"" + text + "\"");
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }

    void getAnimeFromCallbackAndSendIt(String text, Commands basicCommands, long chatId, MyDatabase database) {
        text = text.split("addInList ")[1];
        StringBuilder builder = new StringBuilder(text);
        builder.append(" <-> ");

        logger.info("user " + userName + " want to add anime \"" + text + "\" in list");

        try {
            animeBot.execute(basicCommands.showLists(chatId, String.valueOf(builder),
                    "Выберите список, в который хотите добавить аниме", database));
            logger.info("user " + userName + " is choosing list for adding anime \"" + text + "\"");

        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }

    void getListFromCallBackAndAddAnime(String text, SendMessage sendMessage, MyDatabase database) {
        int animeId = Integer.parseInt(text.split(" <-> ")[0]);
        String list = text.split(" <-> ")[1];

        int listId = database.getPersonalListIdByName(list);

        boolean wasAdded = database.addListItem(animeId, listId);

        String messageText;
        if (wasAdded) {
            messageText = "Аниме добавлено в список \"" + list + "\"";
            logger.info("user " + userName + " added anime with id = " + animeId + " in list "
                    + list);

        } else {
            messageText = "Аниме уже есть в этом списке";
            logger.info("user " + userName + " already has this anime in this list");
        }

        try {
            animeBot.execute(sendMessage.setText(messageText));
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }

    void showMarks(String text, long chatId) {
        text = text.split("markAnime ")[1];
        logger.info("user " + userName + " want to mark anime with id = \"" + text + "\"");

        try {
            animeBot.execute(showButtonsForMark(text, chatId));
            logger.info("user " + userName + " is choosing mark for anime with id = \"" + text + "\"");
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }

    private SendMessage showButtonsForMark(String animeName, long chatId) {
        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rowList = new ArrayList<>();
        List<InlineKeyboardButton> row = new ArrayList<>();

        for (int i = 1; i <= 10; i++) {
            if (i == 6) {
                rowList.add(row);
                row = new ArrayList<>();
            }
            row.add(new InlineKeyboardButton().setText(
                    String.valueOf(i)).setCallbackData(animeName + " markOfAnImE = " + i));
            if (i == 10) {
                rowList.add(row);
            }
        }

        markup.setKeyboard(rowList);
        return new SendMessage().setChatId(chatId).setText(
                "Оцените аниме (1-ужасно, 10-великолепно)").setReplyMarkup(markup);
    }

    void addUserMark(String text, SendMessage sendMessage, long chatId, MyDatabase database) {
        int animeId = Integer.parseInt(text.split(" markOfAnImE = ")[0]);
        int mark = Integer.parseInt(text.split(" markOfAnImE = ")[1]);

        database.addOrUpdateUserMark(chatId, mark, animeId);
        logger.info("user " + userName + " added mark for anime with id = " + animeId + ", mark = " + mark);

        try {
            animeBot.execute(sendMessage.setText("Оценка обновлена до " + mark));
            logger.info("mark for user " + userName + " is updated");
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }
}
